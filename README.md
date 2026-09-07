# DayLoop — Android Task Execution App (Working Title)

> A today-first Android task system that prioritises execution, reliable alarms, interval tasks, time-blocked tasks, and explicit daily closure.

**Status:** Project scaffold / product-definition stage  
**Primary workspace:** Android Studio  
**Primary platform:** Android first  
**Language:** Kotlin  
**UI:** Jetpack Compose + Material 3 / Material 3 Expressive  
**Persistence:** Room + DataStore  
**Scheduling:** AlarmManager for user-facing exact alarms; WorkManager for deferrable maintenance work  
**Architecture:** offline-first, deterministic core; optional agent/AI layer above the task engine

## Product thesis

Most task managers optimise for capturing and organising tasks. DayLoop should optimise for **seeing what must be done now and actually completing it**.

The home screen is not a project dashboard. It is an execution surface:

- what is overdue;
- what is active now;
- what remains today;
- what is scheduled for later;
- what repeats at intervals;
- whether the day is still `OPEN` or `COMPLETE`.

The defining feature is an escalation-capable reminder system. A task may be configured to re-alert until it is completed, snoozed within allowed rules, or explicitly rescheduled. The app must still preserve operating-system user control and comply with Android alarm/full-screen-intent policies.

## Core product principles

1. **Today first.** Opening the app should answer “What do I still need to do today?” immediately.
2. **Deterministic alarms.** Alarm delivery must never depend on an LLM, network access, or a background agent loop.
3. **Different time semantics stay different.** Deadline, time block, interval, recurrence, and all-day task are separate concepts.
4. **Strictness is chosen by the user.** Normal, Persistent, Strict, Focus and Critical-style behaviours are user-configurable contracts, not hidden coercion.
5. **Offline first.** Core tasks, schedules, reminders, completion and recurrence work locally.
6. **Calm pressure.** The UI should be comfortable and low-clutter while still making unresolved commitments difficult to ignore.
7. **Projects and Goals are context, not clutter.** Longer processes and outcomes belong on a secondary Plans surface; Today still shows executable tasks.
8. **Learn while building.** Guided Build teaches each substantial component before files are generated.
9. **AI is optional.** Intelligent planning and replanning can improve the day, but must sit above the reliable task engine.
10. **Auditability.** When an alarm fails or permission is missing, the app should be able to explain what happened.

## Repository documents

- [`PROMPT.md`](PROMPT.md) — master coding-agent prompt and project contract.
- [`PROJECT_PHASES.md`](PROJECT_PHASES.md) — staged implementation roadmap with gates and acceptance criteria.
- [`LOOPS.md`](LOOPS.md) — runtime reminder loops, escalation loop, recurrence loop and development-agent loop.
- [`AGENTS.md`](AGENTS.md) — rules for coding agents working in the repository.
- [`BUILD_LEARNING_PROTOCOL.md`](BUILD_LEARNING_PROTOCOL.md) — mandatory teach-before-write workflow that prevents passive vibe coding.
- [`TOKEN_EFFICIENCY.md`](TOKEN_EFFICIENCY.md) — minimum-sufficient-context and compact-output protocol for preserving usage.
- [`LEARNED_CONCEPTS.md`](LEARNED_CONCEPTS.md) — registry that prevents repeated deep teaching of concepts already covered.
- [`PRODUCT_CONTRACT.md`](PRODUCT_CONTRACT.md) — frozen Phase 0 product promise, MVP boundary, non-goals and acceptance scenarios.
- [`TASK_SEMANTICS_SCENARIOS.md`](TASK_SEMANTICS_SCENARIOS.md) — Phase 0 examples and acceptance checks for all MVP task timing models.
- [`phases/`](phases/) — compact executable phase briefs; use these instead of loading the master roadmap each session.
- [`contexts/`](contexts/) — phase-specific minimum-context manifests.
- [`UPDATES_AND_UPGRADES.md`](UPDATES_AND_UPGRADES.md) — safe process for adding features, migrations and platform/dependency upgrades.
- [`PROJECTS_AND_GOALS.md`](PROJECTS_AND_GOALS.md) — lightweight Task → Project → Goal semantics and phase-based protocols such as study/rest cycles.
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — proposed Android architecture, modules and data flows.
- [`FEATURE_SPEC.md`](FEATURE_SPEC.md) — feature definitions and behavioural requirements.
- [`SOURCES.md`](SOURCES.md) — official Android references and open-source/product references to study.
- [`STATE.md`](STATE.md) — current implementation state and next action.
- [`TASKS.md`](TASKS.md) — executable project task backlog.
- [`agents/`](agents/) — specialised agent instructions for planning, implementation, review and QA.
- [`docs/adr/`](docs/adr/) — architecture decision records.

## Suggested initial package namespace

Use a placeholder until the final app name and domain are selected:

```text
com.example.dayloop
```

Do not publish with the placeholder namespace.

## Proposed high-level code layout

```text
app/
core/
  model/
  database/
  time/
  alarms/
  notifications/
  recurrence/
  design/
  common/
  projects/             # later
feature/
  today/
  plans/                # later: Projects + Goals
  taskeditor/
  taskdetails/
  completed/
  settings/
  diagnostics/
agent/                 # optional, introduced late
widget/                # home-screen widget
```


## Development style

This repository uses **Guided Build** by default. Before a coding agent creates a substantial component, it must teach the user the component's purpose, architectural position, relevant Kotlin/Android concepts, data flow, failure modes, and test strategy; preview the files; then write and test them. See `BUILD_LEARNING_PROTOCOL.md`.

The user may request fast generation for an individual boilerplate-heavy task, but that does not switch the whole project into vibe-coding mode.

## MVP definition

A usable MVP exists when a user can:

1. create an all-day, deadline, time-block, interval or recurring task;
2. open the app and immediately see unresolved tasks for the current day;
3. receive reliable scheduled reminders after the app process has been killed;
4. receive the next occurrence of an interval task without background polling;
5. configure a task to re-alert until acknowledged/completed;
6. complete, snooze or reschedule from the reminder surface;
7. survive reboot/timezone/time-change events without silently losing alarms;
8. inspect basic alarm diagnostics;
9. see `DAY COMPLETE` when all required tasks are resolved.

AI, cloud sync, collaboration and cross-platform support are explicitly **not required** for MVP.

## First action

Open `STATE.md`, then the current compact phase brief and matching context manifest. For the present state that is `phases/00-product-spike.md` + `contexts/phase-00.context.md`. Use `PROJECT_PHASES.md` only as the master roadmap when needed.




