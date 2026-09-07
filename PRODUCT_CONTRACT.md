# DayLoop Product Contract

## Status

Frozen for Phase 0, task P0-01. Later changes must be deliberate product-model changes, not accidental scope creep.

## One-sentence promise

DayLoop helps one person see what still matters today, act on it at the right time, and trust that local reminders remain reliable even when the app, network, or AI is unavailable.

## Product promise

DayLoop is a today-first Android task execution app. It is not mainly an inbox, archive, analytics dashboard, or project-management suite. Opening the app should answer, quickly and calmly:

- What is overdue?
- What should I be doing now?
- What is next?
- What can still be done any time today?
- Is today complete?

The app should create useful completion pressure without hiding control from the user. Reminder strictness is an explicit contract: the user chooses how assertive a task should be, sees what that means, and always has an understandable resolution path such as Complete, Snooze, Reschedule, or changing permissions/settings where Android requires it.

## Core user

The first user is an individual Android user who wants stronger daily execution support than a passive task list provides. They may have study, work, habit, deadline, or interval-based commitments, but they are not managing a team.

## MVP boundary

The MVP is complete when a user can:

1. Create tasks with distinct all-day, deadline, time-block, interval, and recurrence semantics.
2. See a Today surface organised around unresolved work before completed work.
3. Receive scheduled reminders after the app process has died, where Android allows.
4. Receive interval reminders through persisted one-shot scheduling, not background polling.
5. Configure a task to re-alert according to a clear reminder policy.
6. Complete, snooze, or reschedule from the relevant reminder surface.
7. Recover valid schedules after reboot, app update, timezone/time changes, and permission changes where Android allows.
8. Inspect basic diagnostics when alarm delivery is degraded or unavailable.
9. See DAY COMPLETE only when all required Today obligations are resolved.

## Product principles

- Today is the default surface.
- Room is the source of truth for tasks and scheduled occurrence state.
- DataStore is for preferences/settings, not canonical task data.
- Alarm delivery is deterministic and does not depend on AI, network access, or a long-running in-memory loop.
- WorkManager is for deferrable work, not exact user-facing alarms.
- Deadline, time block, interval, recurrence, and all-day are separate product concepts.
- Projects and Goals are later planning context; Today remains an execution surface.
- AI is optional and advisory. It may propose structured changes, but deterministic application code validates and applies them.
- User trust, privacy, and platform honesty outrank engagement.

## Explicit non-goals for MVP

- Team or enterprise project management.
- Chat, comments, social feeds, or collaboration.
- Kanban boards, documents, wiki pages, or a general knowledge base.
- Cloud account requirement.
- Multi-device sync requirement.
- Calendar write sync.
- Desktop, web, iOS, or cross-platform parity.
- Marketplace, plugins, or third-party automation platform.
- Autonomous AI mutation of schedules.
- Dark-pattern reminders that pretend Android cannot be overridden by the user.
- Analytics-heavy productivity scoring.
- Location-triggered reminders.
- Wearable or voice-assistant integration.

## Acceptance scenarios for this contract

These scenarios define whether future product decisions still fit the Phase 0 promise.

1. A user opens the app at 08:00 and immediately sees overdue, active-now, next, anytime-today, and completed work without first navigating through projects.
2. A deadline task and a time-block task with the same end time are displayed and stored with different semantics.
3. A persistent reminder re-alerts through persisted scheduled occurrences and stops when the task is completed, snoozed according to policy, or rescheduled.
4. If exact alarm or notification capability is unavailable, DayLoop explains the limitation and uses a documented fallback instead of crashing or pretending the reminder is guaranteed.
5. A rest day in a later Project protocol creates no required Today task and does not prevent DAY COMPLETE.
6. An AI planning feature can be disabled or offline while task creation, reminder delivery, completion, and recurrence still work.
7. A proposed feature that requires cloud accounts, collaboration, autonomous AI schedule changes, or a project dashboard as the default home screen is outside MVP unless a later ADR changes the contract.

## Phase 0 verification

P0-01 is verified when:

- this contract exists in the repository;
- MVP requirements and non-goals are explicit;
- acceptance scenarios are written;
- `TASKS.md` marks P0-01 complete;
- `STATE.md` points to the next single Phase 0 action.
