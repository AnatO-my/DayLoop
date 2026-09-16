# Focus notifications - 2026-09-10

## Implemented
- Service owns due-session settlement, independent of the foreground screen.
- Notification label follows activeMode, not the currently viewed tab.
- Immediate initial foreground notification; subsequent updates use notify.
- UI service startup no longer occurs once per second.
- Android 16 promoted ongoing notification request and manifest permission.
- Xiaomi OS2/OS3 structured extras, Parcelable icon bundle, version and permission checks.
- Ordinary ongoing countdown notification remains the fallback.

## Not guaranteed
Global HyperOS 3 in Nigeria may not expose or authorize the documented Xiaomi domestic integration. No Xiaomi APP_ID, certificate registration, approved timer business scenario, or device authorization has been supplied. Do not invent credentials or claim the device will render an island.

An active foreground service is not a wake lock or exact alarm. Session settlement occurs when the process runs; exact transitions during deep sleep, reboot recovery and forced-stop recovery are not added by this patch. Existing late-settlement behavior starts the next interval from the settlement time.

The Xiaomi compact small-island payload shows the timer icon; the larger presentation and ticker carry the remaining time. OEM presentation still requires real-device acceptance.

## Verification
Source reviewed. Three payload tests and one headless store transition test authored but not run. No builder, emulator, connected tests, install, uninstall or device commands were used. Existing tests may contain unrelated stale expectations.

Manual checks: start Pomodoro, view another mode, background across completion, confirm next-session label/countdown and credit; pause from notification; deny notification permission and confirm app remains usable; inspect Global HyperOS live-notification availability. Never uninstall the app to reset permissions or channels.

## Sources
- https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2131
- https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2132
- https://developer.android.com/develop/ui/views/notifications/live-update
