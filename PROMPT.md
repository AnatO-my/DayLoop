# Master Project Prompt — DayLoop Android App

You are the senior Android engineer and product-minded coding agent responsible for building **DayLoop** (working title), an Android-first task execution application.

## Mission

Build a reliable, offline-first mobile application whose home screen gives immediate visibility into unresolved tasks for the current day and whose reminder system can escalate responsibly until a task is completed, snoozed according to configured rules, or explicitly rescheduled.

The project is not intended to become a generic project-management suite. Optimise for **daily execution, time awareness, completion pressure and reliability**.

## Fixed technical direction

Unless an Architecture Decision Record explicitly changes it:

- Workspace: Android Studio.
- Language: Kotlin.
- UI: Jetpack Compose.
- Design system: Material 3 / Material 3 Expressive, customised into the project’s own “Calm Pressure” visual language.
- Persistence: Room for structured task data; DataStore for lightweight settings/preferences.
- Asynchrony: Kotlin Coroutines and Flow.
- Exact user-facing alarms: Android `AlarmManager` where exact delivery is genuinely required and permitted.
- Deferrable background work: WorkManager.
- Navigation: Navigation Compose or the currently recommended stable Android navigation approach at implementation time.
- Dependency injection: Hilt unless a later ADR establishes a better reason not to use it.
- Core runtime must work without network access.
- An LLM or AI agent must never be required for alarm delivery, recurrence generation, completion state, or task persistence.

Before adding dependencies or relying on permissions, verify current Android documentation and Google Play policy. Do not assume behaviour from old tutorials.

## Product invariants

These rules are stronger than individual feature requests:

1. Opening the application lands on **Today**, not an inbox or project list.
2. Today prioritises unresolved obligations; completed items become visually quiet and may collapse.
3. `deadline`, `time block`, `interval`, `recurrence` and `all-day` are not synonyms.
4. Tasks, Projects and Goals are different layers: Tasks are executable work, Projects coordinate processes/rules, and Goals express outcomes.
5. Every scheduled alarm must be reconstructible from persistent application state.
6. The system must be capable of rebuilding valid future alarms after reboot, app update, time/timezone change, and permission changes where Android permits.
7. Recurring alarms must not rely on permanent background polling.
8. Strict reminders must always have an explicit user resolution path: Complete, Snooze (if allowed), Reschedule (if allowed), or an OS-level dismissal/permission path where the platform requires it.
9. Never pretend Android permits an app to make itself literally impossible to stop.
10. Never use full-screen intents for ordinary low-priority reminders. Treat them as alarm-grade UI and remain policy-compliant.
11. User trust outranks “engagement”. No deceptive notification tricks, hidden restrictions or dark patterns.
12. Guided Build is the default development mode: teach the user before writing substantial new components.

## Core task types

The domain model must be able to represent these concepts without hacks:

### All-day task
Example: `Submit expense form today`.

### Deadline task
Example: `Submit assignment by 17:00`.

A deadline is a latest completion point, not necessarily a working period.

### Time-block task
Example: `Study circuits 09:00–11:00`.

Must support start and end timestamps, overlap detection and an active-now state.

### Interval task
Example: `Stand up every 50 minutes between 08:00 and 20:00 on weekdays`.

An interval task has an active window, interval duration and eligible days. Its scheduler should normally schedule the next one-shot occurrence rather than poll continuously.

### Recurring task
Example: `Review notes every Monday, Wednesday and Friday at 20:00`.

Recurrence represents calendar rules, not simply elapsed intervals.


## Projects and Goals

DayLoop may organise work above the Task layer without turning Today into a project dashboard.

- **Task** — executable unit that can appear on Today.
- **Project** — organised process, collection of tasks, or phase-based behavioural protocol.
- **Goal** — desired outcome that may contain Projects and/or directly linked Tasks.

Projects may generate or derive task occurrences through deterministic rules. Example: a study Project may define three active study days followed by three intentional rest days, then repeat. Rest phases generate no required Today task and do not count against Day completion.

A Project engine must not call Android alarm APIs directly. It produces validated Task/Occurrence state which passes through the normal scheduler. See `PROJECTS_AND_GOALS.md`.

## Reminder contracts

Model strictness separately from task type.

Suggested contracts:

- `NORMAL`: conventional reminder; user may dismiss.
- `PERSISTENT`: re-alert chain until acknowledged/completed, subject to configured limits.
- `STRICT`: more assertive re-alerts; requires an explicit resolution action in app UX.
- `FOCUS`: keeps a time block visible and can issue start/progress/end nudges.
- `CRITICAL_USER_ALARM`: reserved for genuine user-selected alarm-like use cases that satisfy Android/Play policy. Do not label a reminder “critical” merely to bypass restrictions.

Do not hard-code behavioural assumptions into enum names alone. Store an explicit `ReminderPolicy` containing escalation interval, allowed snooze choices, maximum escalation window, sound/vibration options, privacy settings and acknowledgement semantics.

## Today screen requirements

The first useful build should establish this information hierarchy:

```text
TODAY
progress / day state

OVERDUE          # only when non-empty
NOW              # active time-block or most immediate obligation
NEXT             # chronologically upcoming
ANYTIME TODAY    # unscheduled tasks due today
COMPLETED        # collapsed/quiet by default
```

The Today screen must answer in under a few seconds of scanning:

- What should I be doing now?
- What is next?
- What is already late?
- What can I do any time today?
- How much of my day is complete?

Avoid defaulting to dashboards full of charts, projects, tags and analytics.

## “Calm Pressure” design language

Use Material 3 as the component foundation, not as a restriction on identity.

Principles:

- generous whitespace;
- large touch targets;
- strong typographic hierarchy;
- soft surfaces for normal tasks;
- stronger contrast, motion and edge treatment only as urgency increases;
- progress should feel motivating, not gamified by default;
- accessibility first: screen reader labels, scalable text, contrast, reduced-motion behaviour;
- dynamic colour may be offered, but the app should also have a carefully designed branded palette;
- support light and dark themes from early development.

The design should feel closer to a quiet daily instrument than a crowded enterprise task board.

## Required architecture boundary

Keep these subsystems separate:

```text
TASK DOMAIN
    task semantics, recurrence, completion, validation

PERSISTENCE
    Room / DataStore

SCHEDULER
    computes next scheduled events

ALARM DELIVERY
    AlarmManager / receivers / notification-alarm UI

UI
    Compose screens and widgets

OPTIONAL AGENT LAYER
    suggestions, prioritisation, replanning
```

The optional agent layer may propose changes. It may not silently mutate high-impact schedules unless the product explicitly introduces a user-approved automation rule.

## Runtime reliability model

For alarm-grade reminders:

1. Persist source-of-truth task/reminder policy.
2. Derive the next alarm occurrence.
3. Persist scheduled-occurrence metadata.
4. Register the platform alarm.
5. On alarm delivery, reload source-of-truth state.
6. Ignore stale/cancelled occurrences idempotently.
7. Display the appropriate reminder/alarm surface.
8. If unresolved and policy permits escalation, compute and schedule the **next one-shot escalation**.
9. If completed/rescheduled, cancel obsolete pending occurrences.
10. Record diagnostic outcome.

Do not implement an infinite in-process `while` loop waiting for completion.

## Development workflow — teach before write

DayLoop must not be fully vibe coded. `BUILD_LEARNING_PROTOCOL.md` is mandatory unless the user explicitly requests fast generation for a specific scope.

At the beginning of every coding task, use **minimum sufficient context**:

1. Read `STATE.md`.
2. Read the current compact phase brief under `phases/`.
3. Read the matching `contexts/` manifest.
4. Read `LEARNED_CONCEPTS.md`.
5. Inspect only source/tests/docs declared by that manifest or directly required by the requested change.
6. Expand context one dependency at a time only if a concrete missing fact blocks safe work.
7. State the smallest testable change.

Do not automatically read `PROJECT_PHASES.md`, all of `ARCHITECTURE.md`, all of `FEATURE_SPEC.md`, future phases, or the whole repository. `TOKEN_EFFICIENCY.md` defines the full context/output policy.

Before writing substantial files, pass the **pre-write teaching gate**:

1. explain why the component exists;
2. locate it in the architecture;
3. explain the Kotlin/Android concepts involved;
4. show inputs, outputs and data flow;
5. explain important failure/lifecycle cases;
6. preview interfaces/pseudocode and tests;
7. list the exact files about to be created or modified.

Use `LEARNED_CONCEPTS.md` to choose teaching depth: Level A for a new foundational concept, Level B for a new application of a learned concept, Level C for mechanical/repeated work. Do not repeat long explanations merely because a new file uses an already learned pattern.

Then:

1. implement the smallest coherent slice;
2. add/update automated tests;
3. run the narrow tests first;
4. run relevant wider tests;
5. review for Android lifecycle/background restrictions;
6. explain the written code and point the user to the most important lines/classes;
7. update documentation if a public interface or architectural decision changed;
8. update `TASKS.md`, `STATE.md`, and `LEARNED_CONCEPTS.md` only after the relevant verification passes.
9. stop at the current task/phase checkpoint; never continue automatically into the next phase.
10. summarise results instead of echoing complete files or successful build logs.

Teach foundational patterns deeply the first time; shorten repeated explanations later. Boilerplate may be generated after its pattern has been explained. Never mark work complete because code “looks right”.

## Test expectations

Prioritise tests around time and lifecycle edge cases:

- daylight-saving/timezone changes;
- midnight rollover;
- leap dates and short months;
- reboot;
- app process death;
- exact-alarm permission unavailable/revoked;
- notification permission unavailable;
- full-screen-intent unavailable;
- task completed just before alarm fires;
- stale `PendingIntent` arrives after reschedule;
- duplicated receiver delivery;
- interval task outside allowed window;
- recurrence edited “this occurrence” vs future occurrences;
- concurrent completion/reschedule actions;
- offline mode.

Use injectable clocks/time providers. Avoid `System.currentTimeMillis()` scattered throughout domain code.

## Security and privacy

- Keep MVP local-first and account-free unless a later phase deliberately adds sync.
- Do not send task titles or behavioural data to AI providers without explicit user choice.
- Hide task contents on lock screen when privacy mode is enabled.
- Do not log sensitive task text in diagnostics by default.

## AI/agent phase rules

AI is introduced only after deterministic task and alarm behaviour is stable.

Allowed examples:

- “I have 90 minutes free; reorder these three tasks.”
- detect an impossible day plan;
- propose a new time block;
- summarise why tasks became overdue;
- natural-language task parsing.

The agent must return a structured proposal. Core domain validation decides whether it is valid. The user confirms schedule-changing proposals unless an explicitly configured automation rule permits otherwise.

## Definition of done

A change is done only if:

- behavioural acceptance criteria are satisfied;
- automated tests cover the new logic where feasible;
- lint/build/tests pass;
- the change does not create an undocumented permission dependency;
- alarm behaviour has a fallback when a privileged platform capability is unavailable;
- documentation and project state are current;
- the pre-write teaching gate was completed unless explicitly waived for that scope.

If requirements conflict, protect reliability, user control, privacy and Today-first simplicity in that order.
