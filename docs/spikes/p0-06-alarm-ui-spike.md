# P0-06 Alarm UI Spike

Status: Verified on API 36 emulator for notification/full-screen intent comparison; physical-device visual QA remains future hardening
Date: 2026-08-26
Phase: 0
Task: P0-06 Alarm UI spike

## Scope

P0-06 compares normal high-priority reminder notifications with alarm-style notifications that request a full-screen intent. It does not finalize Google Play eligibility, exact-alarm permission policy, reboot recovery, interval scheduling, or production UI design.

This spike extends the disposable SDK-only harness under `spikes/exact-alarm/`. It is not the production DayLoop Android project structure.

## Current Platform Facts

- Android 13/API 33 and higher gates non-exempt notifications behind the runtime `POST_NOTIFICATIONS` permission.
- Android 8/API 26 and higher require notification channels. Channel importance controls interruption behavior, and users can change channel behavior after creation.
- `NotificationManager.IMPORTANCE_HIGH` makes notifications noisy and capable of peeking; it does not by itself launch a full-screen Activity.
- Android 14/API 34 and higher restrict full-screen intents. Apps should check `NotificationManager.canUseFullScreenIntent()` before relying on alarm-style full-screen behavior.
- Google Play policy says Android 14+ `USE_FULL_SCREEN_INTENT` auto-grant eligibility is limited to apps whose core function is alarms or calls. Other cases require user consent and clear explanation.

## Harness Changes

The disposable harness now includes:

- `POST_NOTIFICATIONS` and `USE_FULL_SCREEN_INTENT` manifest declarations.
- A high-importance notification channel named `dayloop_spike_reminders`.
- A normal high-priority reminder notification path.
- An alarm-category notification path that calls `setFullScreenIntent(...)`.
- `FullScreenAlarmActivity`, the Activity target for full-screen intent launch.
- `UiCommandReceiver`, an exported spike-only receiver so ADB can post notifications while the app is backgrounded.

## Executed Verification

Environment:

- Date: 2026-08-26
- Emulator: `Pixel_9` AVD, API 36 / Android 16
- Package: `com.dayloop.spike.exactalarm`
- Build path: `spikes/exact-alarm/build/exact-alarm-spike.apk`
- Build method: Android SDK tools (`aapt2`, `javac`, `d8`, `zipalign`, `apksigner`)

Commands used included:

```text
adb shell pm grant com.dayloop.spike.exactalarm android.permission.POST_NOTIFICATIONS
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.CHECK_UI
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.POST_HIGH
adb shell am broadcast -n com.dayloop.spike.exactalarm/.UiCommandReceiver -a dayloop.spike.POST_FULL_SCREEN
adb shell appops set com.dayloop.spike.exactalarm USE_FULL_SCREEN_INTENT deny
adb shell dumpsys notification --noredact
adb shell dumpsys activity activities
```

Observed app log evidence:

```text
13:59:15.037 UI_CAPABILITY notificationsEnabled=true postGranted=true canUseFullScreenIntent=true channelImportance=missing sdk=36
13:59:27.898 NOTIFICATION_POSTED type=HIGH_PRIORITY postGranted=true canUseFullScreenIntent=true
13:59:49.603 NOTIFICATION_POSTED type=FULL_SCREEN_REQUEST postGranted=true canUseFullScreenIntent=true
14:00:17.157 UI_CAPABILITY notificationsEnabled=true postGranted=true canUseFullScreenIntent=false channelImportance=4 sdk=36
14:00:35.947 NOTIFICATION_POSTED type=FULL_SCREEN_REQUEST postGranted=true canUseFullScreenIntent=false
```

Observed system notification evidence:

- High-priority path produced `importance=4`, `category=reminder`, and `fullscreenIntent=null`.
- Allowed full-screen request produced `importance=4`, `category=alarm`, and a non-null `fullscreenIntent` in the notification record.
- Denied full-screen request produced `importance=4`, `category=alarm`, `fullscreenIntent=null`, and `FSI_REQUESTED_BUT_DENIED` in the notification flags.
- On the headless emulator, the full-screen Activity did not become the top resumed Activity during this run; launcher remained top after wake. Treat that as a platform-state limitation of the test, not as permission failure, because the notification record still showed whether the full-screen intent was attached or denied.

## Result

P0-06 is complete for its Phase 0 comparison. The verified implementation contract is:

- DayLoop must always check notification permission before posting user-facing reminders on Android 13+.
- DayLoop must create reminder notification channels deliberately and inspect channel importance when diagnosing degraded reminder UI.
- Normal reminders can use a high-importance reminder notification without a full-screen intent.
- Alarm-grade reminders must check `canUseFullScreenIntent()` before claiming full-screen capability.
- If full-screen access is unavailable, Android can strip the full-screen intent and record `FSI_REQUESTED_BUT_DENIED`; DayLoop must fall back to a high-importance alarm-category notification plus an explanatory repair path.
- Even when full-screen access is reported available, production must not promise that every delivery will visually take over the screen in every device state. Physical-device and Android Studio visual QA remain required before shipping alarm-grade UI.

## Deferred Decisions

- Which DayLoop reminder contracts qualify for alarm-style UI.
- Exact user-facing copy for notification/full-screen denial repair.
- Whether DayLoop can satisfy Google Play full-screen intent auto-grant criteria.
- Full-screen Activity design and resolution actions.
- Physical-device verification across locked, unlocked, screen-off, DND, and OEM-specific states.

## Sources Reviewed

- https://developer.android.com/reference/android/app/NotificationManager
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
- https://developer.android.com/about/versions/14/behavior-changes-14
- https://developer.android.com/develop/ui/compose/notifications/channels
- https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1
