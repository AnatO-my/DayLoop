# Task Semantics Scenarios

## Status

Frozen for Phase 0, task P0-02. These scenarios describe product behaviour before Android domain classes are implemented.

## Purpose

DayLoop must treat task timing as explicit product meaning, not as a single nullable due date. The same clock time can mean different things depending on whether the user meant a deadline, a working block, an interval loop, or a calendar recurrence.

## Shared vocabulary

- All-day task: belongs to a local date and has no required clock time.
- Deadline task: must be completed no later than a specific local date/time.
- Time-block task: occupies a start/end working period and may be active now or unfinished later.
- Interval task: repeats after an elapsed duration inside allowed windows/days.
- Recurring task: follows a calendar rule such as weekdays, monthly dates, or yearly dates.

## Scenario 1 - All-day task

Example: `Submit expense form today`.

User intent: the task belongs to the current local day, but the user has not promised to do it at a precise time.

Expected behaviour:

- Today shows it under Anytime Today unless another policy elevates it.
- It contributes to day progress while unresolved.
- It does not create an exact clock alarm merely because it exists.
- Rollover behaviour after midnight must be explicit: keep overdue, move to today, mark missed and create next, or ask the user.
- Completion records the local date of resolution and closes the obligation for that day.

Acceptance checks:

- A task dated 2026-08-26 with no time is not treated as due at 00:00.
- It can appear on Today all day without becoming overdue during the same local day.
- A new task added to today after DAY COMPLETE reopens the day.

## Scenario 2 - Deadline task

Example: `Submit assignment by 17:00`.

User intent: the user can work on it any time before the due moment, but missing the due time matters.

Expected behaviour:

- Before 17:00, Today may show it as Next or Anytime Today depending on reminder policy and ordering.
- After 17:00, if unresolved, Today classifies it as Overdue.
- Reminder offsets may warn before the deadline without changing the deadline itself.
- Completion is idempotent and cancels obsolete future reminders/escalations.
- Reschedule changes the due moment and invalidates stale scheduled occurrences.

Acceptance checks:

- A deadline of 17:00 is not displayed as a 09:00-17:00 work block.
- Completing at 16:59 prevents the 17:00 alarm from resurrecting the task.
- A stale alarm from the old due time is rejected after reschedule.

## Scenario 3 - Time-block task

Example: `Study circuits 09:00-11:00`.

User intent: the task owns a working period with a start, end, and duration.

Expected behaviour:

- Before 09:00, Today can show it as Next.
- From 09:00 through 11:00, Today can show it as Now or active.
- After 11:00, if unresolved, it may become Unfinished rather than simply Overdue; the exact label is a product decision.
- Rescheduling should either preserve duration or clearly ask the user to change it.
- Overlap detection can be built later because start and end are explicit.

Acceptance checks:

- A 09:00-11:00 block stores both start and end, not just the end time.
- Moving the start to 10:00 preserves a two-hour duration unless the user edits the end.
- Large Today text should still distinguish the active period from a deadline.

## Scenario 4 - Interval task

Example: `Stand up every 50 minutes between 08:00 and 20:00 on weekdays`.

User intent: the task repeats based on elapsed time inside an eligible local window.

Expected behaviour:

- The app computes the next eligible occurrence from interval, active window, eligible days, and anchor mode.
- The scheduler registers the next one-shot event instead of running a permanent polling loop.
- When an occurrence is delivered or resolved, the next eligible occurrence is computed and persisted.
- If the next interval would fall outside the active window, it moves to the next eligible window according to policy.
- The product must choose whether the next occurrence is clock-anchored or acknowledgement-anchored.

Acceptance checks:

- At 19:40 with a 50-minute interval and 20:00 window end, the next occurrence is not scheduled for 20:30 inside the closed window.
- A weekday-only interval does not fire on Saturday unless the rule includes Saturday.
- Reboot recovery can reconstruct the next occurrence from persisted rule state.

## Scenario 5 - Calendar recurrence

Example: `Review notes every Monday, Wednesday and Friday at 20:00`.

User intent: the task follows a calendar pattern, not an elapsed timer.

Expected behaviour:

- The recurrence engine expands the next applicable local date/time from the calendar rule.
- Completing Monday late does not shift Wednesday's recurrence unless the user chose an explicit policy that says it should.
- Monthly/yearly rules require documented short-month and leap-date behaviour.
- Editing a series must distinguish at least the current occurrence from future occurrences once recurrence editing exists.
- Missed occurrence behaviour must be explicit before production recurrence support is considered complete.

Acceptance checks:

- A Monday/Wednesday/Friday recurrence remains tied to those weekdays.
- A monthly rule on the 31st has a documented February policy before implementation is accepted.
- Timezone changes are tested with an injectable clock/time provider once code exists.

## Non-conflation rules

- An all-day task is not a midnight deadline.
- A deadline is not a time block ending at the same time.
- A time block is not an interval unless it contains an explicit repeated rule.
- An interval is not a calendar recurrence.
- A recurrence is not a background polling loop.
- Reminder strictness is separate from task timing type.
- Priority/importance is separate from reminder strictness.

## Product questions left for later Phase 0 tasks

- Which all-day rollover policy is MVP default?
- Does acknowledgement silence a persistent reminder, or is completion required?
- Which task types qualify for exact alarm delivery after policy review?
- Which reminder contracts can use alarm-grade UI?
- What minSdk/targetSdk and Android policy constraints shape the first implementation?

## Phase 0 verification

P0-02 is verified when:

- each MVP timing model has at least one concrete scenario;
- each scenario states user intent, expected behaviour, and acceptance checks;
- non-conflation rules are explicit;
- remaining semantic decisions are listed for later Phase 0 tasks;
- `TASKS.md` marks P0-02 complete;
- `STATE.md` points to P0-03 as the next single action.
