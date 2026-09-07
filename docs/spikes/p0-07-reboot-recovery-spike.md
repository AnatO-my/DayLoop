# P0-07 Reboot Recovery Spike

Date: 2026-08-27
Status: Verified on Android API 36 emulator (`Pixel_9`, `emulator-5554`)
Harness: `spikes/exact-alarm/`

## Question

Can DayLoop reconstruct valid scheduled alarm work from persisted state after a device reboot, without relying on `AlarmManager` to remember registrations?

## Result

Yes. The spike persisted the scheduled occurrence before reboot, received `android.intent.action.BOOT_COMPLETED` after reboot, reloaded the same occurrence/generation from app storage, and registered a new exact `RTC_WAKEUP` alarm for the same fire time.

## Evidence

Pre-reboot persisted state:

```text
BOOT_RECEIVER enabled=true
SCHEDULE_EXACT source=ui-command occurrence=occ-1787792814630 generation=1 fireAt=02:06:54.630
scheduleActive=true
requestedExact=true
```

Post-reboot app log:

```text
BOOT_RECEIVED action=android.intent.action.BOOT_COMPLETED
REBOOT_RELOAD active=true occurrence=occ-1787792814630 generation=1 fireAt=02:06:54.630
SCHEDULE_EXACT source=boot-completed occurrence=occ-1787792814630 generation=1 fireAt=02:06:54.630
```

Post-reboot system alarm service:

```text
RTC_WAKEUP origWhen=2026-08-27 02:06:54.630 com.dayloop.spike.exactalarm
tag=*walarm*:dayloop.spike.DELIVER_REMINDER
exactAllowReason=permission
operation=PendingIntentRecord{... com.dayloop.spike.exactalarm broadcastIntent}
```

## Implementation Notes

- `AlarmManager` registration is treated as volatile runtime state. DayLoop must persist canonical scheduled occurrence state separately.
- The boot receiver is disabled by default and enabled only while active schedules exist.
- The boot receiver must be system-deliverable when enabled. In this harness that required `android:exported="true"` for `BootRecoveryReceiver`, while still keeping the receiver disabled until needed.
- On `BOOT_COMPLETED`, recovery reloads persisted state, rejects inactive/past/invalid entries, and re-registers the same occurrence/generation.
- Recovery reuses the exact-alarm capability gate and fallback branch from P0-05.
- The boot receiver does quick reconstruction work only. It does not launch UI.

## Deferred Questions

- Direct Boot / user-locked storage path is deferred. MVP can begin with credential-protected state if the product accepts recovery after normal boot/user unlock behavior.
- Physical-device and OEM boot-delivery variance remains future hardening.
- Time change and timezone-change recovery are separate Phase 0 or later hardening topics.

## Sources

- Android alarm scheduling guide: https://developer.android.com/develop/background-work/services/alarms
- Android broadcasts guide: https://developer.android.com/develop/background-work/background-tasks/broadcasts
- `Intent.ACTION_BOOT_COMPLETED` reference: https://developer.android.com/reference/android/content/Intent#ACTION_BOOT_COMPLETED
