//! Narrow Android ABI entry point.
//!
//! The v1 transition protocol is deliberately small: it returns only the state
//! fields changed by Complete/Reopen/Skip. Kotlin applies that result to its
//! original task and remains the single Room writer and alarm-effect sequencer.

use dayloop_domain::{Task, TaskId, TaskStatus};
use dayloop_engine::{apply_task_command, TaskCommand};
use std::panic::{catch_unwind, AssertUnwindSafe};

const REQUEST_MAGIC: [u8; 4] = *b"DLT1";
const RESPONSE_MAGIC: [u8; 4] = *b"DLR1";
const MAX_REQUEST_BYTES: usize = 6_144;
const MAX_ID_BYTES: usize = 1_024;
const MAX_TITLE_BYTES: usize = 4_096;
const RESPONSE_BYTES: usize = 13;

/// Returned by [`dayloop_ffi_apply_task_transition`].
#[repr(u32)]
pub enum TransitionStatus {
    Ok = 0,
    OutputTooSmall = 1,
    InvalidInput = 2,
    InternalFailure = 3,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum WireCommand {
    Complete,
    Reopen,
    Skip,
}

impl TryFrom<u8> for WireCommand {
    type Error = ();

    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            1 => Ok(Self::Complete),
            2 => Ok(Self::Reopen),
            3 => Ok(Self::Skip),
            _ => Err(()),
        }
    }
}

struct TransitionRequest {
    command: WireCommand,
    task: Task,
    command_at_epoch_millis: i64,
}

fn read_u16(input: &[u8], offset: &mut usize) -> Result<u16, ()> {
    let bytes: [u8; 2] = input
        .get(*offset..*offset + 2)
        .ok_or(())?
        .try_into()
        .map_err(|_| ())?;
    *offset += 2;
    Ok(u16::from_le_bytes(bytes))
}

fn read_i64(input: &[u8], offset: &mut usize) -> Result<i64, ()> {
    let bytes: [u8; 8] = input
        .get(*offset..*offset + 8)
        .ok_or(())?
        .try_into()
        .map_err(|_| ())?;
    *offset += 8;
    Ok(i64::from_le_bytes(bytes))
}

fn decode_request(input: &[u8]) -> Result<TransitionRequest, ()> {
    if input.len() > MAX_REQUEST_BYTES || input.len() < 26 || input.get(..4) != Some(&REQUEST_MAGIC)
    {
        return Err(());
    }
    let command = WireCommand::try_from(input[4])?;
    let status = match input[5] {
        0 => TaskStatus::Active,
        1 => TaskStatus::Completed,
        2 => TaskStatus::Skipped,
        3 => TaskStatus::Someday,
        _ => return Err(()),
    };
    let mut offset = 6;
    let id_length = usize::from(read_u16(input, &mut offset)?);
    let title_length = usize::from(read_u16(input, &mut offset)?);
    if id_length == 0
        || id_length > MAX_ID_BYTES
        || title_length == 0
        || title_length > MAX_TITLE_BYTES
    {
        return Err(());
    }
    let completed_at_epoch_millis = read_i64(input, &mut offset)?;
    let command_at_epoch_millis = read_i64(input, &mut offset)?;
    let expected_length = offset
        .checked_add(id_length)
        .and_then(|value| value.checked_add(title_length))
        .ok_or(())?;
    if expected_length != input.len() {
        return Err(());
    }
    let id = std::str::from_utf8(&input[offset..offset + id_length]).map_err(|_| ())?;
    offset += id_length;
    let title = std::str::from_utf8(&input[offset..]).map_err(|_| ())?;
    let mut task = Task::new(TaskId::new(id).map_err(|_| ())?, title).map_err(|_| ())?;
    task.status = status;
    task.completed_at_epoch_millis =
        (completed_at_epoch_millis != -1).then_some(completed_at_epoch_millis);
    Ok(TransitionRequest {
        command,
        task,
        command_at_epoch_millis,
    })
}

fn encode_result(task: Task) -> [u8; RESPONSE_BYTES] {
    let mut output = [0u8; RESPONSE_BYTES];
    output[..4].copy_from_slice(&RESPONSE_MAGIC);
    output[4] = match task.status {
        TaskStatus::Active => 0,
        TaskStatus::Completed => 1,
        TaskStatus::Skipped => 2,
        TaskStatus::Someday => 3,
    };
    output[5..].copy_from_slice(&task.completed_at_epoch_millis.unwrap_or(-1).to_le_bytes());
    output
}

fn apply_transition(input: &[u8]) -> Result<[u8; RESPONSE_BYTES], ()> {
    let request = decode_request(input)?;
    let command = match request.command {
        WireCommand::Complete => TaskCommand::Complete {
            at_epoch_millis: request.command_at_epoch_millis,
        },
        WireCommand::Reopen => TaskCommand::Reopen,
        WireCommand::Skip => TaskCommand::Skip,
    };
    Ok(encode_result(apply_task_command(request.task, command)))
}

/// Increment when a backward-compatible FFI capability is added.
#[unsafe(no_mangle)]
pub extern "C" fn dayloop_ffi_abi_version() -> u32 {
    1
}

/// Applies a v1 task transition request. `output` must point to at least 13
/// writable bytes. This entry point contains all pointer handling and panics;
/// no Rust panic may cross JNI/C ABI.
///
/// # Safety
///
/// `input` must designate `input_length` readable bytes for the full call, and
/// `output` must designate `output_capacity` writable bytes. Both pointers must
/// remain valid and non-overlapping while the function executes. The caller must
/// not retain or use either pointer after the backing Kotlin/Java direct buffer
/// has been released.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn dayloop_ffi_apply_task_transition(
    input: *const u8,
    input_length: usize,
    output: *mut u8,
    output_capacity: usize,
) -> TransitionStatus {
    if input.is_null()
        || output.is_null()
        || output_capacity < RESPONSE_BYTES
        || input_length > MAX_REQUEST_BYTES
    {
        return if output_capacity < RESPONSE_BYTES {
            TransitionStatus::OutputTooSmall
        } else {
            TransitionStatus::InvalidInput
        };
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        // SAFETY: null and maximum length were checked above; the caller owns
        // the readable input range and writable output range for this call.
        let request = unsafe { std::slice::from_raw_parts(input, input_length) };
        let response = apply_transition(request)?;
        // SAFETY: output_capacity was checked against RESPONSE_BYTES above.
        unsafe { std::ptr::copy_nonoverlapping(response.as_ptr(), output, RESPONSE_BYTES) };
        Ok::<(), ()>(())
    }));
    match result {
        Ok(Ok(())) => TransitionStatus::Ok,
        Ok(Err(())) => TransitionStatus::InvalidInput,
        Err(_) => TransitionStatus::InternalFailure,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn complete_request() -> Vec<u8> {
        let id = b"task-1";
        let title = b"Plan day";
        let mut bytes = Vec::from(REQUEST_MAGIC);
        bytes.extend([1, 0]);
        bytes.extend((id.len() as u16).to_le_bytes());
        bytes.extend((title.len() as u16).to_le_bytes());
        bytes.extend((-1_i64).to_le_bytes());
        bytes.extend(1_000_i64.to_le_bytes());
        bytes.extend(id);
        bytes.extend(title);
        bytes
    }

    #[test]
    fn reports_the_initial_abi_version() {
        assert_eq!(super::dayloop_ffi_abi_version(), 1);
    }

    #[test]
    fn complete_transition_encodes_only_changed_state() {
        let response = apply_transition(&complete_request()).unwrap();
        assert_eq!(&response[..4], b"DLR1");
        assert_eq!(response[4], 1);
        assert_eq!(i64::from_le_bytes(response[5..].try_into().unwrap()), 1_000);
    }

    #[test]
    fn rejects_unknown_command_and_trailing_bytes() {
        let mut unknown = complete_request();
        unknown[4] = 44;
        assert!(apply_transition(&unknown).is_err());
        let mut trailing = complete_request();
        trailing.push(0);
        assert!(apply_transition(&trailing).is_err());
    }
}
