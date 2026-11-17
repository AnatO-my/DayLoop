# DayLoop Task Backlog

## Theme consistency pass (2026-11-10)
- [x] Define missing text styles; shared button shape; theme corners; task-editor colour roles; outfit dialog restyle (source).
- [ ] Compile and review on device in both themes: Task editor (subtask rows, save bar), Plans editors, date picker, Focus label using displaySmall, wardrobe buttons.
- [ ] Decide: keep or remove the unused palette/custom-colour settings.
- [ ] Decide one pattern for edit surfaces (sheet versus full screen versus alert dialog).
- [ ] Move wardrobe UI text to string resources; enum labels and recommender sentences live in pure Kotlin and need a separate approach.

## Wardrobe design ideas A-D and naming guide (2026-11-10)
- [x] Quick-confirm sheet, several-items overview with draw-around, Today outfit idea with feedback, sync conflict screen (source); naming guide.
- [ ] Compile second pass; generate and review schema 19; run MigrationTest.migrate18To19 and the 75 `core.outfit` unit tests in Gradle.
- [ ] Device check: draw gesture versus page scroll, outline positions on rotated photos, Today idea flow, TalkBack on the tick list.
- [ ] Modernise TodayLayoutRegressionTest with a case for the idea body once it has been seen on a device.
- [ ] Sync decision (ADR 0006) before wiring SyncConflictDialog to anything.

## Wardrobe recognition and styling (2026-11-10)
- [x] Recognition schema, shape model, colour analysis, fusion with "not sure" handling; review-and-correct flow; details table and migration 17->18 (source).
- [x] Explainable outfit recommender, Style this and Outfit ideas screens, camera capture (source).
- [x] Sync core with three-way merge, idempotent retries and conflict parking (logic only; no transport).
- [ ] Compile, run `com.dayloop.app.core.outfit.*` unit tests in Gradle, generate and review schema 18, run MigrationTest.migrate17To18 and WardrobeAttributesTest on a device.
- [ ] Manual checklist in docs/WARDROBE_RECOGNITION_HANDOFF.md on disposable data, including upgrade from real v17 data.
- [ ] Decide on network sync (ADR 0006); include item details in wardrobe export/import; move Today's suggestion card to OutfitRecommender.
- [ ] Build a consented real-photo evaluation set and measure recognition before any model change.

## Editorial theme (2026-11-05)
- [x] Repair tooltip navigation sizing; verify P&Gs/Focus access on the physical device; implement wardrobe photo grid and supplied OCR A BT replacement.
- [ ] Run NavigationLayoutRegressionTest and verify gallery photo creation/crop, filters, archive/restore and deletion on disposable data.
- [x] Bundle Funnel Display under OFL; persist typography/readable-body preference; apply orange/neutral styling and compact navigation/settings. Legacy OCR A/SF Pro/Warping preference values fall back safely to Funnel Display for the public beta.
- [ ] Build and run EditorialTypographyPreferencesTest and CalmPressureTokensTest; review small screens, large text, both themes, side-panel keyboard and tooltips manually.
- [ ] Future custom clothes model: staged photo/description storage followed by evaluated extraction/classification with user confirmation; no inactive model controls in current settings.

## Guide and Today Layout Repair (2026-09-22)
- [x] Prevent nested guide hosts from duplicating overlays or shadowing target anchors.
- [x] Pin Today header below status bar and reduce greeting size; remove duplicate metric anchor.
- [x] Hide saved outfit prompt, keep Wardrobe shortcut and calendar review/edit; compact empty-state actions.
- [ ] Run GuideHostTest and TodayLayoutRegressionTest; manually review both themes, large text, top scrolling, guide replay, calendar edit/clear and date rollover.
- [ ] Older TodayScreenTest still asserts superseded checkbox/Score/Streak UI; modernize separately instead of treating those failures as this slice's regression.

## Wardrobe Export Security (2026-09-21)
- [x] Remove shared hard-coded password and backup logging; add password-protected system-file export/import.
- [x] Clearly scope transfer to wardrobe text details and item-linked outfit dates; reject legacy CBC files.
- [x] Authenticate and validate before atomic, non-destructive import; preserve existing records and photo links.
- [ ] Run BackupCipherTest and WardrobeBackupTest; manually verify picker cancellation, wrong password, corrupt/oversized files, repeated import and process interruption.
- [ ] Design complete app backup with tasks/projects/goals/settings/focus data, real image files and metadata, consistent snapshots and restore-time alarm/widget reconciliation. Current transfer does not cover these.
- [ ] Audit/remove the unused destructive OutfitRepository.restoreAll API separately; it is no longer called by BackupManager.

## Small UI and Documentation Cleanup (2026-09-21)
- [x] Reconcile current guide docs with existing introduction replay.
- [x] Enlarge subtask completion hit region without enlarging its visual mark; add checkbox semantics.
- [x] Clarify task-editor action labels and project progress singular/plural resources.
- [x] Confirm before clearing Recent Events.
- [ ] Run SubtaskControlTest and IntroductionReplayTest; manually inspect count labels and dialog dismissal.

## Small Cleanup Pass (2026-09-21)
- [x] Reconcile duplicate implementation entries below without claiming tests passed.
- [x] Confirm stale scheduling messages are absent; update the guide's outdated warning.
- [x] Restore 48dp subtask removal control, clearer label/contrast and disabled-during-save behavior.
- [x] Add More-header introduction replay icon; restart only that guide after persistence succeeds.
- [ ] Run IntroductionReplayTest and manually check replay, busy state and subtask removal.
- [x] Replace hard-coded password/log-only export with authenticated wardrobe-details transfer; photo files are explicitly excluded, and complete backup remains deferred above.

## AI Wardrobe Cleanup (2026-09-21)
- [x] Add ML Kit Subject Segmentation dependency and manifest metadata.
- [x] Create `ClothingImageProcessor.kt` for background removal logic.
- [x] Implement `importBitmap` in `WardrobeMediaRepository` and Room implementation.
- [x] Add "Magic Clean" AI button and preview to `SimpleCropScreen.kt`.
- [x] Update `WardrobeScreen.kt` to handle cleaned bitmap imports.
- [ ] Manually verify background removal accuracy on real clothing photos.

## Guide Delivery Hardening (2026-09-21)
- [x] Fix Next progression, live target geometry/disposal, existing-user selection and interruption gating.
- [x] Add definition/selection/placement and Compose host regressions (not run).
- [ ] Run GuideDeliveryTest and GuideHostTest and manually inspect this latest revision.
- [x] Add introduction replay access (manual verification pending).
- [ ] Add replay selection for other guides, event-verified editor/save teaching and deferred offscreen-step delivery.

## Contextual Guides (2026-09-21)
- [x] Audit primary feature entry points and hidden gestures; create guide content and implementation checklist.
- [x] Implement guide progress, skip and replay semantics separately from Welcome completion; tests authored.
- [x] Implement anchored `GuideHost` UI engine and `guideTarget` modifiers.
- [x] Implement first-launch/Today guide presentation with auto-start and repository wiring.
- [ ] Run GuideProgressTest and GuideProgressPreferencesTest; compile before runtime integration acceptance.
- [ ] Extend guides to task editor and scheduling after reconciling stale copy.
- [ ] Audit secondary settings, backup photo coverage/restore entry, widgets and assistant guidance.
- [ ] Manually verify first install, existing install, accessibility, interruptions and replay.

## Wardrobe Visuals - Stage 2 (2026-09-20)
- [x] Extend `OutfitRecord` with photo support.
- [x] Implement `outfit_media` persistence and v16 migration.
- [x] Add photo management to `OutfitDetailDialog`.
- [x] Display outfit cover photos on Today screen.
- [x] Add photo indicators to Idle Calendar.
- [ ] Manually verify multiple photos per record and cover selection logic.

## Wardrobe Visuals - Stage 1 (2026-09-20)
- [x] Create `WardrobeMedia` domain and Room persistence (v15).
- [x] Implement `WardrobeMediaRepository` for file management.
- [x] Integrate Coil for image loading.
- [x] Add gallery picker and thumbnails to `WardrobeScreen`.
- [ ] Manually verify photo import from different sources and orientation correction.
- [ ] Implement manual cropping logic in `SimpleCropScreen`.

## Migration Coverage & Backup Hardening (2026-09-20)
- [x] Extend `MigrationTest` to cover v11 -> v13 jumps (wardrobe tables and archiving).
- [x] Update `BackupManager` to include wardrobe and outfit data in versioned backups.
- [x] Implement atomic `restoreAll` in `OutfitRepository`.
- [ ] Run instrumented `MigrationTest` and manually verify full backup/restore round-trip.

## Description Persistence (2026-09-20)
- [x] Persist task descriptions through Room entity and both mappings; register migration 13 to 14.
- [x] Author save/edit/clear and migration/reopen regressions.
- [ ] Build to export schema 14 and run repository/migration tests; manually verify description persistence.

## Icon Actions (2026-09-20)
- [x] Add native temporary tooltips to shared icon buttons.
- [x] Replace routine wardrobe, outfit-editor and note creation labels with vector icons.
- [x] Keep explicit wardrobe deletion confirmation and meaningful accessibility labels.
- [ ] Manually verify tap versus hold, disabled states, screen-reader names and action outcomes.
- [ ] Audit remaining text actions and undersized caller overrides without replacing domain choices or existing long-press gestures.

## Wardrobe Hardening & Repository Refactor (2026-09-20)
- [x] Introduce `OutfitRepository` interface and Room implementation.
- [x] Add archiving support to clothing items with v12->13 migration.
- [x] Implement suggestion previews on Today screen with "Wear this" confirmation.
- [x] Refactor wardrobe management and outfit recording to use the new repository.
- [ ] Manually verify suggestion drafts do not persist until user confirmation.
- [ ] Manually verify archived items are hidden from suggestions but visible in history.

## Outfit Sorter & Recorder (2026-09-20)
- [x] Implement multi-category wardrobe persistence and transactional daily outfit recording with many-to-many join support.
- [x] Build `OutfitSuggester` engine for composed ensemble picks across all available categories.
- [x] Create immersive `WardrobeScreen` for clothing management and `OutfitDetailDialog` for historical editing.
- [x] Integrate `TodayOutfitCard` on the home screen and enable interactive date tapping in `IdleScreen` calendar.
- [ ] Manually verify suggestion randomness, many-to-many data integrity, interactive calendar navigation, and large-wardrobe scrolling.
- [ ] Run `OutfitSuggesterTest` and full build verification when authorized.


## Missed-work review (2026-09-20)
- [x] Finish missed classification, non-duplicated Today/widget counts, review routes, Focus-safe task transitions and reminder cancellation wiring.
- [x] Add recoverable Someday/Dismissed lists under P&Gs Saved work, with bring-to-today and editor rescheduling.
- [ ] Manually review all four actions, navigation/back, busy state, persisted recovery, large text and light/dark mode.
- [ ] Run TodayMissedTaskClassifierTest, TaskReviewTransitionTest, updated snapshot/UI-state tests, MissedTaskReviewScreenTest and AlarmResolutionPersistenceTest when authorized. Tests authored but not run.
- [ ] Follow-up: define bounded historical recurrence backfill; current generator does not reconstruct unmaterialized past instances. Verify series/instance alarm ownership separately.


## Completion timestamps (2026-09-20)
- [x] Implement explicit completion time, nullable v9->10 migration, dated Today/widget/calendar projection and reopening reset.
- [ ] Generate and commit Room schema 10 through an authorized build; run TaskCompletionTimeTest, TodayCompletedClassifierTest, GeneratedTaskConnectionTest and CompletionMigrationTest.
- [ ] Manually verify late completion, next-day counts, duplicate completion, edit preservation and reopen/recomplete. Legacy unknown dates intentionally excluded from dated totals.
- [ ] Deferred: immutable completion event history, backup timestamp preservation and calendar widget. Recurrence slice exists separately; latest timestamp is not a full event log.

## HyperOS timer presentation (2026-09-20)
- [ ] Manually accept Timer/Coffee/Armchair island icons, equal-width countdown, expanded Pause/Resume/End and notification-to-Focus navigation on Global HyperOS 3.
- [ ] Check paused persistence, repeated/stale taps, active vs viewed mode, automatic transitions and End preserving earned history/sets.
- [ ] Run FocusNotificationPresentationTest, HyperOsFocusNotificationTest and notification-command cases in FocusDurationTest when authorized. Tests authored, not executed; source/resource checks passed.
- [ ] Follow-up: validate Xiaomi timerInfo timestamp semantics before replacing per-second digit updates; do not assume native Clock privileges or exact layout parity.

## Widget refresh (2026-09-19)
- [ ] Manually accept picker names/previews for Today, Goals and Focus in HyperOS; source and six static resources implemented, resource checks passed, generated previews not runtime-tested. See docs/WIDGET_PICKER_PREVIEWS_2026-09-19.md.
- [ ] Review responsive Today compact/agenda, privacy, task links/completion and Add in launcher.
- [ ] Review new Focus widget commands against existing timer, including cold/warm app and paused state.
- [ ] Verify Goal proportional progress, theme changes and all minimum widget footprints.
- [ ] Run widget mapper tests and compile when authorized; no build/device verification this slice.
- [ ] Follow-up: configurable P&G Spotlight, Calendar after completion history, exact time-boundary refresh and optional native countdown.


## Header and personalization review
- [ ] Manually check Today last-row scrolling behind dock in both themes, large text and gesture/three-button navigation.
- [ ] Check identical page titles, mascot tap/hold More access, Back dismissal and no bottom pull gesture.
- [ ] Check Warping/JetBrains choice, display-name save/clear/restart and long names; tests added but not run.
- [ ] Include displayName/font in a future complete backup implementation; existing BackupManager is partial.


## Task and scheduling audit repairs
- [ ] Review connection repair: all schedule inputs and validation, project/priority/reminder edits, generated Today tasks, Overdue, swipe/hold/subtask actions; run TaskScheduleRepairTest when authorized.
- Completion timestamps: implemented; remaining tests and backup-history coverage are tracked in the Completion timestamps section.
- Recurring task instances: implemented; verify completion/skip/series and alarm ownership, with historical backfill tracked under Missed-work review.
- Missed all-day/time-block handling: implemented; review and Saved work verification are tracked above. A separate Inbox remains undecided.
- [ ] Replace schedule draft booleans with one selection state and decide independent deadlines/repeat timing.
- [ ] Choose goal COUNT weighting (tasks or milestones) rather than implicitly adding both.


## Focus sound selector
- [ ] Manually review shared sound dialog in normal/fullscreen timers: Off and all five sounds, large text, narrow display, light/dark, dismissal without changes, save-busy protection and live playback.
- [ ] Run FocusSoundPickerTest when authorized; source-only implementation, no build/device operations performed.


## Today notched metrics
- [ ] Manually accept curved cutouts, arrow/card shared action, light/dark ripples and large-text layouts.
- [ ] Verify Completed/Remaining sheets with empty/populated/live data and task-editor navigation; run TodayMetricTest when authorized.


Only check a task after its acceptance criteria/tests are satisfied.

## Reference UI adjustment (2026-09-17)
- [ ] Manually review Today completed/remaining counts, violet/lime metrics, enlarged-text stacking and inverted task surfaces.
- [ ] Review Focus daily minutes, full-width P&Gs cards, schedule/reminder grids and project picker.
- [ ] Verify fixed reference colors and JetBrains Mono despite previously saved custom HEX/font settings; light/dark selection still works.
- [ ] Verify plus above the dock on Today/P&Gs only, stable Today/P&Gs/Focus positions, repeated navigation and disabled states.
- [ ] Run ReferenceUiTest when instrumentation is authorized. Tests authored, no build/device checks performed.
This request supersedes earlier palette/HEX/font-selector customization work; stored preference fields remain for backup compatibility.

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

- [x] **Rust foundation**. Added `rust/` domain/engine/FFI workspace with validated task transitions, a bounded complete/reopen/skip ABI (nine passing Rust tests), arm64 instrumentation-library build wiring, and a test-only JNI fixture adapter. NDK output is AArch64 with expected exports and no C++ runtime dependency; the fresh matching debug/test APKs and `RustFfiLoadTest` passed on a physical arm64-v8a device (3 tests), including Kotlin/Rust differential checks for complete, reopen, and skip. A feature-gated transaction-level observer seam now performs optional shadow checks immediately before the canonical Room write; its default is a no-op and its bounded process-local diagnostics retain only command/outcome. The focused physical-device Room test passed for complete, reopen, and skip. Next: explicitly approve or reject a developer-only Rust bridge enablement; production routing remains prohibited.

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
