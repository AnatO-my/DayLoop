//! Command-oriented pure task transitions.

use dayloop_domain::{CivilDate, Task};

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum TaskCommand {
    Complete { at_epoch_millis: i64 },
    Reopen,
    Skip,
    MoveToSomeday,
    KeepForToday { date: CivilDate },
}

/// Applies no I/O and has no platform effects. Persistence and alarm effects are
/// deliberately sequenced by the existing Kotlin coordinator during migration.
pub fn apply_task_command(task: Task, command: TaskCommand) -> Task {
    match command {
        TaskCommand::Complete { at_epoch_millis } => task.complete(at_epoch_millis),
        TaskCommand::Reopen => task.reopen(),
        TaskCommand::Skip => task.skip(),
        TaskCommand::MoveToSomeday => task.someday(),
        TaskCommand::KeepForToday { date } => task.keep_for_today(date),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use dayloop_domain::{TaskId, TaskStatus};

    #[test]
    fn command_keeps_the_first_completion_time() {
        let task = Task::new(TaskId::new("task").unwrap(), "Read").unwrap();
        let done = apply_task_command(task, TaskCommand::Complete { at_epoch_millis: 4 });
        let repeated = apply_task_command(done, TaskCommand::Complete { at_epoch_millis: 5 });
        assert_eq!(repeated.status, TaskStatus::Completed);
        assert_eq!(repeated.completed_at_epoch_millis, Some(4));
    }
}
