# DayLoop Project Phases

This roadmap is deliberately staged so that UI polish and AI do not outrun the reliability of the task/alarm engine.

Each phase has a **goal**, **deliverables**, **tests/checkpoints**, and **exit gate**. Do not start a later phase merely because it is more interesting.

**Guided Build applies to every phase.** Before substantial files are written, complete the teaching gate in `BUILD_LEARNING_PROTOCOL.md`. Updates to existing schemas/behaviour must also follow `UPDATES_AND_UPGRADES.md`.

## Execution model — use compact phase briefs

This file is the **master roadmap**, not the normal per-session context. Execute phases through `phases/` and the matching `contexts/` manifest. A fresh session should normally read only `STATE.md`, the current phase brief, its context manifest, `LEARNED_CONCEPTS.md`, and the source/tests declared there.

Follow `TOKEN_EFFICIENCY.md`: build one verified slice, update the handoff files, then stop. Do not automatically continue into the next phase.

---

## Phase 0 — Product Contract and Technical Spike

### Goal
Freeze the first coherent product definition and prove the risky Android behaviours before building the application around assumptions.

### Deliverables
- Finalise working app name or retain `DayLoop` as codename.
- Write a one-page product promise and non-goals.
- Confirm minimum Android version and target SDK based on current policy.
- Spike exact-alarm permission behaviour on representative Android versions.
- Spike normal high-priority notification vs alarm-style/full-screen behaviour.
- Spike reboot rescheduling.
- Spike one-shot interval recurrence.
- Decide whether `SCHEDULE_EXACT_ALARM` or another permitted capability is appropriate for the app’s actual use case.
- Confirm Play policy implications before relying on full-screen intent or exact alarms.
- Record findings as ADRs.

### Checkpoints
- [ ] Exact alarm fires after app process is killed.
- [ ] App can detect inability to schedule exact alarms.
- [ ] App presents a graceful fallback.
- [ ] Reboot test demonstrates reconstruction of a scheduled alarm.
- [ ] No polling service is needed for interval reminders.

### Exit gate
We can explain precisely which task/reminder types need exact delivery and what happens when Android refuses the privileged path.

---

## Phase 1 — Android Project Foundation

### Goal
Create a maintainable Android Studio project with the smallest viable architectural skeleton.

### Deliverables
- Kotlin Android project.
- Jetpack Compose enabled.
- Material 3 theme with light/dark support.
- Version catalog.
- Core modules/packages.
- Hilt setup if retained by ADR.
- Coroutines/Flow conventions.
- Test infrastructure.
- CI build/lint/unit test workflow.
- Central `Clock`/time abstraction.

### Suggested modules

```text
:app
:core:model
:core:database
:core:time
:core:alarms
:core:notifications
:core:recurrence
:core:design
:feature:today
:feature:taskeditor
:feature:settings
```

Start with fewer Gradle modules if build complexity outweighs benefits; package boundaries may precede module extraction.

### Exit gate
Clean build, sample Compose screen, dependency injection, fake clock and unit test all work from a fresh clone.

---

## Phase 2 — Domain Model and Persistence

### Goal
Represent the task semantics correctly before building rich UI.

### Deliverables
- `Task` domain model.
- `TaskSchedule` sealed hierarchy/value objects.
- `ReminderPolicy`.
- `RecurrenceRule`.
- `TaskStatus` and completion record.
- Room entities/DAOs.
- Mapper layer between persistence and domain.
- DataStore preferences for global reminder/theme settings.
- Repository interfaces.

### Required schedule shapes
- All-day.
- Deadline.
- Time block (start/end).
- Interval (interval + active window + eligible days).
- Calendar recurrence.

### Tests
- schedule validation;
- invalid end-before-start;
- interval window validation;
- persistence round-trip;
- task completion idempotence;
- date boundary cases.

### Exit gate
Every MVP task type can be created, stored, read, edited and completed without UI-specific logic.

---

## Phase 3 — Today Query Engine

### Goal
Make “today” a domain capability, not merely a UI filter.

### Deliverables
- `TodaySnapshot` use case.
- Buckets: overdue, now, next, anytime, completed.
- Deterministic ordering.
- Day progress calculation.
- `DayState = OPEN | COMPLETE`.
- Midnight rollover behaviour.
- Flow-based observation from Room.

### Tests
- time-block active-now detection;
- all-day inclusion;
- deadline overdue state;
- tasks crossing midnight;
- completed tasks no longer counted as unresolved;
- empty day is handled intentionally.

### Exit gate
Given a fake clock and fixture data, the application can produce the exact Today screen model without Compose.

---

## Phase 4 — Today UI / Calm Pressure Design System

### Goal
Create the signature homepage.

### Deliverables
- Today screen.
- Day progress header.
- Overdue section.
- Now card.
- Next list.
- Anytime Today list.
- Collapsible Completed section.
- Quick-complete action.
- Floating add action.
- Empty and Day Complete states.
- Material 3-based branded theme.
- Accessibility semantics.

### UX requirements
- Unresolved information above analytics.
- One-handed operation where possible.
- Completion action easy but not accidental.
- Urgency communicated without turning the entire interface red.
- No project dashboard on first launch.

### Exit gate
A user can understand the remaining day at a glance and complete tasks from the home screen.

---

## Phase 5 — Task Creation and Time Semantics

### Goal
Make task entry fast while preserving advanced scheduling capability.

### Deliverables
- Quick-add sheet.
- Full editor.
- Schedule type selector.
- Date/time pickers.
- Start/end duration editor.
- Interval editor.
- Recurrence editor.
- Reminder contract editor.
- Priority/importance separate from reminder strictness.
- Validation messages.

### UX target
Common task in a few taps; advanced controls progressively disclosed.

### Exit gate
All domain schedule types can be created/edit correctly from UI without ambiguous date fields.

---

## Phase 6 — Scheduler and Exact Alarm Engine

### Goal
Establish reliable, reconstructible scheduling.

### Deliverables
- `NextOccurrenceCalculator`.
- alarm request identity strategy;
- platform `AlarmScheduler` abstraction;
- Android AlarmManager implementation;
- alarm receiver;
- cancellation/reschedule path;
- exact-alarm capability checker;
- fallback strategy for non-exact delivery where appropriate;
- alarm diagnostics log.

### Reliability rules
- Source of truth remains in Room.
- `PendingIntent` arrival is treated as a signal, not trusted as current state.
- Receiver reloads task/occurrence and rejects stale events.
- Alarm identifiers are stable and collision-safe.
- A task update cancels/replaces obsolete occurrences.

### Tests
- stale alarm;
- duplicate alarm;
- completed-before-delivery;
- rescheduled-before-delivery;
- process death;
- permission absent.

### Exit gate
Scheduled tasks fire reliably in instrumented tests and manual device tests without keeping the app process alive.

---

## Phase 7 — Interval and Recurrence Engine

### Goal
Implement repeating behaviour without polling and without date arithmetic bugs.

### Deliverables
- interval next-occurrence calculation;
- active-day/window rules;
- self-scheduling one-shot interval alarms;
- recurrence expansion;
- editing recurrence scope: this occurrence / this and future / series (if implemented in MVP);
- missed-occurrence policy;
- short-month handling decision;
- recurrence preview.

### Tests
- 31st-of-month behaviour;
- leap year;
- weekday-only interval;
- interval after active window closes;
- timezone shift;
- daylight-saving behaviour for travelling users;
- reboot after missed interval.

### Exit gate
Intervals and recurrences behave predictably across calendar edge cases.

---

## Phase 8 — Persistent and Strict Reminder Escalation

### Goal
Implement the project’s defining completion-pressure system safely.

### Deliverables
- re-alert-until-acknowledged chain;
- configurable escalation interval;
- maximum escalation horizon;
- Complete action;
- Snooze action with policy-controlled presets;
- Reschedule action;
- privacy mode for lock-screen content;
- vibration/sound channel strategy;
- alarm-style UI for eligible user-selected alarms;
- fallback when full-screen intent is unavailable.

### Core algorithm
Do not hold a process open. When an alert remains unresolved, persist state and schedule the next one-shot escalation occurrence.

### Safety/UX requirements
- The user understands strict behaviour before enabling it.
- The user can globally disable/adjust alarm behaviour in settings.
- The app never misrepresents OS restrictions.
- Urgent surfaces are reserved for explicitly selected urgent/alarm tasks.

### Exit gate
A persistent task continues to re-alert according to policy after process death and stops immediately when resolved.

---

## Phase 9 — Lifecycle Recovery and Diagnostics

### Goal
Make failures visible and recoverable.

### Deliverables
- boot completed recovery;
- time/timezone-change recovery where appropriate;
- package replaced/app update recovery;
- exact-alarm permission state handling;
- notification permission handling;
- full-screen-intent capability detection where applicable;
- diagnostics page showing scheduled next events and failure reason codes;
- “repair schedules” action.

### Exit gate
The app can explain why a reminder did not use the expected delivery path and can reconstruct valid schedules.

---

## Phase 10 — Home-Screen Widget and Quick Actions

### Goal
Provide daily visibility without opening the app.

### Deliverables
- Today widget.
- unresolved count;
- Now/Next item;
- quick completion where Android widget APIs safely permit;
- privacy-conscious widget setting;
- deep link to Today/task detail.

### Exit gate
The widget remains consistent with Room state and never becomes a second source of truth.

---

## Phase 10A — Projects, Goals and Behavioural Protocols

### Goal
Add a lightweight long-horizon planning layer without compromising the Today-first product.

### Product boundary

```text
Goal → Project → Task/Occurrence → normal Scheduler
```

Projects/Goals live primarily on a secondary `Plans` surface. Today shows only executable work, with optional subtle project provenance.

### Deliverables
- `Goal` and `Project` domain models;
- linking ordinary tasks to Projects;
- Goal → Project association;
- project provenance on generated/linked tasks;
- Plans screen;
- simple Goal progress policies;
- deterministic `ProjectProtocol`;
- Active and Rest project phases;
- `ProjectOccurrenceGenerator`;
- phase-cycle anchor semantics;
- reference protocol: `3 study days → 3 rest days → repeat`;
- protocol edit/reconciliation rules.

### Reliability rules
- Project engine never calls Android alarm APIs directly.
- Rest phases generate no required task and do not reduce Day completion.
- Generated occurrences carry project/rule provenance and generation/version metadata.
- Editing a protocol invalidates stale generated occurrences deterministically.
- Existing normal Tasks remain valid without belonging to a Project or Goal.

### Teaching checkpoints
Before implementation, teach:
- why recurrence/interval/project-cycle are different;
- Task → Project → Goal domain modelling;
- phase-state evaluation from an anchor date;
- derived occurrence provenance;
- how project rules reuse the normal scheduler instead of bypassing it.

### Tests
- active day derives expected task;
- rest day derives no required task;
- day-complete calculation ignores intentional rest;
- cycle boundary 3 → 3 → repeat;
- project pause differs from Rest phase;
- anchor-date change;
- protocol edit invalidates stale generated occurrence;
- linked ordinary task behaves normally;
- project-generated task reminder still uses standard alarm pipeline.

### Exit gate
A user can define a lightweight study/rest Project, see only the appropriate active-day task on Today, and relate that Project to a longer-term Goal without turning Today into a project dashboard.

---

## Phase 11 — Product Quality, Accessibility and Battery Discipline

### Goal
Harden the application before adding intelligence.

### Deliverables
- accessibility audit;
- TalkBack tests;
- large text/layout tests;
- dark theme review;
- battery behaviour review;
- performance profiling;
- startup optimisation;
- notification channel migration tests;
- database migrations;
- backup/export strategy;
- local data export/import if desired.

### Exit gate
Core product is reliable enough that adding AI cannot mask foundational defects.

---

## Phase 12 — Optional Planning Agent

### Goal
Add intelligence without putting reliability under probabilistic control.

### First agent capabilities
- detect impossible schedules;
- propose reordered tasks;
- propose new start/end times;
- natural-language quick capture;
- explain why a day is overloaded;
- generate a recovery plan after delays.

### Architecture

```text
TodaySnapshot + constraints
        ↓
Planner Agent
        ↓
Structured Proposal
        ↓
Domain Validator
        ↓
User Confirmation
        ↓
Task Repository + Scheduler
```

### Rules
- No direct alarm API access from LLM output.
- Validate every proposed timestamp.
- Persist user-approved changes only.
- AI failure leaves original schedule untouched.
- Local/on-device model support may be explored later.

### Exit gate
AI can improve a plan but the app remains fully functional when AI is disabled or offline.

---

## Phase 13 — Calendar and Optional Sync

### Goal
Integrate external commitments only after local task semantics are stable.

### Possible deliverables
- read-only calendar overlay first;
- explicit calendar event creation second;
- conflict detection;
- account/sync architecture;
- multi-device sync protocol;
- mutation outbox/offline conflict handling.

Do not make cloud sync a prerequisite for core reminders.

---

## Phase 14 — Beta, Store Readiness and Field Reliability

### Goal
Validate behaviour on real Android devices and OEM battery-management variations.

### Deliverables
- closed beta;
- device matrix tests;
- crash/ANR monitoring decision;
- privacy policy;
- Play Data Safety form;
- permission rationale screens;
- exact-alarm/full-screen-intent policy review;
- store listing;
- onboarding that explains reminder contracts;
- release checklist.

### Exit gate
The product claims in the store listing exactly match real behaviour on supported Android versions.

---

# Deferred ideas

These are deliberately outside early phases:

- social accountability partners;
- collaboration/team tasks;
- punitive streaks;
- desktop/web clients;
- iOS port;
- location-triggered tasks;
- wearable integration;
- voice assistant integration;
- advanced analytics;
- marketplace/plugins;
- autonomous AI schedule mutation without explicit policy.
