# HyperOS Timer Handoff

## Scope and verification
Approved: native-style island countdown with Timer (Pomodoro), Coffee (Short break), Armchair (Long break), persistent paused state, Pause/Resume and End controls, direct Focus entry.

Implemented in source. Icon XML parses, notification drawable references resolve, and git diff whitespace checks pass. A first resource-check script incorrectly treated android.R.drawable.ic_media_play as an app resource; corrected check passed. No Gradle/build/lint, Kotlin tests, emulator, ADB, install/uninstall, app-data clearing or physical-device operations ran. Exact Global HyperOS 3 appearance remains unverified.

## Architecture
- FocusNotificationPresentation derives display mode from activeMode before mode, clamps/rounds remaining seconds through the existing secondsAt, formats minutes without wrapping at 60, and maps modes to three new lime VectorDrawables.
- HyperOsFocusNotification retains permission/protocol gating. OS3 uses summary template 6: imageTextInfoLeft.picInfo for the icon and sameWidthDigitInfo.digit for the countdown. Removed redundant right-side icon and compact mode text. Small island receives the mode icon.
- Expanded template 13 combines highlightInfo (remaining time, mode, Paused/Remaining) with actions. OS2 receives expanded/ticker fields but no OS3 island layout. Unsupported/denied devices still receive standard Android notifications.
- Mode icons are Lucide timer/coffee/armchair, adapted to 24-unit Android vectors. License/source attribution is bundled at assets/licenses/lucide_isc.txt. Standard Android small icons may be system-tinted; OEM icon bundles retain their resource color.
- FocusService uses one stable notification ID, retains ongoing paused notifications, publishes Resume or Pause plus End, and supplies matching Notification.Action objects to the Xiaomi bundle. A content intent routes to Focus through the existing widget_route dispatcher; it does not start a new timer.
- Foreground placeholder is posted immediately before DataStore/OEM queries. Explicit NONE sound mapping completes the existing sound switch. Existing sound/haptics/completion behavior otherwise remains unchanged.
- Notification controls carry an immutable snapshot token in both URI identity and extras. FocusStore.applyNotificationCommand validates it within one DataStore edit. notification_revision is a new backward-compatible Long preference defaulting to zero, incremented by timer transitions/mutations. It prevents an old paused notification from matching a later session with identical time remaining. Merely viewing a different mode does not invalidate active-session controls.
- Pause/Resume do not switch the active mode. End clears the active deadline/mode and restores the viewed mode's configured duration; accumulated minutes and sets are retained. Expired controls do not mutate an elapsed session, allowing existing finishIfDue to settle it and transition normally. No new database table or timer engine.

## Deliberate limitations
- Kept existing one-second publication while running and no ticking loop while paused. Using a supported digit field achieves the visual template without guessing timerInfo semantics. The official PDF describes timerSystemCurrent but some examples say timerCurrent; native timestamp/countdown behavior needs on-device validation before reducing update frequency. No battery improvement claimed.
- HyperOS decides admission, capsule size, animation, placement, lifetime and action rendering. Ongoing is a request, not a guarantee of indefinite island visibility. No system app impersonation, whitelist bypass or private Clock APIs.
- Existing unrelated duration/editor/history semantics were not refactored. Older pre-revision notification controls are ignored until replaced by a fresh notification.

## Tests authored, not executed
- FocusNotificationPresentationTest: active/viewed mode separation, frozen paused value/label, expiration clamp and 180-minute format.
- HyperOsFocusNotificationTest: OS0/1 fallback, OS2 fields, OS3 digits/no duplicate icon, two actions, distinct mode resources and paused label.
- FocusDurationTest additions: idempotent resume/stale end, selected-tab independence, End preserves history/sets, expired actions cannot end the successor, and identical future paused sessions reject old tokens.

## Manual acceptance
1. User builds/updates without clearing app data. Start each mode and inspect its icon and countdown in compact/expanded notifications.
2. Pause from island. Wait, reopen it, resume; remaining time must stay frozen while paused.
3. View another timer tab while one is active; island icon/name and actions must still refer to the active timer.
4. End running and paused sessions; notification/audio should stop, saved completed minutes/sets remain.
5. Rapidly repeat actions and cross a completion transition; stale controls must not affect the next session.
6. Tap standard/island notification from a cold and warm app; Focus should open without changing timer state.
7. Review non-Xiaomi/permission-denied fallback and long custom durations. Inspect 180:00 fitting on-device.

## References
- Xiaomi official integration guide: https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2131
- Official template PDF linked from that guide, dated 2026-01-29: expanded template 13, summary template 6, highlightInfo/actions/sameWidthDigitInfo sections. Downloaded to OS temp for inspection, not added to app assets.
- https://lucide.dev/icons/timer
- https://lucide.dev/icons/coffee
- https://lucide.dev/icons/armchair

Next action: manual user acceptance and, only when authorized, targeted compile/tests. Do not start new timer architecture or unrelated UI work.
