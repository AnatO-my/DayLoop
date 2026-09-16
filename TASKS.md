# DayLoop Task Backlog

Only check a task after its acceptance criteria/tests are satisfied.

## Idle Calendar slice and deferred UI requests (2026-09-08)
- [x] Manually accept Idle cover gestures, preserved underlying screen, portrait/landscape sizing, past-day theme colors and removal of both buttons. Implemented in source; fixed month clipping.
- [ ] Projects/Goals long-press delete/archive: implemented with confirmation, Show archived/Reactivate, and transactional unlinking. Manual acceptance and two new Room instrumentation tests pending; execution waived.
- [ ] Deferred: More as a left-swipe side panel, advanced sections, font-weight adjustment and custom HEX colors.
- [ ] Deferred: remove welcome replay from More and confirm startup/cache semantics.
- [ ] Deferred: Recent Events collapsed by default with expand and clear history.
- [x] Focus long-press duration editing: implemented with per-mode persistence, 1-180 minute validation, preserved in-flight sessions and accurate completion credit. Fixed custom color application to timer card.
- [x] Focus decoupling and auto-transition: decoupled viewed focus mode from active running timer. Added `activeMode`, `selectMode`, and auto-transition (Work <-> Break). Added "Sets until long break" configuration. Added active indicator to tab row. Implemented and built successfully.

Only Idle Calendar was authorized for the current slice.

## Latest UI revision (2026-09-08)
- [ ] Manual acceptance of equal-height metrics, immediate dated-task updates, centered plus controls, separate Projects/Goals screens, four-tab inset-aware navigation, compact editor, JetBrains Mono and native date/time dialogs. Source implemented; verification intentionally not run.
- [ ] Manual acceptance of welcome/replay, supplied action icons, reduced-motion navigation, appearance controls, task gestures, project protocol editor and schedule ruler. These are implemented and supersede the older deferred entries below.
- [ ] Run newly authored planning-date regression cases only when verification is authorized. See `docs/UI_MANUAL_REVIEW.md`.

## Supplied artwork slice (2026-09-08)

- [x] Manual acceptance of launcher, monochrome, notification, Android 12+ splash and five navigation icons. Source implementation complete; app verification waived.
- [x] Implement agreed first-run welcome using infinitely looping mascot animation (`mascot_animation.xml`).
- [ ] Export supplied Play Store SVG as 512 x 512 PNG. Remaining action icons and animation are separate work.

## Phase 0 — Product and spikes

- [x] **P0-00 Development protocol scaffold** — Guided Build, token-efficient context manifests, learned-concepts registry, and user operating guide established.
- [x] **P0-01 Product promise and non-goals** — freeze a concise product contract and MVP boundary.
- [x] **P0-02 Task semantics scenarios** — write examples for all-day, deadline, time-block, interval and recurrence.
- [x] **P0-03 Reminder contract scenarios** — define Normal/Persistent/Strict/Focus behaviour and user resolution paths.
- [x] **P0-04 Android version policy** — choose minSdk/targetSdk based on current user/device goals and Android policy.
- [x] **P0-05 Exact alarm spike** — prototype exact alarm scheduling, process death and permission detection.
- [x] **P0-06 Alarm UI spike** — compare high-priority notification vs eligible full-screen alarm behaviour.
- [x] **P0-07 Reboot recovery spike** — demonstrate re-registration from persisted state.
- [x] **P0-08 Interval one-shot spike** — schedule next interval occurrence without polling.
- [x] **P0-09 Policy review** — document exact-alarm/full-screen-intent Google Play constraints.
- [x] **P0-10 ADR update** — record final decisions from spikes.

## Phase 1 — Foundation

- [x] **P1-01 Create Android Studio project**.
- [x] **P1-02 Configure Kotlin/Compose/Material 3**.
- [x] **P1-03 Add version catalog and dependency conventions**.
- [x] **P1-04 Add DI architecture**.
- [x] **P1-05 Add fake clock/time provider**.
- [x] **P1-06 Add unit/instrumentation test baseline**.
- [x] **P1-07 Add CI verification**.
- [x] **P1-08 Implement first Calm Pressure theme tokens**.

## Phase 2 — Domain/persistence

- [x] **P2-01 Task identifiers/status/priority**.
- [x] **P2-02 TaskSchedule hierarchy**.
- [x] **P2-03 ReminderPolicy model**.
- [x] **P2-04 Recurrence/interval model**.
- [x] **P2-05 Room schema + DAOs**.
- [x] **P2-06 Repository layer**.
- [x] **P2-07 DataStore settings**.
- [x] **P2-08 Persistence and validation tests**.

## Phase 3 — Today engine

- [x] **P3-01 TodaySnapshot model**.
- [x] **P3-02 Overdue classifier**.
- [x] **P3-03 Now/Next classifier**.
- [x] **P3-04 Anytime classifier**.
- [x] **P3-05 Completed/day-progress state**.
- [x] **P3-06 Midnight/timezone tests**.

## Phase 4 — Today UI

- [x] **P4-01 Today scaffold**.
- [x] **P4-02 Progress/day-state header**.
- [x] **P4-03 Overdue/Now/Next/Anytime sections**.
- [x] **P4-04 Completion interaction**.
- [x] **P4-05 Completed collapse**.
- [x] **P4-06 Refined Day Complete experience** (removed dedicated messages, utilizing hero card).
- [x] **P4-07 Accessibility pass**.

## Phase 5 — Editor

- [x] **P5-01 Quick add**.
- [x] **P5-02 Full task editor**.
- [x] **P5-03 Deadline picker**.
- [x] **P5-04 Time-block start/end editor**.
- [x] **P5-UI-00 Visual contract + reusable component alignment prompt**.
- [x] **P5-UI-01 Visual token + component foundation**.
- [x] **P5-05 Interval editor**.
- [x] **P5-06 Recurrence editor**.
- [x] **P5-07 Reminder policy editor**.

## Phase 6 — Alarm engine

- [x] **P6-01 ScheduledOccurrence persistence**.
- [x] **P6-02 NextOccurrenceCalculator**.
- [x] **P6-03 AlarmScheduler abstraction**.
- [x] **P6-04 Android AlarmManager implementation**.
- [x] **P6-05 AlarmReceiver stale-event guard**.
- [x] **P6-06 Complete/snooze/reschedule notification actions**.
- [x] **P6-07 Exact-alarm fallback**.
- [x] **P6-08 Alarm diagnostics log**.

## Phase 7 — recurrence/interval

- [x] **P7-01 Interval next-event engine**.
- [x] **P7-02 Interval active-window handling**.
- [x] **P7-03 Recurrence engine**.
- [x] **P7-04 Short-month/leap-date rules**.
- [x] **P7-05 Recurrence preview**.
- [x] **P7-06 Edge-case test matrix**.

## Phase 8 — escalation

- [x] **P8-01 Persistent re-alert state machine**.
- [x] **P8-02 One-shot escalation scheduling**. Verified with 272 JVM tests, both APK builds, and 4 targeted Room emulator tests; unrelated existing lint errors are deferred below.
- [x] **P8-03 Strict snooze constraints**. Implementation complete; focused tests passed before the user waived further Phase 8 verification. Remaining checks skipped.
- [x] **P8-04 Alarm-grade UI where eligible**. Implemented; verification waived.
- [x] **P8-05 Privacy mode**. Implemented; verification waived. Refined to lock-screen only privacy.
- [x] **P8-06 Stop-chain-on-resolution tests**. Implementation and receiver-side UI coordination complete; verification waived.

User exception (2026-09-07): skip verification runs for the remaining Phase 8 work only. Continue implementation and handoff documentation; do not mark skipped checks as passed. Normal verification resumes in Phase 9.

## Phase 9 — Lifecycle recovery/repair

- [x] **P9-01 Boot/time/timezone/package-replaced reconciliation**. Implemented; verification waived. Added `LifecycleRecoveryReceiver` and `ScheduleReconciler`.
- [x] **P9-02 Permission/capability repair flow**. Implemented; verification waived. Added `CapabilityRepairFlow` and registered `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- [x] **P9-03 Alarm diagnostics screen**. Implemented; verification waived. Accessible via "More" bottom nav item.
- [x] **P9-04 Manual repair schedules action**. Implemented; verification waived. Integrated into Diagnostics and Today screens.

## Phase 10 — Widget

- [x] **P10-01 Today widget state projection**. Implemented; verification waived. Added Jetpack Glance and `WidgetStateReconciler`.
- [x] **P10-02 Now/Next + unresolved count**. Implemented; verification waived. Refined Glance UI.
- [x] **P10-03 Widget privacy and quick actions**. Implemented; verification waived. Added privacy toggle support and "Done" action.

## Phase 10A — Projects and Goals

- [x] **P10A-01 Teach Task → Project → Goal boundary and accept ADR 0003**. Done.
- [x] **P10A-02 Goal/Project domain models + persistence**. Implemented; verification waived. Added `Goal`, `Project`, and `Task` provenance support with Room v5 schema.
- [x] **P10A-03 Task-to-Project linking + provenance**. Implemented; verification waived. Enabled project-aware queries and provenance mapping.
- [x] **P10A-04 Plans screen with Projects + Goals**. Implemented; verification waived. Added Plans screen with Projects and Goals tabs.
- [x] **P10A-05 Active/Rest phase-cycle protocol model**. Implemented; verification waived. Added `SimplePhaseCycle` and Room v6 schema.
- [x] **P10A-06 ProjectOccurrenceGenerator using fake clock**. Implemented; verification waived. Added dynamic task derivation.
- [x] **P10A-07 3-study/3-rest reference scenario + tests**. Verified via JVM `ProjectScenarioTest`.
- [x] **P10A-08 Today integration for generated project tasks**. Implemented; verification waived. Project titles now visible on Today tasks.
- [x] **P10A-09 Protocol-edit anchor semantics + stale generation tests**. Implemented; verification waived. Added project versioning and versioned provenance.
- [x] **P10A-10 Goal progress: choose/implement initial policies**. Implemented; verification waived. Added `GoalProgressCalculator` and Room v8 schema.

## Phase 11+ placeholders

- [ ] **P11-UI Appearance confirmation**. Dark-theme implementation and screen audit completed 2026-09-08; 298 JVM tests and both APK builds passed. User will perform visual/interaction confirmation. Final lint and emulator execution remain pending after a test-runner dependency download timeout. See `docs/SCREEN_CONNECTION_AUDIT.md`.
- [x] **P11-UI Today task flow connections (source only)**. Add/edit/save/project selection, completion/reopen/delete, saved draft identity and transactional reminder changes implemented 2026-09-08. Verification explicitly waived; user will report issues manually.
- [x] **P11-UI Navigation connections (source only)**. Direct reminder edit intents with stale checks, Goals tab, saved routes/back, More navigation and foreground capability/time refresh implemented; no builder or device checks run.
- [x] **P11-UI Planning editors (source only)**. Project/goal creation and editing, links, task access and derived progress implemented; project-protocol configuration remains deferred.
- [x] **P11-UI Focus and Idle (source only)**. Persistent focus/break state and completed-minute/streak metrics; manual Idle calendar entry and exit. No background notification or auto-start. Unbuilt/untested by user instruction.
- [ ] **P11-UI User confirmation**. Manually confirm all new connections. Five regression cases authored but not executed; prior 298-test checkpoint does not validate this batch.
- [ ] **P11-UI Onboarding and final assets**. Single welcome screen recommended; await supplied asset directories and design acceptance. Sizes and naming in `docs/ONBOARDING_AND_ASSET_HANDOFF.md`.
- [ ] **P11-UI Remaining design work**. Final navigation icons/animation, standalone draggable schedule ruler, curated palette/font settings and project-protocol editing. Widget projection/theme/time refresh remains a separate slice.

- [x] **P11-01 Battery & performance profiling setup**. Added `:benchmark` module and `StartupBenchmark`.
- [x] **P11-02 Database migration tests**. Added `MigrationTest` to verify Room schema jumps from v1 through v8.
- [ ] Accessibility hardening.
- [ ] Implement the new proposed screen style on already existing screens, using `docs/DAYLOOP_VISUAL_CONTRACT.md` and the shared UI foundation as the visual contract.
- [ ] Optional planning agent.
- [ ] Calendar integration.
- [ ] Beta/store readiness.

## Deferred verification repairs

- [x] Fix 8 existing `UnrememberedMutableState` lint errors in task-editor instrumentation tests: `QuickAddBarTest.kt` (30, 51) and `TaskEditorScreenTest.kt` (44, 65, 92, 124, 161, 205). Verified via successful lint run.
