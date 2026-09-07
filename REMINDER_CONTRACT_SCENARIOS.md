# Reminder Contract Scenarios

## Status

Frozen for Phase 0, task P0-03. These scenarios define product behaviour before Android notification, alarm, receiver, or scheduler code exists.

## Purpose

A reminder contract describes how DayLoop tries to get the user's attention and what resolution paths are available. It is separate from task timing. A deadline task can use a Normal reminder, and an all-day task can use a Persistent reminder if the user explicitly configures one.

Reminder contracts must preserve user control, platform honesty, and deterministic local behaviour. DayLoop must never claim that Android cannot be overridden by the user, and it must never depend on AI or network access to deliver core reminders.

## Shared contract fields

Every production reminder policy should eventually make these ideas explicit rather than hiding behaviour behind an enum name:

- mode;
- initial reminder time or offset;
- re-alert interval, if any;
- maximum re-alert count or maximum escalation horizon;
- allowed resolution actions;
- snooze availability and presets;
- reschedule availability;
- sound/vibration profile;
- lock-screen privacy behaviour;
- acknowledgement semantics;
- whether completion stops the chain;
- platform capability required, if any;
- fallback when the capability is unavailable.

## Scenario 1 - Normal reminder

Example: `Submit assignment by 17:00` with a reminder at 16:30.

User intent: the user wants a conventional reminder but does not want an assertive re-alert chain.

Expected behaviour:

- DayLoop schedules the configured reminder where Android permits.
- The reminder can be dismissed or opened according to normal platform behaviour.
- Completion from Today or the reminder surface cancels obsolete future reminder state.
- If notification permission is denied, DayLoop records/explains the limitation and does not crash.
- Normal mode does not escalate repeatedly merely because the task is still incomplete.

Resolution paths:

- Complete.
- Open task detail.
- Dismiss platform notification where Android allows.
- Change task/reminder settings later.

Acceptance checks:

- A Normal reminder has no implicit re-alert loop.
- Dismissing a Normal reminder is not treated as task completion.
- Completing the task before the reminder fires makes later delivery stale and harmless.

## Scenario 2 - Persistent reminder

Example: `Take medication at 20:00` with re-alert every 10 minutes until resolved, capped by policy.

User intent: the user wants the app to keep reminding them until they take an explicit action.

Expected behaviour:

- The first reminder is scheduled as a persisted occurrence.
- If unresolved, each re-alert is a new persisted one-shot occurrence, not an in-memory loop.
- Re-alerts stop immediately when the task is completed, snoozed according to policy, rescheduled, cancelled, or the maximum horizon is reached.
- Duplicate delivery/action events are idempotent.
- If the app process dies, the next valid scheduled occurrence can still be delivered or reconstructed where Android permits.

Resolution paths:

- Complete.
- Snooze using allowed presets.
- Reschedule, if policy allows.
- Open task detail.
- Disable/change the reminder contract.

Acceptance checks:

- Persistent mode does not require the app process to stay alive.
- A stale re-alert from an old generation is rejected after completion or reschedule.
- The user can see or infer why re-alerting will continue and how to stop it.

## Scenario 3 - Strict reminder

Example: `Leave for examination hall at 07:30` with limited snooze and stronger interruption.

User intent: the user explicitly chooses a stronger contract because missing the task has high personal cost.

Expected behaviour:

- Strict mode is opt-in and explained before activation.
- Strict mode uses stronger sound, vibration, UI emphasis, or reduced snooze choices only within platform limits.
- Strict mode still provides clear resolution actions and does not trap the user.
- Snooze and reschedule options are constrained by policy rather than hidden.
- If an alarm-grade surface is unavailable or not eligible, DayLoop uses a fallback and explains the limitation.

Resolution paths:

- Complete.
- Snooze from a constrained preset list, if allowed.
- Reschedule only if the configured policy permits it.
- Open task detail/settings.
- Use OS-level notification/permission controls where Android allows.

Acceptance checks:

- Strict mode is never silently enabled by priority alone.
- Strict mode cannot remove Android's user-controlled dismissal, permission, or settings paths.
- Strict mode fallback remains honest when privileged alarm UI is unavailable.

## Scenario 4 - Focus reminder

Example: `Study circuits 09:00-11:00` with start, midway, and end prompts.

User intent: the user wants support for staying with a time block rather than a single due-time nudge.

Expected behaviour:

- The reminder contract is tied to the time-block's start/end period.
- DayLoop may show a start prompt, optional progress check, and end/unfinished prompt.
- Focus mode does not automatically imply Strict mode.
- Completing the task during the block cancels future focus prompts for that occurrence.
- If the block is rescheduled, old prompts become stale.

Resolution paths:

- Start/open task.
- Mark complete.
- Snooze or defer a prompt if policy allows.
- Reschedule the block if policy allows.
- Mark unfinished or leave unresolved for later handling, depending on later product decision.

Acceptance checks:

- Focus mode requires explicit time-block semantics; it is not applied to a bare deadline without a start time.
- A completed focus task does not receive its old end prompt.
- Rescheduling from 09:00-11:00 to 10:00-12:00 invalidates 09:00-era prompts.

## Scenario 5 - Alarm-grade user-selected reminder

Example: `Wake for clinic appointment at 06:00`, configured by the user as alarm-like.

User intent: the user wants the strongest permitted delivery path for a genuinely alarm-like personal commitment.

Expected behaviour:

- Alarm-grade mode is reserved for user-selected use cases that satisfy the app's eventual Android and Play policy decisions.
- DayLoop verifies platform capability before promising exact or full-screen behaviour.
- If exact alarm or full-screen capability is unavailable, DayLoop uses a documented fallback and explains what changed.
- Alarm-grade mode still routes through persisted task/reminder state and stale-event checks.
- AI cannot directly create or activate alarm-grade delivery without validated user approval.

Resolution paths:

- Complete.
- Snooze if the policy allows it.
- Reschedule if the policy allows it.
- Open task detail/settings.
- Use OS-level controls where Android requires them.

Acceptance checks:

- Alarm-grade mode is not used merely to make an ordinary reminder louder.
- The app never claims full-screen or exact delivery before platform/policy eligibility is verified.
- Fallback behaviour remains useful and visible when privileged delivery is unavailable.

## Cross-contract rules

- Reminder contract is separate from task timing type.
- Priority is separate from reminder strictness.
- Acknowledgement is not automatically completion.
- Completion must cancel obsolete future reminder/escalation occurrences.
- Snooze is explicit persisted state, not just notification dismissal.
- Reschedule changes source-of-truth state before future scheduling is reconciled.
- Every notification/alarm action must be idempotent.
- Stale scheduled events are expected and must be harmless.
- Platform permission denial is normal product state, not an exception.
- AI may suggest reminder changes only through structured, validated, user-approved proposals in later phases.

## Product decisions left for later Phase 0 tasks

- Does acknowledgement permanently stop Persistent mode, or only silence the current alert?
- What is the MVP default maximum re-alert horizon?
- Which snooze presets exist for Normal, Persistent, Strict, and alarm-grade modes?
- Which task/reminder combinations qualify for exact alarm delivery?
- Which task/reminder combinations qualify for alarm-style or full-screen UI?
- What fallback copy and diagnostics should be shown when notification/exact-alarm/full-screen capabilities are denied?
- Which behaviours are constrained by minSdk, targetSdk, and current Google Play policy?

## Phase 0 verification

P0-03 is verified when:

- Normal, Persistent, Strict, Focus, and alarm-grade reminder scenarios exist;
- each scenario states user intent, expected behaviour, resolution paths, and acceptance checks;
- cross-contract rules are explicit;
- unresolved policy questions are listed for later Phase 0 tasks;
- `TASKS.md` marks P0-03 complete;
- `STATE.md` points to P0-04 as the next single action.
