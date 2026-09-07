# DayLoop Task Backlog

Only check a task after its acceptance criteria/tests are satisfied.

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
- [x] **P4-06 Day Complete experience**.
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
- [ ] **P8-02 One-shot escalation scheduling**.
- [ ] **P8-03 Strict snooze constraints**.
- [ ] **P8-04 Alarm-grade UI where eligible**.
- [ ] **P8-05 Privacy mode**.
- [ ] **P8-06 Stop-chain-on-resolution tests**.

## Phase 9 — Lifecycle recovery/repair

- [ ] **P9-01 Boot/time/timezone/package-replaced reconciliation**.
- [ ] **P9-02 Permission/capability repair flow**.
- [ ] **P9-03 Alarm diagnostics screen**.
- [ ] **P9-04 Manual repair schedules action**.

## Phase 10 — Widget

- [ ] **P10-01 Today widget state projection**.
- [ ] **P10-02 Now/Next + unresolved count**.
- [ ] **P10-03 Widget privacy and quick actions**.

## Phase 10A — Projects and Goals

- [ ] **P10A-01 Teach Task → Project → Goal boundary and accept ADR 0003**.
- [ ] **P10A-02 Goal/Project domain models + persistence**.
- [ ] **P10A-03 Task-to-Project linking + provenance**.
- [ ] **P10A-04 Plans screen with Projects + Goals**.
- [ ] **P10A-05 Active/Rest phase-cycle protocol model**.
- [ ] **P10A-06 ProjectOccurrenceGenerator using fake clock**.
- [ ] **P10A-07 3-study/3-rest reference scenario + tests**.
- [ ] **P10A-08 Today integration for generated project tasks**.
- [ ] **P10A-09 Protocol-edit anchor semantics + stale generation tests**.
- [ ] **P10A-10 Goal progress: choose/implement initial policies**.

## Phase 11+ placeholders

- [ ] Accessibility/battery hardening.
- [ ] Implement the new proposed screen style on already existing screens, using `docs/DAYLOOP_VISUAL_CONTRACT.md` and the shared UI foundation as the visual contract.
- [ ] Optional planning agent.
- [ ] Calendar integration.
- [ ] Beta/store readiness.






























