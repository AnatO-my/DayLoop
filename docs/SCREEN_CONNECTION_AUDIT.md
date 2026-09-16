# Screen and Appearance Audit

## Current connection batch: 2026-09-08

The following source changes supersede the initial audit below. The user requested no builder/build/test/emulator verification and will report issues manually. None of this batch has been compiled or run.

| Surface | Now connected in source |
| --- | --- |
| Today | Add task, title-to-editor, completion, completed-task editing/reopening, ordinary-task deletion, project selection and saved editor drafts. Generated work has date-specific persistence and is deduplicated against projected tasks. |
| Task editor | Save through AlarmCoordinator and transactional task/next-occurrence persistence; changed schedules cancel old reminders. Missing tasks, write errors and deletion confirmation have UI states. Removing a deadline actually removes it. |
| Plans / Goals | Correct tab routing; create/edit projects and goals; project-to-goal linking; linked task creation/editing; status and target date controls; derived progress and explicit manual goal progress. |
| More | Appearance, capability repair, error handling, main navigation and Idle entry. Capabilities refresh on activity resume. |
| Reminder | New Reschedule notification actions launch MainActivity directly and validate occurrence generation/action policy before opening the editor. In-app reminder rescheduling also reaches the editor. |
| Focus | Pomodoro, short/long breaks, start/pause/reset, persisted remaining time/deadline, completed work minutes and focus streak. Mode changes reset the timer. DataStore history is bounded to 366 dates. |
| Idle | Current date and month-dot calendar, scheduled-work completion markers, manual entry from Today calendar or More, and return to Today. No tab bar. |

Navigation uses saved route state and Back handling. Foreground time updates stop when the activity pauses. Today uses real completed-focus totals and focus streak; the undefined Score placeholder is replaced by explicit task Progress.

## Current limits and user checks

- No build, lint, unit-test run, emulator run, install or screenshot was performed for this batch. Five regression cases were added but not executed. The earlier 298-test/build checkpoint predates these edits.
- Focus deadline state survives app restarts but does not run a background alarm/service or post completion notifications. Completed sessions are settled when the app next runs. Manual wall-clock changes affect its persisted deadline. Only completed work sessions contribute minutes; pauses retain remaining time, reset/mode changes discard partial progress.
- Idle markers indicate completed work scheduled on a date, not a historical completion timestamp. Auto-entry, boot auto-start and always-on display are not implemented.
- Task reminder saves use the existing NextOccurrenceCalculator; this does not expand the recurrence engine or add timed alarms to date-only recurrence/all-day models.
- Older already-posted notifications may retain their previous action PendingIntents until reposted. New reschedule actions use a direct activity PendingIntent, per [Android notification trampoline guidance](https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines).
- Onboarding awaits acceptance/artwork. Final icons, navigation animation, a draggable schedule ruler, project-protocol editing and advanced palette/font controls remain deferred.
- Widget projection, manual-theme parity and time-boundary refresh remain unchanged and are not covered by this screen batch.
- Suggested manual flow: create goal -> create linked project -> add task -> edit schedule -> complete -> reopen -> delete; navigate all tabs; switch themes; restart Focus; open Idle; use a newly posted reminder's Reschedule action.
- Asset sizes and onboarding recommendation: `ONBOARDING_AND_ASSET_HANDOFF.md`.

## Initial audit retained for context

The material below describes the repository before this connection batch, not its current connection status.

Repository inspection: 2026-09-08. This describes the current source, not phase completion claims or mock-ups.

## Existing screens

| Surface | Current implementation | Connections still needed |
| --- | --- | --- |
| Today | Matcha dashboard, derived Room task/project data, progress, sections, completion affordances, reminder warning, bottom navigation. | MainActivity does not supply Add task or completion callbacks. Menu and calendar buttons are TODOs. Focus duration, streak, and focus score are placeholders. |
| Task editor / Quick Add | Draft validation and task construction; schedule, recurrence, interval and reminder controls exist. Material components consume the shared theme. | No application route opens either component. Save needs repository/coordinator integration; editing existing tasks and returning to Today need wiring. Proposed standalone time-ruler editor is not implemented. |
| Plans / Goals | Room-backed project and goal lists and an internal Projects/Goals selector. | Goals bottom-tab selection routes to plans and always initializes Projects; selecting Goals while in Plans also does not switch the internal tab. Add/edit/detail controls and derived progress presentation are absent. |
| More / Diagnostics | Today warning and More open diagnostics. Capability settings intents and manual reconciliation are connected. Appearance controls now persist System/Light/Dark. | More currently is diagnostics rather than a complete settings destination. Capability snapshots need refresh after returning from Android settings. |
| Reminder activity | Full-screen reminder, privacy gate, Complete/Snooze actions and Open Today. Uses the saved appearance preference. | Reschedule emits edit_task_id, but MainActivity does not consume it. Validate the complete permission/lock-screen flow separately. |
| Home-screen widget | Glance projection, open-Today action and completion callback exist. Uses Glance system-aware colors. | Manual app theme/palette is not projected into Glance. Snapshot refresh is data-driven and needs a day/time boundary strategy. Widget project projection differs from Today. |
| Onboarding / Welcome | No feature screen or route in current source. | Implement first-run persistence, assets, CTA route and onboarding UI. |
| Focus | Bottom navigation label only; selection has no destination. | Implement timer/session state, screen and navigation. Reminder diagnostics are not a focus timer. |
| Idle / Ambient | No feature screen or route in current source. | Implement calendar visualization, entry/exit policy and settings. Background auto-start is a separate deferred platform decision. |

## Cross-screen findings

- MainActivity navigation uses remember rather than saved state, and has no explicit back handling. Recreation returns to Today.
- Today captures time once per route composition; the visible day and Now/Next classification need lifecycle/time-boundary refresh.
- Today and Diagnostics capture reminder capabilities once per composition; permission changes can leave stale warnings until the route is recreated.
- The bottom bar exposes state and icon slots, but animation and final icon assets remain pending.
- DayLoopWarningBanner does not invoke a supplied leading icon lambda; fix and test that slot when integrating final assets.
- Do not mark an underlying model, isolated composable, or callback test as an end-to-end screen connection.

## Appearance work

- MainActivity and ReminderActivity now apply persisted ThemeMode to DayLoopTheme.
- More > Appearance exposes System, Light and Dark using radio selection; failed preference writes display an error and do not claim success.
- Both Material palettes now explicitly define container, inverse, tint and error-container roles. Custom cards and navigation retain their semantic DayLoop colors.
- Progress-ring text inherits its parent's content color, including the near-black hero.
- Runtime window background and system-bar icon contrast follow the selected appearance. Android 8.0 retains a dark navigation bar for its light system buttons. Night resources cover system-dark launch windows.
- Android's initial launch window follows the device theme before DataStore is read; a manual app override is applied when preferences arrive. No custom splash mechanism is added.
- Curated alternate palettes, custom font-size settings, final SVG assets and animated navigation remain deferred.
- Glance and Android-owned permission/settings screens follow their own system theme; the in-app preference applies to DayLoop Compose screens.

System-bar API reference: [Android WindowInsetsControllerCompat](https://developer.android.com/reference/androidx/core/view/WindowInsetsControllerCompat), checked 2026-09-08.

## Verification

- 298 JVM tests passed, including ThemeMode resolution and existing DataStore preference tests.
- `scripts/verify-local.ps1` passed: module unit tests, app APK and instrumentation APK builds.
- Two new Compose appearance tests compile, covering Light/Dark selection, semantic contrast and actual progress-label pixels; device execution did not start because the Android test-runner dependency download from dl.google.com timed out.
- Initial lint found two API-27 XML attributes; corrected for minSdk 26. The final combined lint/device run stopped before a final lint report, so lint remains unconfirmed.
- User requested personal confirmation of the implementation. Visual inspection, restart persistence, reminder activity appearance, system-mode switching and large-text/device coverage remain pending. No successful device check is claimed.
- Manual entry: More > Appearance > Light / Dark / Use system setting. Check Today and Plans after selection, then close/reopen the app to confirm persistence.
- No additional emulator verification should run unless the user requests it. Do not treat this audit as verification of disconnected workflows.

## Suggested repair order

1. Connect Today Add task -> editor -> validated repository save -> observed Today, then completion through the existing coordinator/repository boundary.
2. Connect edit_task_id from reminder rescheduling, including cold start and an already-open app.
3. Repair Goals tab state, saved navigation/back handling and permission/time refresh.
4. Add project/goal editing and derived progress presentation.
5. Implement remaining proposed screens as separate approved slices; replace placeholder icons/metrics and add navigation animation after destinations work.

Evidence: MainActivity.kt; feature/today, feature/taskeditor, feature/plans, feature/diagnostics; core/notifications/ReminderActivity.kt; widget/TodayWidget.kt and WidgetStateReconciler.kt; ui/theme and ui/components/DayLoopComponents.kt.
