# Pomodoro Timer Enhancements

Modify the focus timer logic to automatically transition to a break session after work, with configurable long break triggers.

## User Review Required

> [!IMPORTANT]
> The timer will now **auto-start** the next break session as soon as a Work session finishes. This changes the current behavior where the user had to manually start each session.

## Proposed Changes

### Focus Feature

#### [MODIFY] [FocusStore.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/focus/FocusStore.kt)
- Add `SetsUntilLongBreak` preference key (defaulting to 4).
- Update `FocusState` to include `setsUntilLongBreak`.
- Modify `finishIfDue(now: Instant)` to:
    - Automatically switch from `WORK` to either `SHORT_BREAK` or `LONG_BREAK`.
    - If `sets + 1` is a multiple of `setsUntilLongBreak`, use `LONG_BREAK`.
    - Automatically switch from `SHORT_BREAK` or `LONG_BREAK` back to `WORK`.
    - Immediately set the `End` time for the new session to start it.
- Add `setSetsUntilLongBreak(count: Int)` method.

#### [MODIFY] [FocusScreen.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/focus/FocusScreen.kt)
- Add UI to the long break edit dialog to configure "Sets until long break".

## Verification Plan

### Automated Tests
- Test `finishIfDue` with `WORK` mode and verify it switches to `SHORT_BREAK`.
- Test `finishIfDue` with `WORK` mode and `sets = 3` (when target is 4) and verify it switches to `LONG_BREAK`.
- Verify `End` is set correctly for the new session.

### Manual Verification
- Start a 1-minute Pomodoro.
- Observe it switching to a 1-minute Short Break automatically.
- Check the settings dialog for the new "Sets until long break" option.
