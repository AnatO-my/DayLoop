# P0-05 Exact Alarm Spike

Status: Verified on API 36 emulator for P0-05 required behaviours; broader API matrix remains future hardening
Date: 2026-08-26
Phase: 0
Task: P0-05 Exact alarm spike

## Scope

P0-05 is about exact alarm scheduling, ordinary process-death delivery, and exact-alarm capability detection. It does not decide full-screen alarm UI, reboot reconstruction, interval scheduling, or final Play policy eligibility. Those remain P0-06 through P0-10.

DayLoop has no production Android project yet. This spike therefore uses a disposable SDK-only Android harness under `spikes/exact-alarm/` to prove platform behaviour without establishing the production project structure.

## Current Platform Facts

- Android alarms can fire intents outside the app process lifetime, including while the app is not running.
- Exact alarms are precise user-facing interruptions and should be used only when the user-facing function requires precise timing.
- Apps targeting Android 12/API 31 or higher must declare an alarms/reminders permission before setting exact alarms with a `PendingIntent`, or a `SecurityException` can occur.
- For Android 13/API 33 and higher, `USE_EXACT_ALARM` is automatically granted but restricted by Play policy to qualifying alarm/timer/calendar core use cases.
- `SCHEDULE_EXACT_ALARM` is broader but user-controlled. The app must check `AlarmManager.canScheduleExactAlarms()` before setting exact alarms and handle revocation as a normal state.
- If `SCHEDULE_EXACT_ALARM` is revoked, Android cancels future exact alarms and the app should reschedule required alarms if access is granted again.
- A process-death test must not use user force-stop as the primary proof. A force-stopped app is a stronger Android state and can invalidate alarm delivery expectations.

## Prototype Contract

DayLoop's first alarm engine should schedule exact user-facing reminder occurrences through an explicit immutable broadcast `PendingIntent`.

The scheduled intent must include only stable identifiers and validation metadata:

- `scheduledOccurrenceId`
- `taskId`
- `reminderPolicyId`
- `triggerGeneration`
- `scheduledAtEpochMillis`
- `expectedFireAtEpochMillis`

The receiver must treat delivery as a request to re-load current canonical state, not as proof that the occurrence is still valid. If the occurrence was completed, rescheduled, canceled, superseded, or has a mismatched generation, the receiver must record a stale delivery and do nothing user-visible.

## Capability Detection

Use this policy before scheduling an exact alarm:

```kotlin
fun canUseExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager.canScheduleExactAlarms()
}
```

Scheduling code must branch on this capability:

- If exact alarms are available and the occurrence qualifies, use `setExactAndAllowWhileIdle(...)` for truly time-critical reminder delivery.
- If exact alarms are unavailable, do not crash and do not silently pretend exact delivery exists.
- Record the degraded state, show a user-facing repair path later, and schedule the best allowed fallback for the reminder contract.
- Use `WorkManager` only for deferrable follow-up work, never as the exact reminder delivery mechanism.

## Disposable Harness Design

When a temporary Android project exists, build a minimal `spikes/exact-alarm` app with:

- One Activity with buttons for capability check, request settings, schedule exact alarm, schedule fallback alarm, cancel alarm, and clear log.
- One manifest-declared `ReminderAlarmReceiver` for alarm delivery.
- One manifest-declared `ExactAlarmPermissionReceiver` for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- A tiny local log store using app-private storage or DataStore so delivery can be inspected after process recreation.
- A foreground notification or visible Activity log entry when the alarm fires; full-screen UI is out of scope until P0-06.

The core scheduling sketch should be:

```kotlin
val intent = Intent(context, ReminderAlarmReceiver::class.java)
    .setAction("dayloop.spike.DELIVER_REMINDER")
    .putExtra("scheduledOccurrenceId", occurrenceId)
    .putExtra("triggerGeneration", generation)
    .putExtra("expectedFireAtEpochMillis", fireAtEpochMillis)

val pendingIntent = PendingIntent.getBroadcast(
    context,
    occurrenceRequestCode,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    fireAtEpochMillis,
    pendingIntent,
)
```

Do not use `FLAG_ONE_SHOT` for DayLoop reminder occurrences because cancellation and replacement must remain possible.

## Manual Verification Plan

Broader hardening should later repeat this matrix on API 31/32, API 33, API 34, and API 36. P0-05 required proof was executed on API 36.

1. Install the disposable spike app.
2. Confirm the capability check result through `canScheduleExactAlarms()`.
3. Schedule an exact alarm two to three minutes in the future.
4. Kill the ordinary app process without force-stopping the package.
5. Wait for the trigger time.
6. Verify the receiver ran and logged the expected occurrence id/generation.
7. Repeat after denying exact-alarm access where the OS supports that denial path.
8. Verify the app detects denial before scheduling exact alarms and routes to fallback behavior.
9. Grant access again and verify the permission-state receiver triggers rescheduling logic.
10. Confirm stale delivery is ignored after schedule replacement or cancellation.

Suggested adb helpers, adjusted for the final package name:

```text
adb devices
adb shell pidof <package.name>
adb shell am kill <package.name>
adb logcat -s DayLoopAlarmSpike
adb shell appops get <package.name> SCHEDULE_EXACT_ALARM
adb shell appops set <package.name> SCHEDULE_EXACT_ALARM deny
adb shell appops set <package.name> SCHEDULE_EXACT_ALARM allow
```

Do not use `adb shell am force-stop <package.name>` as the ordinary process-death proof.

## Executed Verification

Environment:

- Date: 2026-08-26
- Emulator: `Pixel_9` AVD, API 36 / Android 16
- Package: `com.dayloop.spike.exactalarm`
- Build path: `spikes/exact-alarm/build/exact-alarm-spike.apk`
- Build method: Android SDK tools (`aapt2`, `javac`, `d8`, `zipalign`, `apksigner`) because Gradle/Kotlin CLI are not on PATH yet

Observed log evidence:

```text
13:45:53.976 CAPABILITY canScheduleExactAlarms=false sdk=36
13:45:55.245 SCHEDULE_FALLBACK requestedExact=true canExact=false occurrence=occ-1787748445225 generation=1 fireAt=13:47:25.225
13:46:15.213 CANCEL requestCode=41005
13:46:16.449 PERMISSION_STATE_CHANGED canScheduleExactAlarms=true
13:46:16.531 CAPABILITY canScheduleExactAlarms=true sdk=36
13:46:17.641 SCHEDULE_EXACT occurrence=occ-1787748467634 generation=2 fireAt=13:47:47.634
13:47:47.667 DELIVERED_VALID occurrence=occ-1787748467634 generation=2
```

Verification result:

- Capability denial was detected on API 36: `canScheduleExactAlarms=false`.
- A requested exact alarm degraded to fallback instead of crashing or pretending exact access existed.
- Granting exact-alarm app-op access triggered the permission-state receiver and changed capability to `true`.
- An exact alarm scheduled with `setExactAndAllowWhileIdle(...)` delivered after ordinary process death caused by `adb shell am kill com.dayloop.spike.exactalarm`.
- The receiver validated occurrence id and generation before logging `DELIVERED_VALID`.

Not covered by P0-05:

- Full-screen alarm UI.
- Reboot reconstruction.
- Interval one-shot scheduling.
- API 31/33/34 matrix verification.
- Play policy eligibility for `USE_EXACT_ALARM`.
## Result

P0-05 is complete for its required Phase 0 proof. The verified implementation contract is:

- DayLoop must gate exact scheduling behind `canScheduleExactAlarms()` on Android 12+.
- DayLoop must use explicit immutable broadcast `PendingIntent` identity for scheduled occurrences.
- DayLoop must handle stale delivery as normal.
- DayLoop must provide a degraded fallback path when exact-alarm capability is unavailable.
- A disposable Android spike app is required before P0-05 can be checked off.

## Sources Reviewed

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/reference/android/app/AlarmManager
- https://developer.android.com/reference/android/app/PendingIntent
- https://developer.android.com/reference/android/content/BroadcastReceiver
- https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1


