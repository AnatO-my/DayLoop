# Learned Concepts Registry

This file prevents repeated teaching while preserving DayLoop's Guided Build approach.

**Status values:** `NOT_STARTED`, `TAUGHT`, `IMPLEMENTED`, `VERIFIED`.

A concept should normally receive a Level A explanation only once. After it reaches `TAUGHT`, later uses should be Level B or C unless the new use introduces materially different behaviour.

| Concept | Status | First phase | Notes |
|---|---|---:|---|
| Repository-guided development / phase handoff | TAUGHT | 0 | Repository state should replace conversation memory. |
| Guided Build / teach-before-write | TAUGHT | 0 | Foundational components require a teaching gate. |
| Token-efficient context manifests | TAUGHT | 0 | Read only current phase + declared dependencies. |
| Android Studio project structure | VERIFIED | 1 | P1-01 created and verified the Gradle wrapper, settings, app module, manifest, and Compose entry activity. |
| Gradle version catalog / dependency conventions | VERIFIED | 1 | P1-03 taught and verified catalog aliases, Compose bundles, and direct dependency declaration for imported APIs. |
| Dependency injection / application graph | VERIFIED | 1 | P1-04 taught and verified the manual Application-owned AppGraph boundary for future app-wide dependencies. |
| Task timing model separation | VERIFIED | 0 | P2-04 verified distinct all-day, deadline, time-block, interval, and calendar-recurrence schedule models. |
| Reminder contract separation | VERIFIED | 0 | P2-03 verified `ReminderPolicy` as validated data separate from task timing, priority, acknowledgement, completion, and alarm execution. |
| Kotlin data classes and immutability | VERIFIED | 2 | P2-01 taught and verified immutable Task values with typed IDs, fixed status/priority enums, validation, and copy-based completion. |
| Kotlin sealed interfaces/classes | VERIFIED | 2 | P2-02 taught and verified sealed `TaskSchedule` variants for mutually exclusive all-day, deadline, and time-block semantics. |
| Coroutines | VERIFIED | 1–2 | P2-06 taught and verified suspend DAO/repository functions for database work without blocking callers. |
| Kotlin `Flow` / `StateFlow` | TAUGHT | 2–4 | P2-07 also verified repository/settings data `Flow`; UI `StateFlow` remains later. |
| Room entities and DAOs | VERIFIED | 2 | P2-08 also verified persisted-row reconstruction against domain validation and required exported schema columns while keeping Room separate from pure models. |
| DataStore settings/preferences | VERIFIED | 2 | P2-07 taught and verified Preferences DataStore for small app settings while keeping Room as the canonical task store. |
| Repository/use-case boundary | VERIFIED | 2–3 | P2-06 taught and verified `TaskRepository` as the app-facing boundary over Room DAO/entity details. |
| Derived domain snapshots/use cases | VERIFIED | 3 | P3-01 taught and verified `TodaySnapshot` as a pure derived domain output with section invariants, while classifier/use-case logic remains later. |
| Room migrations | VERIFIED | 6 | P6-01 taught and verified explicit Room schema versioning from v1 to v2 with a scheduled occurrence table migration. |
| Time abstraction / fake clock | VERIFIED | 1 | P1-05 taught and verified injectable TimeProvider with SystemTimeProvider and FakeTimeProvider tests. |
| Android unit/instrumentation test baseline | VERIFIED | 1 | P1-06 taught and verified JVM unit tests plus compiled Compose instrumentation tests through Gradle source sets. |
| CI verification workflow | VERIFIED | 1 | P1-07 taught and verified a local script plus GitHub Actions workflow for the foundation Gradle checks. |
| Zoned/local/instant time semantics | VERIFIED | 2–3 | P3-06 verified instant-plus-zone conversion into local Today date/time and midnight classifier boundaries, while alarm instant scheduling remains later. |
| Active-window Today classification | VERIFIED | 3 | P3-03 taught and verified inclusive-start/exclusive-end time-block Now classification plus same-day Next scheduling, while interval/recurrence occurrence policies remain later. |
| Date-only Anytime Today classification | VERIFIED | 3 | P3-04 taught and verified active all-day tasks for the target local date as Anytime, while unscheduled intent and all-day rollover remain later policy decisions. |
| Today completion and progress state | VERIFIED | 3 | P3-05 taught and verified completed-section derivation plus progress and OPEN/COMPLETE day status from snapshot contents. |
| Compose state hoisting | VERIFIED | 4 | P4-01 taught and verified `TodayScreen` receiving hoisted `TodayUiState` from its caller instead of owning canonical state. |
| Compose form state / validation | VERIFIED | 5 | P5-01 taught and verified Quick Add draft state that validates task title input before creating a plain domain `Task`. |
| Task editor round-trip preservation | VERIFIED | 5 | P5-02 taught and verified editing title/priority while preserving existing task id/status/schedule/reminder fields. |
| Deadline input validation | VERIFIED | 5 | P5-03 taught and verified explicit deadline date/time draft parsing into `TaskSchedule.Deadline` while preserving non-deadline schedules during edit. |
| Time-block input validation | VERIFIED | 5 | P5-04 taught and verified explicit start/end date/time draft parsing into `TaskSchedule.TimeBlock` with equal and reversed range rejection. |
| Interval input validation | VERIFIED | 5 | P5-05 taught and verified explicit interval start-date, duration, and active-window parsing into `TaskSchedule.Interval`, including mutual exclusivity and hidden-rule preservation on edit. |
| Recurrence input validation | VERIFIED | 5 | P5-06 taught and verified explicit recurrence frequency, repeat interval, date range, and weekly-day parsing into `TaskSchedule.CalendarRecurrence` without occurrence expansion. |
| Reminder policy editor validation | VERIFIED | 5 | P5-07 taught and verified editor parsing for Normal, Persistent, Strict, and Focus reminder policies without alarm delivery. |
| Unidirectional UI data flow | VERIFIED | 4 | P4-04 verified one-way completion intent flow from task row to caller while leaving ViewModel `StateFlow` persistence for later. |
| Material 3 theming / design tokens | VERIFIED | 1–4 | P5-UI-01 verified the accepted matcha/mint/near-black palette and expanded DayLoop semantic tokens for shared UI components. |
| Reusable Compose UI foundation | VERIFIED | 5 | P5-UI-01 taught and verified shared DayLoop screen, card, action, selector, progress, warning, and bottom-navigation components with hoisted event APIs. |
| Section-list accessibility semantics | VERIFIED | 4 | P4-03 taught and verified named Today section headings, counts, task rows, and semantic section summaries derived from `TodaySnapshot`. |
| Large-font / accessibility resilience | VERIFIED | 4 | P4-07 taught and verified text wrapping plus explicit disclosure semantics for Today content at accessibility scale. |
| Event-hoisted completion interaction | VERIFIED | 4 | P4-04 taught and verified checkbox completion emits a `TaskId` upward without mutating canonical state inside Compose. |
| Completed-section disclosure | VERIFIED | 4 | P4-05 taught and verified completed Today items as a collapsed disclosure derived from `TodaySnapshot.completed`. |
| Day Complete / empty-state presentation | VERIFIED | 4 | P4-06 taught and verified empty Today and completed Today as distinct derived presentation states from `TodaySnapshot`. |
| `AlarmManager` | VERIFIED | 0 spike / 6 production | P6-07 verified exact scheduling plus inexact idle-allowed fallback behind AlarmScheduler with typed exactness reporting and stable PendingIntent identity. |
| `PendingIntent` identity | VERIFIED | 0 spike / 6 | P6-04 verified stable occurrence data URIs, immutable broadcast PendingIntents, and generation metadata for future receiver reconciliation. |
| `BroadcastReceiver` lifecycle | VERIFIED | 0 spike / 6 | P0-05 verified receiver delivery after ordinary `am kill` process death on API 36. |
| Notification channels and runtime permission | VERIFIED | 0 spike / 6 | P0-06 verified POST_NOTIFICATIONS gating and high-importance reminder channel behavior on API 36. |
| Full-screen intent notification gating | VERIFIED | 0 spike / 8 | P0-10 accepted opt-in alarm-grade use only with canUseFullScreenIntent checks and notification fallback. |
| Scheduled occurrence persistence | VERIFIED | 6 | P6-01 taught and verified durable alarm-intent records with task id, fire instant, generation, status, and Room repository access without scheduling alarms. |
| Deterministic next-occurrence calculation | VERIFIED | 6 | P6-02 taught and verified local schedule-to-Instant calculation for active deadline/time-block tasks while leaving all-day, interval, and recurrence expansion for later slices. |
| AlarmScheduler abstraction boundary | VERIFIED | 6 | P6-03 taught and verified an Android-free schedule/cancel interface and typed result model over ScheduledOccurrence without AlarmManager or PendingIntent implementation. |
| Alarm reconciliation / stale-event guard | VERIFIED | 6 | P6-05 taught and verified receiver-side source-of-truth reload, generation matching, scheduled-status checking, and fired-occurrence persistence before delivery can advance. |
| Notification action idempotency | VERIFIED | 6 | P6-06 taught and verified complete/snooze/reschedule notification commands that reload occurrence state, ignore stale generations, avoid duplicate mutation, and keep reschedule as a deferred UI request. |
| Exact-alarm fallback / degraded precision | VERIFIED | 6 | P6-07 taught and verified fallback from exact `setExactAndAllowWhileIdle` to inexact `setAndAllowWhileIdle` with `exact = false` while preserving occurrence identity. |
| Alarm diagnostics as reliability evidence | VERIFIED | 6 | P6-08 taught and verified append-only alarm diagnostics for schedule, cancel, delivery, and notification-action outcomes without making diagnostics a source of truth. |
| Interval next-occurrence scheduling | VERIFIED | 0 spike / 7 | P7-01 taught and verified clock-anchored interval next-event calculation for one-shot scheduling without polling, leaving completion anchoring for later state. |
| Interval active-window handling | VERIFIED | 7 | P7-02 taught and verified explicit interval window status for before/inside/after/ineligible/out-of-range moments with inclusive start and exclusive end semantics. |
| Calendar recurrence algorithms | VERIFIED | 7 | P7-06 taught and verified deterministic daily, weekly, monthly, and yearly next-date calculation plus bounded recurrence preview with matrix coverage for calendar edge cases. |
| Edge-case matrix testing | VERIFIED | 7 | P7-06 taught and verified grouped boundary tests for recurrence, interval windows, scheduler integration, and DST conversion. |
| Persistent reminder state machine | VERIFIED | 8 | P8-01 verified a persisted deterministic re-alert chain model with active, resolved, capped, and cancelled states. |
| Boot/time/timezone recovery | VERIFIED | 0 spike / 9 | P0-07 verified BOOT_COMPLETED reconstruction from persisted scheduled occurrence state on API 36. |
| Glance/widget state projection | NOT_STARTED | 10 | Widget is never source of truth. |
| Task → Project → Goal modelling | TAUGHT | 10A | Product distinction has been discussed; implementation is later. |
| Project Active/Rest phase protocol | TAUGHT | 10A | 3-study/3-rest example defined conceptually. |
| Derived occurrence provenance | NOT_STARTED | 10A | Teach before implementation. |
| LLM proposal → validator → user confirmation boundary | TAUGHT | 12 | Architectural principle established; implementation later. |
| Android/Play permission-policy research workflow | VERIFIED | 0 | P0-09 verified exact-alarm and full-screen-intent constraints from current official Android/Google Play sources. |

## Update rule

After a teaching or implementation checkpoint:

1. update only the affected row;
2. keep the note to one sentence;
3. do not add a diary entry;
4. if a concept changes materially, note the new phase and why Level A teaching is needed again.












