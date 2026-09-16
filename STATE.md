# Project State

## Current phase
**Phase 11 — Product Quality, Accessibility and Battery Discipline**

**Phase brief:** `phases/11-quality.md`
**Context manifest:** `contexts/phase-11.context.md`

## Status
Non-Material Design System foundation (2026-09-14): Started architectural shift to remove Google Material dependency. Created `DayLoopDesignSystem.kt` with custom `DayLoopColors`, `DayLoopTypography`, and `DayLoopShapes`. Implemented `MatchaCard` using `GenericShape` to match the distinctive geometric style of Reference 2. Created standalone `DayLoopText` and `DayLoopBox` primitives to enable a purely handcrafted UI.

Phase 12: The Sentient Loop (2026-09-13): implemented deep mascot personality with `SLEEPY`, `ENERGETIC`, and `STARGAZING` moods. Added `FocusHeartbeat` haptic feedback in `FocusService`. Created `NaturalIntentParser` for on-device natural language planning. Upgraded Mascot Modal with unread "Gifts" (milestone rewards) and a Chat UI for instant task/timer actions. Synced mascot mood to `TodayWidget` and created a new `GoalWidget`.

UI Refinement and mascot depth (2026-09-13): refined navigation gestures to prevent accidental settings activation. Deepened mascot integration into Plans, Goals, Focus reflection and Efficiency Report screens. Added `EXCITED` mascot mood and made the Today mascot always clickable for encouragement. Redesigned `TodayWidget` with DayLoop's "Matcha" visual identity, bold typography and brand iconography. Improved Focus sound selectors with descriptive emojis and labels.

Technical hardening and data portability (2026-09-12): implemented Upgrade 6. Added `UsageStats` to track cumulative focus time and background service activity. Created `EfficiencyReport` dashboard to show battery impact scores. Implemented `BackupManager` for encrypted AES-256 data exports/imports of app settings and tasks. Integrated backup/restore and efficiency controls into the Diagnostics screen.

Mascot Assistant, celebrations and interactive widget (2026-09-10): implemented Upgrade 4. Moved AI Assistant suggestions into a clickable Mascot modal. Mascot now "sparkles" (SUGGESTING mood) when tips are available and "looks down" at the suggestion inside the modal. Added a confetti celebration (`CelebrationOverlay`) when all tasks are completed. Updated `TodayWidget` with a "START FOCUS" button that triggers the background focus service.

DayLoop Assistant and intelligent planning (2026-09-10): implemented Upgrade 2. Created `TodayAssistant` logic engine for schedule gap detection and predictive task placement. Added `AssistantSuggestion` and `SuggestionType`. Updated `TodayUiState` to include assistant suggestions. Created `TodayAssistantCard` UI component. Integrated Assistant into `TodayScreen` with one-tap scheduling that converts "Anytime" tasks into "Now" time blocks. Added `aiAssistantEnabled` toggle to appearance settings.

Subtasks and task management maturity (2026-09-10): implemented Upgrade 3. Added `Subtask` model and updated `Task` to support a checklist of sub-items. Implemented database migration v8->v9 to store subtasks as JSON. Updated `TaskEditorScreen` to allow adding/deleting subtasks. `TodayScreen` now shows subtask progress (e.g., "1/3") and allows expanding task cards to check off subtasks inline.

Mascot moods and saved palettes (2026-09-10): implemented Upgrade 5. Added `MascotMood` (NEUTRAL, FOCUSED, ACCOMPLISHED, OVERWHELMED) to `DayLoopMascot` with distinct visual animations. `TodayUiState` now derives the mascot's mood based on active focus timers, overdue tasks, and day completion. Added "Saved Palettes" to appearance settings, allowing users to save custom HEX colors and switch between them. Palettes are persisted via DataStore JSON.

Focus integrated sounds and reflections (2026-09-10): implemented Upgrade 1. Added `FocusSound` enum (Rain, Fire, Stream, Library, Cafe) and `lastSessionReflection` to `FocusStore`. `FocusService` now loops ambient audio tracks while a timer is running, managing `MediaPlayer` and `AudioManager` (ducking). `FocusScreen` and `FullscreenTimerView` include sound selector icons. A "Session complete" dialog prompts for work reflections when a Pomodoro finishes. 5 MP3 tracks imported to `res/raw`.

Focus compile follow-up (2026-09-10): replaced unavailable platform promoted-notification constant with AndroidX NotificationCompat equivalent; converted loop delay to Kotlin Duration, removed redundant pre-26 startup branch and unused stop helper. Source-only checks; compilation and device behavior not verified.

Focus notification repair (2026-09-10): service now settles due sessions independently of Compose, posts foreground notification before asynchronous reads, labels the active mode, and updates rather than re-promoting every second. UI starts service only when foreground/running state changes. Android 16 promoted ongoing permission/request added. Replaced speculative Xiaomi extras with structured protocol-specific payload and Icon Bundle, guarded by OEM focus permission/version query. Global HyperOS 3 island eligibility remains external and unverified; no fabricated Xiaomi app ID or authorization bypass. Standard countdown remains fallback. Four regression tests authored, not executed; no build, emulator, install or phone commands. Next: manual device validation and authorized Xiaomi Global integration eligibility, not a claim of working island rendering.

Focus decoupling and auto-transition (2026-09-09): decoupled viewed focus mode from the active running timer. Added `activeMode` to `FocusState` and `selectMode(mode)` to `FocusStore`. The timer now auto-starts the next session (Break after Work, Work after Break) and continues in the background even if the user views another tab. Added configurable "Sets until long break" (1-20 sets) to the Long Break edit dialog. Tab row now shows a small indicator dot for the active timer.

Focus duration follow-up (2026-09-08): long-press Pomodoro/Short break/Long break to edit 1-180 minutes, cancel or restore the default value before saving. Each mode's duration is persisted independently. Untouched timers update immediately; running/paused sessions preserve their deadline, remaining time and original completion credit. Reset or restarting a completed session uses the new duration. Completion totals now use the session duration rather than a hardcoded 25 minutes. Three instrumentation regressions authored but not run; source-only review, manual verification pending.

Projects/Goals follow-up (2026-09-08): card long-press opens Archive/Reactivate and confirmed Delete. Archived records are hidden by default and accessible with Show archived. Project deletion transactionally clears saved task project links; goal deletion clears project goal links. Child records and reminders are preserved; no schema change. Existing project status semantics stop new protocol generation when archived. Two Room instrumentation regressions authored, not run. Source review only; manual acceptance pending. Other UI requests remain deferred.

Idle-only update (2026-09-08): replaced the Idle route with a modal cover preserving the underlying screen/draft. A downward swipe starting in the app's top 88dp below the status bar opens it; upward swipe, exposed-area tap or Back dismisses it. Removed Today calendar and More idle buttons. Responsive portrait/landscape layout, exact past-day colors #002A32 light / #E8E8E8 dark, palette accent for today, accessibility open/dismiss actions. Source reviewed only; no builder/tests/device runs. Other newly requested UI changes are deferred in TASKS.md.

Latest UI revision (2026-09-08), source implemented, NOT built/tested/device-verified: equal-height Today metrics; dated Anytime defaults for new tasks and classification-clock refresh after successful writes; centered icon-only add controls; shared four-tab bottom dock; More via top-left menu; white active icon tint without a selection bubble; compact task form; native Material date/time dialogs across task, schedule and planning editors; bundled JetBrains Mono. Duplicate Projects/Goals switch removed, domain relationships retained. Manual checklist: `docs/UI_MANUAL_REVIEW.md`. This entry supersedes older UI/asset/deferred statements below.

Earlier approved batch is also implemented in source: first-run/replay welcome with supplied artwork; palette/text-size/reduced-motion persistence; supplied action icons; swipe-left completion, swipe-right timing options, two-second hold actions; project active/rest protocol editor; draggable schedule ruler. Original artwork and excluded mascot XML remain unchanged. No schema/dependency upgrade. All these edits remain unverified; historical passing results below do not cover them.

Asset integration (2026-09-08): converted supplied SVGs from `icon_files/` using the cached Android converter, without Gradle. Launcher/adaptive monochrome, notification silhouette, Android 12+ light/night splash and five coolicons navigation symbols are wired in source. More > Icon credits provides attribution. Mascot and wordmark variants are imported but not displayed; onboarding behavior is unchanged. Original assets and excluded `mascot_animation.xml` are untouched. App build/tests/device appearance remain unverified at the user's request. Details: `docs/ASSET_IMPORT.md`.

Current user-requested slice (2026-09-09): Focus auto-transition, decoupled selection, and Long Break configuration implemented and built. Build `app:assembleDebug` passed. Manual verification pending.

Current user-requested slice (2026-09-08): screen connections, planning editors, Focus and Idle are implemented in source. The user explicitly waived builder/build/test/emulator verification for this batch and will report issues manually. No Gradle, lint, tests, install or emulator commands were run for these edits. Read `docs/SCREEN_CONNECTION_AUDIT.md` for current connections and remaining limits; `docs/ONBOARDING_AND_ASSET_HANDOFF.md` records the onboarding recommendation and requested asset sizes.

Previous checkpoint only: 298 JVM tests and both APK builds passed before this screen-connection batch. Those results do not verify the new code. Five additional regression cases were authored for focus timing/streaks, generated-task completion, and deadline removal but were not run. Previous final lint and device appearance checks also remain unconfirmed.

Appearance: More > Appearance persists System/Light/Dark; MainActivity and ReminderActivity observe it. Material container/inverse/error roles, hero progress-label contrast, window backgrounds and system-bar contrast are connected. Night launch resources follow the device theme; manual overrides apply after DataStore loads. Glance retains its separate system-aware theme.

Implemented steps: (1) coordinator save/delete and transactional task/next-reminder persistence; (2) Today add/edit/complete/reopen/delete routes with project selection and saved drafts; (3) direct notification editor intents with stale-generation validation and singleTop reuse; (4) saved tab/back state and correct Goals routing; (5) project/goal editing, linking, task access and progress; (6) persistent Pomodoro/break timer with completed minutes and focus streak; (7) manually entered Idle calendar from Today/More; (8) foreground time and capability refresh; (9) fixed supplied warning-icon rendering. All are source-reviewed only, unbuilt and untested.

Remaining: onboarding artwork is imported; welcome behavior/design acceptance, remaining action icons, animated navigation, advanced palette/font controls, project-protocol editor and draggable schedule ruler remain deferred. Play Store artwork still needs a 512 px PNG export. Widget theme/projection/time-boundary gaps are unchanged. Focus has no background alarm or notification: an elapsed saved session is settled when the app runs again. Idle has no auto-start. No database schema change or dependency upgrade was made.

Phase 10A is complete. Phase 11 hardening is in progress: battery & performance profiling infrastructure set up (`:benchmark` module) and database migrations (v1-v8) verified via `MigrationTest`. Mascot animation implemented on the Welcome screen. Refined Idle Calendar with 3x larger month and 1.5x larger date labels. Fixed Focus page timer/color issues. Accessibility hardening is next.

## Approved batch: remaining Phases 8 through 10
- User approved all remaining tasks in Phases 8, 9, and 10 on 2026-09-07, waived verification runs for this batch, and requested the results report only after Phase 10. Phase 10A is outside this batch.
- Work proceeds across phase boundaries without additional approval. Each step below records implementation status, never an unrun verification result. Normal verification resumes after Phase 10.
- P8-04: refined to STRICT-only full-screen, unverified - real notification channels, permission/full-screen gates, opt-in alarm setting, alarm activity and receiver presentation.
- P8-05: implemented, unverified - generic private/public notifications, refined to lock-screen only privacy, and full action set (Complete/Snooze/Reschedule) in notifications.
- P8-06: implemented, unverified - transactional completion/snooze/reschedule invalidation, serialized coordinator, and receiver-side reschedule UI launch.
- P9-01: implemented, unverified - lifecycle reconciliation for boot, package-replaced, and time/timezone changes using ScheduleReconciler.
- P9-02: implemented, unverified - capability repair flow providing settings intents and reacting to exact alarm permission state changes.
- P9-03: implemented, unverified - alarm diagnostics screen showing capabilities and recent events, accessible via the "More" tab.
- P9-04: implemented, unverified - manual repair action in Diagnostics and proactive discovery banner on Today screen.
- P10-01: implemented, unverified - widget state projection using Jetpack Glance, TodayWidgetState, and reactive WidgetStateReconciler.
- P10-02: implemented, unverified - refined widget UI with Now/Next prominence, improved typography, and branding.
- P10-03: implemented, unverified - widget privacy mode and "Done" quick action for the "Now" task.
- P10A-02: implemented, unverified - Goal and Project domain models, Task provenance, and Room schema version 5 with MIGRATION_4_5.
- P10A-03: implemented, unverified - Task-to-Project linking in DAOs/Repositories and TaskProvenance mapping for generated tasks.
- P10A-06: implemented, unverified - dynamic task derivation from project protocols using ProjectOccurrenceGenerator and TodaySnapshotBuilder integration.

## Superseded Phase 8-only verification exception
- User instruction on 2026-09-07: skip verification for the tasks remaining in Phase 8 only (remaining P8-03 checks and P8-04 through P8-06).
- Continue Guided Build, scoped implementation, and handoff updates. Do not run further tests, builds, lint, or emulator verification for these slices; distinguish implemented work from verified work.
- Superseded by the approved batch above: verification is also waived for Phases 9 and 10. Do not describe these implementations as fully verified.

## Last verified checkpoint
- Phase 11 Accessibility & Visual Review (2026-09-08): All implementation slices from Phase 8 through 10A (including P10A-01 through P10A-10) verified via 296 JVM unit tests and a successful lint run. Accessibility semantics added to Today, Plans, and Diagnostics screens.
- P8-03: focused AlarmNotificationActionProcessorTest and PersistentReminderEscalationProcessorTest run passed before the waiver took effect. Full module tests, APK builds, lint, and device verification were not run for P8-03.
- P8-03 implemented rejection of snoozes for completed tasks and invalid snooze durations; added tests for default denial, explicit opt-in, duplicate/stale actions, rejected scheduling, and isolating the matching escalation chain.
- P8-02 (2026-09-07): wired `AlarmReceiver` accepted deliveries to `PersistentReminderEscalationProcessor`, with generation/status checks, persisted-chain continuation, terminal-state protection, re-alert caps, and exact/inexact result propagation.
- P8-02: `RoomPersistentReminderEscalationRepository.advanceAfterDelivery` checks the persisted inputs and atomically saves chain + successor using the existing v4 schema; failed platform scheduling leaves durable intent for later recovery.
- P8-02 focused JVM checks: 16 passed. `scripts/verify-local.ps1`: PASS, including all 272 JVM tests and both debug APKs. An initial Kotlin incremental-cache failure was cleared by a non-incremental rebuild; normal verification then passed.
- P8-02 device verification: all 4 `EscalationSchedulingPersistenceTest` checks passed on a temporary read-only Medium_Phone emulator (database reopen, transaction rollback, stale occurrence, competing resolution). The temporary emulator was closed afterward; the full connected UI suite was not run.
- P8-02 lint: FAIL with 8 existing `UnrememberedMutableState` errors in unrelated task-editor instrumentation tests and 15 warnings; no lint errors point to the P8-02 changed files. See the deferred verification repair in `TASKS.md`.
- Phase 1 local foundation baseline is complete: Kotlin + Compose + Material 3, dependency conventions, manual DI, injectable time, JVM/unit and instrumentation-test baselines, local/CI verification, and first Calm Pressure theme tokens.
- Phase 0 product and Android alarm/reminder capability decisions remain accepted.
- P2-01 added `TaskId`, `TaskStatus`, `TaskPriority`, and immutable `Task` under `com.dayloop.app.core.model`.
- P2-01 unit tests verify default active/normal task state, blank ID rejection, blank title rejection, generated ID uniqueness/non-blank values, idempotent completion, and priority ordering separate from task status/reminder strictness.
- P2-02 added sealed `TaskSchedule` variants for all-day date-only schedules, deadline latest-completion date-times, and validated time-block start/end periods.
- P2-02 linked `Task` to an optional immutable schedule with copy-based scheduling, without adding reminder, recurrence, persistence, Today UI, or alarm behaviour.
- P2-02 unit tests verify schedule kind separation, date-only all-day schedules, deadline date-time preservation, valid time blocks, equal/reversed time-block rejection, and schedule copy immutability on `Task`.
- P2-03 added `ReminderPolicy` and `ReminderMode` for Normal, Persistent, Strict, and Focus reminder contracts, with validation for re-alert configuration and focus/time-block compatibility.
- P2-03 linked `Task` to a default Normal reminder policy with copy-based policy changes, without adding AlarmManager, notification, recurrence, persistence, or UI behaviour.
- P2-03 unit tests verify default Normal policy, Persistent/Strict re-alert requirements, positive re-alert intervals, Normal/Focus re-alert rejection, Strict policy independence from task priority, Focus policy time-block validation, and completion preserving reminder policy.
- P2-04 added `TaskSchedule.Interval`, `TaskSchedule.CalendarRecurrence`, `LocalTimeRange`, `IntervalRule`, `IntervalAnchor`, `RecurrenceRule`, and `RecurrenceFrequency` as pure local-calendar domain models.
- P2-04 intentionally models recurrence/interval intent and validation only; next-occurrence calculation, exact alarm conversion, custom recurrence expansion, persistence, and UI remain later slices.
- P2-04 unit tests verify interval duration/window/day/date validation, interval schedule kind separation, recurrence frequency/date validation, weekly day requirements, and local start-date anchoring for daily/monthly/yearly recurrence.
- P2-05 added Room dependency setup using the Room Gradle plugin and Kotlin kapt, after KSP resolution was blocked by DNS for `repo.maven.apache.org`.
- P2-05 added `DayLoopDatabase`, `TaskEntity`, and `TaskDao` under `com.dayloop.app.core.database`, keeping Room annotations out of pure domain models.
- P2-05 added a compiled instrumentation `TaskDaoTest` for in-memory insert/read/list/update/delete coverage, plus exported schema `app/schemas/com.dayloop.app.core.database.DayLoopDatabase/1.json`.
- P2-06 updated `TaskDao` to expose suspend reads/writes plus `Flow` observers, and added direct `kotlinx-coroutines-core` dependency ownership in the version catalogue.
- P2-06 added `TaskRepository`, `RoomTaskRepository`, and entity/domain mappers that keep Room rows out of app-facing task logic.
- P2-06 unit tests verify repository save/get/delete behaviour, observed task streams, every schedule kind mapping, strict reminder policy mapping independent of priority, and invalid persisted schedule rows being rejected during domain reconstruction.
- P2-07 added Preferences DataStore dependency setup and `AppSettings`, `ThemeMode`, `SettingsRepository`, and `DataStoreSettingsRepository` under `com.dayloop.app.settings`.
- P2-07 intentionally stores only small app preferences in DataStore; task source-of-truth data remains in Room and default task reminder policy remains validated through the domain model.
- P2-07 unit tests verify settings defaults, persistence for each preference key, invalid stored enum fallback to safe defaults, and JVM DataStore file-backed behaviour without touching Room task data.
- P2-08 added `PersistenceValidationTest` under `com.dayloop.app.repository` for persisted mapper validation, date-boundary round-trips, reminder-policy reconstruction, and exported-schema regression coverage.
- P2-08 unit tests verify short-month monthly and leap-day yearly recurrence anchors round-trip without expansion/date arithmetic, invalid persisted rows are rejected for reversed time blocks, non-positive interval durations, missing eligible interval days, missing weekly recurrence days, incomplete strict re-alert configuration, and focus reminders without time-block schedules.
- P2-08 unit tests verify a strict deadline reminder round-trip at a late-day boundary and guard required Room task columns in the exported schema.
- Focused P2-08 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 58s, 29 actionable tasks (4 executed, 25 up-to-date).
- P2-08 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 48s, 79 actionable tasks (79 up-to-date).
- P3-01 added `TodaySnapshot`, `TodayItem`, and `TodaySection` under `com.dayloop.app.today` as a pure derived domain model, not a Compose/UI filter or persistence source.
- P3-01 unit tests verify empty local-day snapshots, Today display section order, unresolved/completed count derivation, completed tasks rejected from unresolved sections, active tasks rejected from completed, and duplicate task IDs rejected across sections.
- Focused P3-01 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodaySnapshotTest --no-daemon`: build successful in 1m 41s, 29 actionable tasks (11 executed, 18 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 39s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 46s, 79 actionable tasks (7 executed, 72 up-to-date).
- P3-02 added `TodayOverdueClassifier` under `com.dayloop.app.today` as a pure local-time classifier for active deadline tasks only.
- P3-02 intentionally leaves all-day rollover, time-block unfinished handling, interval missed occurrences, and recurrence missed-occurrence policies to later dedicated slices.
- P3-02 unit tests verify past active deadlines become overdue, deadlines exactly at `now` are not overdue until after due time, future deadlines and completed past deadlines are excluded, non-deadline timing models are excluded, and classifier output can populate `TodaySnapshot.overdue`.
- Focused P3-02 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodayOverdueClassifierTest --no-daemon`: build successful in 1m 27s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 3s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-02 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 54s, 79 actionable tasks (6 executed, 73 up-to-date).
- P3-03 added `TodayNowNextClassifier` and `TodayNowNextClassification` under `com.dayloop.app.today` for pure active-window and same-day upcoming schedule classification.
- P3-03 classifies active time blocks as Now using inclusive start and exclusive end semantics, including cross-midnight blocks when the current local time falls inside their stored start/end range.
- P3-03 classifies same-day future time blocks and deadlines at/after `now` as Next in scheduled-time order, while excluding completed tasks, past deadlines, past time blocks, future-day schedules, all-day tasks, intervals, recurrences, and unscheduled tasks.
- Focused P3-03 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodayNowNextClassifierTest --no-daemon`: build successful in 1m 18s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 38s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-03 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 1s, 79 actionable tasks (6 executed, 73 up-to-date).
- P3-04 added `TodayAnytimeClassifier` under `com.dayloop.app.today` for pure date-only Anytime Today classification.
- P3-04 classifies active `TaskSchedule.AllDay` tasks whose date equals the target local date as Anytime, preserving input order.
- P3-04 intentionally excludes completed all-day tasks, other-date all-day tasks until rollover policy exists, deadline/time-block work owned by earlier classifiers, interval/recurrence work owned by later occurrence policy, and unscheduled tasks until the model has explicit Today intent.
- Focused P3-04 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodayAnytimeClassifierTest --no-daemon`: build successful in 1m 12s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 38s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-04 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 47s, 79 actionable tasks (6 executed, 73 up-to-date).
- P3-05 extended `TodaySnapshot` with `TodayProgress`, `TodayDayStatus`, completed count, remaining count, completion ratio, and derived OPEN/COMPLETE status.
- P3-05 added `TodayCompletedClassifier` for completed all-day, deadline, and time-block tasks whose stored schedule belongs to the target local date.
- P3-05 intentionally keeps empty days OPEN for now and excludes completed unscheduled, interval, and recurrence tasks until later model/policy slices can prove Today membership.
- P3-05 unit tests verify completed-only snapshots become COMPLETE, unresolved work reopens the day, progress count validation, completed all-day/deadline/time-block classification, cross-midnight completed time blocks, and completed classifier output feeding snapshot progress.
- Focused P3-05 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodaySnapshotTest --tests com.dayloop.app.today.TodayCompletedClassifierTest --no-daemon`: build successful in 1m 24s, 29 actionable tasks (13 executed, 16 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 29s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-05 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 41s, 79 actionable tasks (7 executed, 72 up-to-date).
- P3-06 added `TodayMoment` for deterministic conversion from `Instant` plus `ZoneId` into local Today date/time before classifier logic runs.
- P3-06 boundary tests verify the same instant can belong to different Today dates by zone, all-day classification follows the target local date, deadlines around midnight remain deterministic, midnight deadlines are Next not overdue, time-block end is exclusive at midnight, cross-midnight time blocks are active just after midnight, and empty boundary snapshots remain OPEN with zero progress.
- Focused P3-06 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.today.TodayBoundaryTest --no-daemon`: build successful in 3m 14s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 7s, 29 actionable tasks (1 executed, 28 up-to-date).
- P3-06 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 39s, 79 actionable tasks (6 executed, 73 up-to-date).
- P4-01 added `TodayUiState` as the hoisted render state for the Today screen, deriving title, stable date label, empty flag, and snapshot counts from `TodaySnapshot`.
- P4-01 added `TodayRoute` and `TodayScreen`, making `DayLoopApp` land on the Today scaffold while keeping repository/database ownership out of Compose.
- P4-01 updated the Activity instrumentation smoke test to expect the Today scaffold landing text and added a JVM `TodayUiStateTest` for the empty and derived-count states.
- Focused P4-01 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 1m 6s, 29 actionable tasks (10 executed, 19 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 37s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m, 79 actionable tasks (10 executed, 69 up-to-date).
- P4-02 extended `TodayUiState` with total count, progress fraction, day-status label, progress summary, remaining label, and a single accessibility progress summary derived from `TodaySnapshot.progress`.
- P4-02 replaced the placeholder empty-state card with a Material 3 Today progress/day-state header that uses Calm Pressure spacing/colors and exposes matching semantic progress state.
- P4-02 updated `TodayUiStateTest` for empty, in-progress, and complete day header states, and updated the Activity smoke test expectations for the header text.
- Focused P4-02 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 2m 55s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 38s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-02 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 7s, 79 actionable tasks (9 executed, 70 up-to-date).
- P4-03 extended `TodayUiState` with `TodaySectionUiState`, `TodayTaskUiState`, all unresolved Today sections, and non-empty visible sections while leaving the completed section to a later slice.
- P4-03 updated `TodayScreen` to render a scrollable Today surface with title, progress header, named section headings, section counts, and passive task rows for Overdue, Now, Next, and Anytime.
- P4-03 updated `TodayUiStateTest` to verify unresolved section order, title mapping, task title mapping, count labels, hidden empty sections, and section accessibility labels.
- Focused P4-03 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 4m 23s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 2m 54s, 29 actionable tasks (5 executed, 24 up-to-date).
- P4-03 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 58s, 79 actionable tasks (6 executed, 73 up-to-date).
- P4-04 extended `TodayTaskUiState` with an accessible completion action label derived from the task title.
- P4-04 threaded `onCompleteTask(TaskId)` from `TodayRoute`/`TodayScreen` into each unresolved task row and added an unchecked Material checkbox that emits only when checked.
- P4-04 added `TodayScreenTest` to verify tapping a task completion checkbox emits the matching `TaskId`; this compiles into the androidTest APK and can run when an emulator is connected.
- Focused P4-04 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 1m 38s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 38s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-04 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 16s after switching the new Compose test to the v2 rule API, 79 actionable tasks (5 executed, 74 up-to-date).
- P4-05 extended `TodayUiState` with a separately derived completed section, completed task rows, completed labels, and a `hasCompletedSection` visibility flag.
- P4-05 updated `TodayScreen` to render a collapsed Completed section header/count by default and reveal disabled checked completed rows only after the user expands it.
- P4-05 updated `TodayUiStateTest` for completed-section derivation and `TodayScreenTest` for expanding the completed disclosure.
- Focused P4-05 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 1m 19s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 52s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-05 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 57s after removing an unsupported Compose test absence assertion, 79 actionable tasks (6 executed, 73 up-to-date).
- P4-06 refined `TodayUiState` by removing `TodayExperienceState`, empty-day and day-complete messages, as the hero card provides sufficient information.
- P4-06 updated `TodayScreen` to render the empty-day message (`Nothing planned`) and completed-day message (`All done today`) while leaving in-progress Today lists focused on unresolved sections.
- P4-06 updated `TodayUiStateTest`, `TodayScreenTest`, and `MainActivityTest` to cover empty and completed-day presentation states.
- Focused P4-06 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 1m 9s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 31s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-06 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 56s, 79 actionable tasks (9 executed, 70 up-to-date).
- P4-07 extended completed-section UI state with explicit expand/collapse action labels for assistive technology.
- P4-07 updated `TodayScreen` so progress headers, section headers, active task rows, and completed task rows keep text inside weighted content space for large-font wrapping.
- P4-07 added semantic state descriptions to the Completed disclosure button and updated Today tests for the accessibility labels.
- Focused P4-07 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.today.TodayUiStateTest --no-daemon`: build successful in 1m 19s, 29 actionable tasks (5 executed, 24 up-to-date).
- P4-07 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 32s, 54 actionable tasks (54 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 32s, 29 actionable tasks (1 executed, 28 up-to-date).
- P4-07 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 41s, 79 actionable tasks (3 executed, 76 up-to-date).
- P5-01 added `QuickAddDraft` and `QuickAddSubmitResult` under `feature/taskeditor` to trim titles, reject blank submissions, and create plain active domain tasks.
- P5-01 added `QuickAddBar`, a hoisted Compose quick-add row with text entry, validation feedback, and an Add action that emits created tasks upward.
- P5-01 added JVM draft tests and Compose Android tests for blank-title validation, valid task emission, and clearing the draft after submit.
- Focused P5-01 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.taskeditor.QuickAddDraftTest --no-daemon`: build successful in 1m 48s, 29 actionable tasks (11 executed, 18 up-to-date).
- P5-01 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 54s, 54 actionable tasks (8 executed, 46 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 35s, 29 actionable tasks (1 executed, 28 up-to-date).
- P5-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 36s, 79 actionable tasks (4 executed, 75 up-to-date).
- P5-02 added `TaskEditorDraft` and `TaskEditorSubmitResult` for base editor state, title validation, new-task creation, and existing-task updates.
- P5-02 added `TaskEditorScreen`, a hoisted Compose form for task title, priority selection, Save, and Cancel without schedule/reminder editing.
- P5-02 added JVM draft tests and Compose Android tests for blank-title validation, selected-priority save, and preserving existing task schedule/reminder fields during edit round-trip.
- Focused P5-02 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.taskeditor.TaskEditorDraftTest --no-daemon`: build successful in 2m 21s, 29 actionable tasks (10 executed, 19 up-to-date).
- P5-02 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 43s, 54 actionable tasks (6 executed, 48 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 51s, 29 actionable tasks (1 executed, 28 up-to-date).
- P5-02 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 57s, 79 actionable tasks (4 executed, 75 up-to-date).
- P5-05 added optional interval editor draft fields for start date, every-minutes duration, and local active-window start/end times, mapping valid input into `TaskSchedule.Interval` without alarm scheduling.
- P5-05 keeps interval input mutually exclusive with deadline/time-block input and preserves existing interval eligible days, end date, and anchor when editing an interval task.
- P5-05 added Compose interval controls to `TaskEditorScreen` and JVM/Android-test coverage for valid interval save, validation errors, mutual exclusivity, and interval round-trip editing.
- Focused P5-05 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.taskeditor.TaskEditorDraftTest --no-daemon`: build successful in 1m 18s, 29 actionable tasks (9 executed, 20 up-to-date).
- P5-05 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 52s, 54 actionable tasks (6 executed, 48 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 32s, 29 actionable tasks (1 executed, 28 up-to-date).
- P5-05 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 34s, 79 actionable tasks (3 executed, 76 up-to-date).
- P5-06 added optional recurrence editor draft fields for frequency, repeat interval, start date, optional end date, and weekly day selection, mapping valid input into `TaskSchedule.CalendarRecurrence` without occurrence expansion or alarm scheduling.
- P5-06 keeps recurrence input mutually exclusive with deadline/time-block/interval input and validates missing, malformed, non-positive, reversed-date, and weekly-without-days states before save.
- P5-06 added Compose recurrence controls to `TaskEditorScreen` and JVM/Android-test coverage for valid daily and weekly recurrence saves, validation errors, mutual exclusivity, and recurrence round-trip editing.
- Focused P5-06 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.taskeditor.TaskEditorDraftTest --no-daemon`: build successful in 1m 54s, 29 actionable tasks (9 executed, 20 up-to-date).
- P5-06 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 55s, 54 actionable tasks (6 executed, 48 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 31s, 29 actionable tasks (1 executed, 28 up-to-date).
- P5-06 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 37s, 79 actionable tasks (3 executed, 76 up-to-date).
- P5-07 added reminder policy editor draft fields for mode, re-alert interval/count, and strict snooze/reschedule controls, mapping valid input into `ReminderPolicy` without notification or alarm delivery.
- P5-07 validates Persistent/Strict re-alert requirements and Focus-with-time-block compatibility before saving a task.
- P5-07 added Compose reminder policy controls to `TaskEditorScreen` and JVM/Android-test coverage for valid policies, validation errors, focus compatibility, and reminder round-trip editing.
- Focused P5-07 unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.feature.taskeditor.TaskEditorDraftTest --no-daemon`: build successful in 2m 14s, 29 actionable tasks (9 executed, 20 up-to-date).
- P5-07 Android test APK verification passed with `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 15s, 54 actionable tasks (6 executed, 48 up-to-date).
- Full debug unit verification passed with `.\gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 57s, 29 actionable tasks (1 executed, 28 up-to-date).
- P5-07 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File .\scripts\verify-local.ps1`: build successful in 1m 13s, 79 actionable tasks (3 executed, 76 up-to-date).
- P6-01 added `ScheduledOccurrence`, `ScheduledOccurrenceId`, and `ScheduledOccurrenceStatus` under `core.alarms` as pure immutable alarm-intent data with task ownership, fire instant, generation, status, and created/updated timestamps.
- P6-01 added Room `ScheduledOccurrenceEntity`, `ScheduledOccurrenceDao`, database schema version 2, exported schema `app/schemas/com.dayloop.app.core.database.DayLoopDatabase/2.json`, and `MIGRATION_1_2` for the `scheduled_occurrences` table and indexes.
- P6-01 added `ScheduledOccurrenceRepository`, `RoomScheduledOccurrenceRepository`, and occurrence entity/domain mappers for save/get/observe/delete operations without scheduling Android alarms.
- Focused P6-01 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.ScheduledOccurrenceTest --tests com.dayloop.app.repository.RoomScheduledOccurrenceRepositoryTest --tests com.dayloop.app.repository.PersistenceValidationTest --no-daemon`: build successful in 1h 44m 44s, 30 actionable tasks (17 executed, 13 up-to-date).
- P6-01 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 43s, 54 actionable tasks (13 executed, 41 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 40s, 29 actionable tasks (5 executed, 24 up-to-date).
- P6-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 42s, 79 actionable tasks (5 executed, 74 up-to-date).
- P6-02 added `NextOccurrenceCalculator` under `core.alarms` as a pure Android-free calculator from active persisted `Task` schedule data into `ScheduledOccurrence` records.
- P6-02 supports future/equal-time deadline occurrences and time-block start occurrences using an explicit `ZoneId`, deterministic occurrence IDs for tests, and supplied generation numbers for later stale-event protection.
- P6-02 intentionally returns no occurrence for completed, unscheduled, all-day, interval, recurrence, and already-past schedule states until later policy/engine slices define those behaviours.
- Focused P6-02 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.NextOccurrenceCalculatorTest --no-daemon`: build successful in 1m 33s, 29 actionable tasks (10 executed, 19 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 50s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-02 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 43s, 54 actionable tasks (3 executed, 51 up-to-date).
- P6-02 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 41s, 79 actionable tasks (4 executed, 75 up-to-date).
- P6-03 verified the existing `AlarmScheduler` abstraction under `core.alarms`, with schedule/cancel methods over `ScheduledOccurrence` and typed result models for success, rejection, cancellation, not-found, and failure states.
- P6-03 keeps the scheduler API Android-free so future `AlarmManager` and `PendingIntent` wiring can sit behind the interface without leaking framework types into pure alarm-domain callers.
- P6-03 tests verify result identity, exactness reporting, capability rejection, resolved-occurrence rejection, cancellation identity, and fake scheduler implementation without Android framework types.
- Focused P6-03 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AlarmSchedulerTest --no-daemon`: build successful in 41s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-03 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 1m 31s, 79 actionable tasks (1 executed, 78 up-to-date).
- P6-04 added `AndroidAlarmScheduler`, an Android-backed adapter behind the existing `AlarmScheduler` interface, plus a small `ExactAlarmPlatform` test boundary for deterministic JVM coverage.
- P6-04 declares `android.permission.SCHEDULE_EXACT_ALARM` in the manifest and gates scheduling with `AlarmManager.canScheduleExactAlarms()` on Android S and newer before calling `setExactAndAllowWhileIdle`.
- P6-04 builds immutable broadcast `PendingIntent`s with a stable occurrence data URI, occurrence id extra, and generation extra so P6-05 can add receiver-side stale-event reconciliation without changing schedule identity.
- P6-04 cancel support looks up the existing occurrence pending intent with `FLAG_NO_CREATE`, cancels it through `AlarmManager`, cancels the pending intent token, and reports Cancelled/NotFound/Failed through the existing typed result model.
- Focused P6-04 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AndroidAlarmSchedulerTest --no-daemon`: build successful in 1m 51s, 29 actionable tasks (13 executed, 16 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 6s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-04 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 11s, 54 actionable tasks (3 executed, 51 up-to-date).
- P6-04 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 1m 27s, 79 actionable tasks (3 executed, 76 up-to-date).
- P6-05 added `AlarmDeliveryProcessor` to reload a delivered occurrence from `ScheduledOccurrenceRepository`, reject missing/replaced/resolved occurrences as stale, and persist a matching scheduled occurrence as fired using `TimeProvider`.
- P6-05 added `AlarmReceiver`, registered it as a non-exported receiver for DayLoop alarm intents, and kept receiver work asynchronous with `goAsync()` while delegating source-of-truth checks to the processor.
- P6-05 exposed `scheduledOccurrenceRepository` through `AppGraph` and added minimal Room-backed repository construction in `DefaultAppGraph` so receiver startup after process death can reach persisted occurrence state.
- P6-05 focused tests verify accepted delivery marks an occurrence fired, missing occurrences are stale, older delivered generations are stale, cancelled occurrences are stale, and invalid delivered generations are rejected.
- Focused P6-05 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AlarmDeliveryProcessorTest --no-daemon`: build successful in 2m 15s, 29 actionable tasks (13 executed, 16 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 31s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-05 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 30s, 54 actionable tasks (3 executed, 51 up-to-date).
- P6-05 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 1m 19s, 79 actionable tasks (3 executed, 76 up-to-date).
- P6-06 added `AlarmNotificationActionProcessor` for Complete, Snooze, and Reschedule commands behind notification action taps without adding notification rendering UI.
- P6-06 routes notification action broadcasts through `AlarmReceiver` using the same occurrence id/generation extras as alarm delivery so stale-event reconciliation remains the first gate.
- P6-06 complete action reloads the task and occurrence, marks active tasks completed, marks the matching occurrence fired, and cancels the occurrence alarm token idempotently.
- P6-06 snooze action creates a later same-id occurrence with `generation + 1`, schedules it through `AlarmScheduler`, and persists the replacement only after scheduling succeeds.
- P6-06 reschedule action currently returns a deterministic `RescheduleRequested` result without mutating state because the reschedule UI/action target does not exist yet.
- P6-06 exposed `taskRepository` and `alarmScheduler` through `AppGraph`, wired `RoomTaskRepository` and `AndroidAlarmScheduler` in `DefaultAppGraph`, and added notification action intent filters to the non-exported `AlarmReceiver`.
- Focused P6-06 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AlarmNotificationActionProcessorTest --no-daemon`: build successful in 2m 30s, 29 actionable tasks (13 executed, 16 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 52s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-06 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 59s, 54 actionable tasks (3 executed, 51 up-to-date).
- P6-06 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 57s, 79 actionable tasks (3 executed, 76 up-to-date).
- P6-07 updated `AndroidAlarmScheduler` so missing exact-alarm capability no longer rejects scheduling outright; it schedules an inexact idle-allowed fallback alarm using the same occurrence `PendingIntent` identity.
- P6-07 keeps exact scheduling on `setExactAndAllowWhileIdle` when available and uses `setAndAllowWhileIdle` as the degraded fallback path when `canScheduleExactAlarms()` is false.
- P6-07 reports fallback precision through the existing typed result model as `AlarmScheduleResult.Scheduled(exact = false)`, leaving user-facing repair UX to later work.
- P6-07 focused tests verify exact scheduling, inexact fallback scheduling, fallback trigger time, stable occurrence identity, and schedule-failure handling for the fallback path.
- Focused P6-07 unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AndroidAlarmSchedulerTest --no-daemon`: build successful in 2m 19s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 1m 20s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-07 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 20s, 54 actionable tasks (3 executed, 51 up-to-date).
- P6-07 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 47s, 79 actionable tasks (3 executed, 76 up-to-date).
- P6-08 added `AlarmDiagnosticEntry`, `AlarmDiagnosticEvent`, `AlarmDiagnosticsRepository`, Room entity/DAO/repository wiring, exported schema v3, and `MIGRATION_2_3` for append-only alarm evidence.
- P6-08 records diagnostics from `AndroidAlarmScheduler` for exact schedules, inexact fallback schedules, rejected schedules, cancelled/not-found/failed cancels, and from alarm processors for accepted/stale delivery plus complete/snooze/reschedule notification actions.
- P6-08 keeps diagnostics no-fail and non-canonical: diagnostics writes are best-effort evidence only, while task and scheduled-occurrence state still decide alarm behavior.
- P6-08 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.AndroidAlarmSchedulerTest --tests com.dayloop.app.core.alarms.AlarmDeliveryProcessorTest --tests com.dayloop.app.repository.PersistenceValidationTest --no-daemon`: build successful in 2m 2s, 29 actionable tasks (10 executed, 19 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 45s, 29 actionable tasks (1 executed, 28 up-to-date).
- P6-08 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 15s, 54 actionable tasks (7 executed, 47 up-to-date).
- P6-08 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 1m 16s, 79 actionable tasks (4 executed, 75 up-to-date).
- P7-01 added `IntervalNextEventCalculator` under `core.recurrence` for deterministic clock-anchored interval slot calculation without background polling.
- P7-01 wires `TaskSchedule.Interval` into `NextOccurrenceCalculator`, converting the next local interval slot to an alarm `Instant` through the caller-supplied `ZoneId`.
- P7-01 handles first window starts, exact slot matches, next same-day interval slots, skipped ineligible days, end-date exhaustion, and window-close rollover to the next eligible day; `IntervalAnchor.COMPLETION` intentionally returns no occurrence until acknowledgement/completion state exists.
- P7-01 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.IntervalNextEventCalculatorTest --tests com.dayloop.app.core.alarms.NextOccurrenceCalculatorTest --no-daemon`: build successful in 1m 5s, 29 actionable tasks (11 executed, 18 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 30s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-01 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 41s, 54 actionable tasks (3 executed, 51 up-to-date).
- P7-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 44s, 79 actionable tasks (4 executed, 75 up-to-date).
- P7-02 added `IntervalActiveWindowCalculator`, `IntervalActiveWindow`, `IntervalActiveWindowStatus`, and `IntervalActiveWindowPosition` under `core.recurrence` so active-window decisions are explicit instead of hidden inside slot stepping.
- P7-02 defines interval window start as inclusive and window end as exclusive, reports ineligible days and out-of-range dates distinctly, and returns the next eligible window when one exists.
- P7-02 updated `IntervalNextEventCalculator` to depend on the active-window policy, including rollover when no clock-anchored slot remains before the current window closes.
- P7-02 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.IntervalActiveWindowCalculatorTest --tests com.dayloop.app.core.recurrence.IntervalNextEventCalculatorTest --tests com.dayloop.app.core.alarms.NextOccurrenceCalculatorTest --no-daemon`: build successful in 1m 5s, 29 actionable tasks (10 executed, 19 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 44s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-02 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 40s, 54 actionable tasks (3 executed, 51 up-to-date).
- P7-02 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 45s, 79 actionable tasks (4 executed, 75 up-to-date).
- P7-03 added `RecurrenceNextDateCalculator` under `core.recurrence` for deterministic date-only recurrence expansion without inventing alarm time-of-day behavior.
- P7-03 supports daily recurrence, every-N-day cadence, weekly recurrence, selected weekdays, every-N-week anchoring from the start week, and end-date exhaustion.
- P7-03 intentionally left monthly/yearly recurrence as `null` until P7-04 defined short-month and leap-date rules.
- P7-03 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.RecurrenceNextDateCalculatorTest --no-daemon`: build successful in 56s, 29 actionable tasks (10 executed, 19 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 29s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-03 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 32s, 54 actionable tasks (3 executed, 51 up-to-date).
- P7-03 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 39s, 79 actionable tasks (4 executed, 75 up-to-date).
- P7-04 extended `RecurrenceNextDateCalculator` with monthly and yearly date expansion using the original start-date anchor and clamping missing days to the month/year's last valid day.
- P7-04 verifies Jan 31 monthly recurrence clamps to Feb 28, returns to Mar 31, respects every-N-month cadence, and applies end-date cutoffs after clamping.
- P7-04 verifies Feb 29 yearly recurrence clamps to Feb 28 in non-leap years, returns to Feb 29 in leap years, respects every-N-year cadence, and advances after a clamped date passes.
- P7-04 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.RecurrenceNextDateCalculatorTest --no-daemon`: build successful in 1m 7s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 30s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-04 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 33s, 54 actionable tasks (3 executed, 51 up-to-date).
- P7-04 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 32s, 79 actionable tasks (3 executed, 76 up-to-date).
- P7-05 added `RecurrencePreviewCalculator` under `core.recurrence` as a pure bounded projection of upcoming recurrence dates, reusing the verified next-date engine without scheduling alarms or mutating task state.
- P7-05 preview tests verify on-cadence inclusion, start-date clamping before a series begins, end-date stopping, weekly selected-day ordering, monthly short-month previews, yearly leap-date previews, and non-positive count rejection.
- P7-05 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.RecurrencePreviewCalculatorTest --tests com.dayloop.app.core.recurrence.RecurrenceNextDateCalculatorTest --no-daemon`: build successful in 1m 38s, 29 actionable tasks (9 executed, 20 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 31s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-05 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 38s, 54 actionable tasks (3 executed, 51 up-to-date).
- P7-05 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 41s, 79 actionable tasks (3 executed, 76 up-to-date).
- P7-06 added `RecurrenceIntervalEdgeCaseMatrixTest` to group recurrence, interval active-window, interval next-event, scheduler DST conversion, and recurrence non-scheduling boundaries into one deterministic matrix.
- P7-06 matrix coverage verifies daily end-date inclusion, every-other-week selected days, monthly 31st clamping/restoration, yearly leap-day clamping/restoration, interval boundary positions, final-window exhaustion, DST spring-forward conversion, and recurrence tasks remaining unscheduled by `NextOccurrenceCalculator`.
- P7-06 focused unit verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.recurrence.RecurrenceIntervalEdgeCaseMatrixTest --no-daemon`: build successful in 38s, 29 actionable tasks (4 executed, 25 up-to-date).
- Full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 30s, 29 actionable tasks (1 executed, 28 up-to-date).
- P7-06 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 23s, 54 actionable tasks (54 up-to-date).
- P7-06 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 27s, 79 actionable tasks (79 up-to-date).
- P8-01 added `PersistentReminderEscalation`, `PersistentReminderEscalationId`, and `PersistentReminderEscalationStatus` under `core.alarms` for deterministic one-shot re-alert chains that advance, cap, resolve, or cancel without Android framework dependencies.
- P8-01 added `persistent_reminder_escalations` Room persistence, DAO, repository boundary/wiring, exported schema v4, and `MIGRATION_3_4` so active re-alert chains can survive process death.
- P8-01 introduced persistence only; P8-02 subsequently connected that state to one-shot scheduling.
- P8-01 focused JVM verification passed with `./gradlew.bat :app:testDebugUnitTest --tests com.dayloop.app.core.alarms.PersistentReminderEscalationTest --tests com.dayloop.app.repository.PersistenceValidationTest --no-daemon`: build successful in 1m 34s, 30 actionable tasks (16 executed, 14 up-to-date).
- P8-01 full debug unit verification passed with `./gradlew.bat :app:testDebugUnitTest --no-daemon`: build successful in 56s, 29 actionable tasks (9 executed, 20 up-to-date).
- P8-01 Android test APK verification passed with `./gradlew.bat :app:assembleDebugAndroidTest --no-daemon`: build successful in 1m 41s, 54 actionable tasks (9 executed, 45 up-to-date).
- P8-01 local CI-equivalent verification passed with `powershell -ExecutionPolicy Bypass -File ./scripts/verify-local.ps1`: build successful in 47s, 79 actionable tasks (5 executed, 74 up-to-date).
- Debug APK artifact exists at `app/build/outputs/apk/debug/app-debug.apk`; instrumentation test APK exists at `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Current accepted direction
- Android first; Android Studio; Kotlin; Jetpack Compose.
- Material 3 / Material 3 Expressive foundation with custom **Calm Pressure** visual language.
- Room + DataStore; deterministic offline core.
- Preferences DataStore is configured with version 1.2.1 for small app settings only; Room remains the source of truth for tasks.
- Room is configured with version 2.8.4, the Room Gradle plugin, Kotlin kapt annotation processing, coroutine/Flow DAO access, and exported schemas under `app/schemas`.
- `DayLoopDatabase` is schema version 8 with `scheduled_occurrences`, `alarm_diagnostic_entries`, `persistent_reminder_escalations`, `goals`, and `projects` Room tables plus explicit migrations through `MIGRATION_7_8`.
- `NextOccurrenceCalculator` currently derives schedulable occurrences for active deadline, time-block, and clock-anchored interval schedules, using explicit zone conversion and explicit interval active-window handling; it returns null for all-day, recurrence, completed, unscheduled, past, or completion-anchored interval schedules.
- `AlarmScheduler` is currently an Android-free boundary with typed schedule/cancel result models; production scheduling, receiver delivery, notification actions, exact fallback, and diagnostics now sit behind that boundary.
- `AndroidAlarmScheduler` currently schedules exact RTC wakeup alarms for scheduled occurrences when exact capability is available, falls back to inexact idle-allowed RTC wakeup alarms when exact capability is missing, uses occurrence-id-based immutable broadcast `PendingIntent` identity for delivery reconciliation, and records best-effort alarm diagnostics.
- `AlarmReceiver` performs source-of-truth stale-event reconciliation, advances accepted deliveries through the persisted one-shot escalation processor, and routes notification commands through the occurrence id/generation gate.
- `AlarmNotificationActionProcessor` enforces snooze permission and active-task status, validates positive snooze duration, and limits optional postponement to the matching occurrence's chain.
- AlarmManager for justified exact user-facing alarms; WorkManager for deferrable work.
- AI never controls canonical alarm delivery directly.
- Today remains the default execution surface; Projects/Goals remain secondary planning context on the Plans screen.
- MVP scope and explicit non-goals are frozen in `PRODUCT_CONTRACT.md`.
- Task timing models remain distinct: all-day, deadline, time-block, interval, and recurrence.
- Task schedule hierarchy currently models all-day, deadline, time-block, interval, and calendar recurrence; daily/weekly/monthly/yearly recurrence next-date expansion, bounded recurrence preview, and deterministic edge-case matrix coverage exist, while advanced custom recurrence remains a later dedicated slice.
- Reminder contracts remain separate from timing type and priority.
- Reminder policy models Normal, Persistent, Strict, and Focus contracts as validated data; P8-01/P8-02 provide durable re-alert state and one-shot scheduling, and P8-03 implements snooze constraints. Notification UX, privacy presentation, alarm-grade surfaces, and full action-chain coordination remain later slices.
- MVP Android version policy: minSdk 26, targetSdk 36, compileSdk 36 initially, and no maxSdk.
- Exact alarm scheduling must use `SCHEDULE_EXACT_ALARM` direction for MVP, gate on `canScheduleExactAlarms()`, persist degraded state, and provide fallback.
- Do not declare `USE_EXACT_ALARM` for MVP unless a future ADR changes product positioning and policy eligibility.
- Alarm-style UI must gate on notification permission, channel state, and `canUseFullScreenIntent()`; full-screen denial falls back to high-importance alarm-category notification and repair UX.
- Reboot recovery must persist scheduled occurrence state, enable a system-deliverable boot receiver only while active schedules exist, reload/reject/reschedule after `BOOT_COMPLETED`, and reuse exact/fallback alarm gating.
- Interval reminders must persist rule plus current occurrence, schedule only the next one-shot alarm, advance only after valid delivery/resolution, and stop cleanly on completion/cancel/policy cap.
- `applicationId` and namespace are currently `com.dayloop.app` as a provisional foundation value until final product naming is decided.
- Gradle wrapper is pinned to 8.14.5 for Java 24 compatibility; Android Gradle Plugin is 8.13.2; Kotlin is 2.3.21; Compose UI/Foundation is 1.11.1 to remain compatible with compileSdk 36.
- Theme ownership lives in `app/src/main/java/com/dayloop/app/ui/theme/`; screens should consume `DayLoopTheme`, Material semantic tokens, and `MaterialTheme.dayLoopTokens` instead of hard-coded visual constants.
- Dependency ownership lives in `gradle/libs.versions.toml`; modules should use catalog aliases or bundles rather than raw dependency coordinates.
- App-wide dependency ownership starts in `DayLoopApplication.appGraph`; Compose code can access the graph through `LocalAppGraph` when a later slice wires real repository/database dependencies.
- Time access must go through `TimeProvider`; production uses `SystemTimeProvider`, and tests should use `FakeTimeProvider` instead of reading the wall clock directly.
- Fast deterministic checks belong in `app/src/test`; Android/Compose platform checks belong in `app/src/androidTest` and require an emulator/device for execution.
- CI should run the same foundation Gradle checks as `scripts/verify-local.ps1`: `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`.
- Core domain models must stay free of Android framework types; use immutable values, explicit validation, typed IDs, and fixed enums/sealed types for constrained concepts.
- Task priority is not reminder strictness; reminder policy remains a later model.
- Task editor deadline input currently accepts explicit local date/time strings (`YYYY-MM-DD` and `HH:mm`) and maps them to `TaskSchedule.Deadline` without alarm scheduling, timezone conversion, or persistence wiring.
- Task editor time-block input currently accepts explicit local start/end date/time strings, maps valid ranges to `TaskSchedule.TimeBlock`, and rejects equal or reversed ranges before save.
- Task editor interval input currently accepts explicit local start date (`YYYY-MM-DD`), every-minutes duration, and active-window times (`HH:mm`), maps valid input to `TaskSchedule.Interval`, and P7-02 can classify the active window before deriving the next clock-anchored one-shot occurrence.
- Task editor recurrence input currently accepts frequency, positive repeat interval, explicit local start date (`YYYY-MM-DD`), optional end date, and weekly weekdays when needed, mapping valid input to `TaskSchedule.CalendarRecurrence`; P7-05 can expand and preview daily/weekly/monthly/yearly rule dates only.
- DayLoop UI implementation should follow `docs/DAYLOOP_VISUAL_CONTRACT.md`: matcha/mint surfaces, near-black action cards/bars, lime accents, pill controls, animated-ready bottom navigation, configurable palette/font direction, and provided-SVG-friendly component slots.
- Future feature screens should prefer the shared `com.dayloop.app.ui.components` foundation before inventing new screen-local cards, chips, progress, warning, or bottom navigation styles.

## Decisions still required
- Product name/final application ID.
- Final acknowledgement/completion/rollover semantics for MVP reminder contracts.
- Final interval anchor semantics, recurrence edge cases, and escalation caps in later phases.
- Whether the current `Task` model should gain creation/update timestamps when persistence is introduced.

## Known failures/blockers
- No connected emulator/device was available during P1-06/P1-08/P2-01/P2-02/P2-03/P2-04/P2-05/P2-06/P2-07/P2-08/P3-01/P3-02/P3-03/P3-04/P3-05/P3-06/P4-01/P4-02/P4-03/P4-04/P4-05/P4-06/P4-07/P5-01/P5-02/P5-03/P5-04/P5-UI-01/P5-05/P5-06/P5-07/P6-01/P6-02/P6-03/P6-04/P6-05/P6-06/P6-07/P6-08/P7-01/P7-02/P7-03/P7-04/P7-05/P7-06/P8-01, so `connectedDebugAndroidTest` was not executed. The instrumentation test source set compiles and packages successfully into the androidTest APK; run the connected test once an Android Studio emulator is running. The GitHub Actions workflow has been added and locally mirrored by `scripts/verify-local.ps1`, but it has not run on GitHub until the repository is pushed to a GitHub remote with Actions enabled. The debug build may emit benign symbol-strip warnings for `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so`; kapt test tasks may emit non-blocking Room option warnings; DataStore protobuf may emit a Java 24 `sun.misc.Unsafe` deprecation warning during JVM tests. None blocks packaging.

## Files currently in play
- `STATE.md`
- `TASKS.md`
- `LEARNED_CONCEPTS.md`
- `phases/11-quality.md`
- `contexts/phase-11.context.md`
- `benchmark/build.gradle.kts`
- `benchmark/src/main/java/com/dayloop/benchmark/StartupBenchmark.kt`

## Next single action
- Await manual acceptance of Focus duration editing using `docs/UI_MANUAL_REVIEW.md`. Other UI slices and build/test/lint/install/emulator execution require a new request.

## Do not start yet
Do not start mascot animation, background auto-start, AI planner, cloud backend/authentication, calendar write sync, or cross-platform work without the corresponding user request.
