# P8-04 Alarm-grade UI Refinement

Refine the eligibility for the alarm-grade surface (Full-screen Intent) to ensure it is only used for `STRICT` reminders, matching the requirements in [ADR 0005](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/docs/adr/0005-alarm-capability-policy.md).

## User Review Required

> [!NOTE]
> This change restricts the full-screen intent to `STRICT` reminders only. `PERSISTENT` reminders will still use the high-priority `ALARM_CHANNEL` and appear as heads-up notifications (if `alarmGradeEnabled` is true), but they will no longer take over the full screen when the device is locked. This aligns with the "Usually no" eligibility for Persistent reminders in the accepted policy.

## Proposed Changes

### Core Alarms / Notifications

#### [MODIFY] [ReminderNotifications.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/core/notifications/ReminderNotifications.kt)
- Refine the `fullScreen` eligibility logic to check for `ReminderMode.STRICT`.
- Ensure the `ALARM_CHANNEL` is still used for both `PERSISTENT` and `STRICT` when `alarmGradeEnabled` is true to maintain "alarm-style" (loud/re-alerting) behavior.

## Verification Plan

### Automated Tests
- Verification is currently waived as per the Phase 8-10 batch approval.
- However, existing `AlarmNotificationActionProcessorTest` ensures the resolution logic remains sound.

### Manual Verification
- This will be recorded as "implemented, unverified" in `STATE.md`.
- Final verification on a physical device/emulator will occur after Phase 10.
