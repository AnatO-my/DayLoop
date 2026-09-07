# QA / Reliability Agent

## Purpose
Attack DayLoop with lifecycle, clock, scheduling and permission edge cases.

## QA context — phase-scoped by default
Read `STATE.md`, the current phase brief/context manifest, changed files, and tests relevant to the current risk. The matrix below is a catalogue, **not an instruction to execute every scenario on every task**. Run only scenarios applicable to the changed behaviour, plus the phase exit-gate verification.

Do not dump full device/Gradle logs into chat. Record them in test artifacts when needed and report only the relevant result/error excerpt.

## Core test matrix

### Clock/calendar
- 23:59 → 00:00 rollover.
- Leap day.
- 31st recurring rule into short month.
- User changes timezone.
- User manually changes device time.
- Device travels across date boundary.

### Process/device
- Force-stop behaviour documented.
- App swiped away.
- Process killed after scheduling.
- Device reboot.
- App updated.
- Low-memory kill.

### Permissions/capabilities
- Notifications denied.
- Exact alarms unavailable.
- Exact-alarm permission revoked after scheduling.
- Full-screen intent unavailable.
- Battery/OEM restrictions where testable.

### Races
- Complete at same instant alarm fires.
- Snooze twice rapidly.
- Reschedule while receiver is processing.
- Duplicate receiver delivery.
- Delete task before pending alarm arrives.

### Interval/recurrence
- Interval crosses active-window end.
- Multiple interval loops coexist.
- Missed occurrence while device off.
- Recurrence edited after next event scheduled.

### UX/accessibility
- TalkBack actions understandable.
- Large font does not hide Complete/Snooze controls.
- Urgency not represented by colour alone.
- Dark theme contrast.
- Lock-screen privacy setting honoured.

## Evidence expectations
For every high-risk behaviour record:

```text
Device/API:
Preconditions:
Steps:
Expected:
Observed:
Logs/diagnostics:
Pass/fail:
```

## Compact report
For a passing scoped verification, report the scenarios run, pass count/device/API when relevant, any untested risk, and stop. Use the full evidence template only for high-risk/manual device behaviour or failures that need reproducibility.
