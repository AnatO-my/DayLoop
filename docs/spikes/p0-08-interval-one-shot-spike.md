# P0-08 Interval One-Shot Spike

Date: 2026-08-27
Status: Verified on Android API 36 emulator (`Pixel_9`, `emulator-5554`)
Harness: `spikes/exact-alarm/`

## Question

Can DayLoop schedule interval reminders as a persisted one-shot chain, without a polling loop or Android repeating alarm?

## Result

Yes. The spike persisted an interval rule plus the currently scheduled occurrence, registered exactly one `RTC_WAKEUP` alarm, and scheduled the next one-shot occurrence only after validating delivery of the current occurrence.

## Verified Behaviour

Final interval chain command:

```text
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.SCHEDULE_INTERVAL --el intervalMs 15000 --ei maxDeliveries 2 --ei windowStartMinute 0 --ei windowEndMinute 1440
```

Observed app log:

```text
INTERVAL_START source=ui-command intervalMs=15000 window=0-1440 maxDeliveries=2 firstFireAt=02:15:04.440
SCHEDULE_EXACT source=ui-command-interval occurrence=interval-1-1787793304440 generation=1 fireAt=02:15:04.440
DELIVERED_VALID occurrence=interval-1-1787793304440 generation=1
INTERVAL_NEXT delivered=1/2 nextOccurrence=interval-2-1787793319464 generation=2 fireAt=02:15:19.464
SCHEDULE_EXACT source=interval-delivery occurrence=interval-2-1787793319464 generation=2 fireAt=02:15:19.464
DELIVERED_VALID occurrence=interval-2-1787793319464 generation=2
BOOT_RECEIVER enabled=false
INTERVAL_CHAIN_STOP reason=max-deliveries delivered=2/2
```

Final persisted state:

```text
intervalDeliveryCount=2
scheduleActive=false
intervalActive=false
```

Active-window edge check:

```text
SCHEDULE_INTERVAL --el intervalMs 15000 --ei maxDeliveries 1 --ei windowStartMinute 0 --ei windowEndMinute 1
INTERVAL_START source=ui-command intervalMs=15000 window=0-1 maxDeliveries=1 firstFireAt=00:00:00.000
SCHEDULE_EXACT source=ui-command-interval occurrence=interval-3-1787871600000 generation=3 fireAt=00:00:00.000
CANCEL requestCode=41005
scheduleActive=false
intervalActive=false
```

This proved that when the next interval candidate is outside the active local-time window, the harness moves the next one-shot to the next eligible window instead of scheduling inside closed time.

## Implementation Contract

- Interval reminders must persist both the interval rule and the current scheduled occurrence.
- The scheduler must register only the next one-shot occurrence.
- Valid delivery increments the interval chain state, computes the next eligible occurrence, persists it, and registers one new alarm.
- Stale delivery remains harmless because occurrence id, generation, and expected fire time are validated before the interval chain advances.
- Completion, cancellation, snooze, reschedule, or a policy cap must stop the chain and clear active interval state.
- Reboot recovery can reconstruct the active next occurrence because it uses the same persisted scheduled state model from P0-07.
- The spike uses a delivery-anchored interval chain for proof. Production Phase 7 still needs the final product decision for clock-anchored versus acknowledgement-anchored interval semantics.

## Deferred Questions

- Full production interval rules for eligible days, holidays, missed occurrences, and anchor mode are deferred to Phase 7.
- Exact-alarm eligibility for interval reminders depends on P0-09 policy review and P0-10 ADR decisions.
- Broader API/OEM matrix testing remains future hardening.

## Sources

- Android alarm scheduling guide: https://developer.android.com/develop/background-work/services/alarms
- `AlarmManager` reference: https://developer.android.com/reference/android/app/AlarmManager
- `PendingIntent` reference: https://developer.android.com/reference/android/app/PendingIntent
