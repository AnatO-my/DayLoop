# DayLoop Feature Specification

## 1. Today-first homepage

### Required
- Default launch destination is Today.
- Show unresolved tasks before completed tasks.
- Sections: Overdue, Now, Next, Anytime Today, Completed.
- Completed section is visually quiet/collapsible.
- Progress indicator shows resolved vs required tasks.
- Day status changes to `COMPLETE` only according to explicit completion rules.

### Optional later
- calendar overlay;
- weather/context cards;
- AI planning suggestion.
- subtle project provenance/context.

Do not place optional cards above unresolved tasks.

---

## 2. Task timing models

### All-day
Task belongs to a date without requiring a clock time.

### Deadline
A latest completion time. May optionally have one or more reminder offsets.

### Time block
A start/end working period. UI should display duration and active-now status.

### Interval
Repeats after a defined duration during eligible windows/days.

Fields may include:
- interval duration;
- active window start/end;
- eligible days;
- start date/end date;
- anchor mode;
- reminder behaviour.

### Calendar recurrence
Daily, weekly, monthly, yearly and custom calendar rules. Add advanced recurrence only after tests exist for date edge cases.

---


## 2A. Projects and Goals — planned

### Goal
A desired outcome. Goals provide context, target dates and optional progress metrics, but do not directly schedule alarms.

### Project
An organised process or set of work. A Project may:

- link ordinary tasks;
- contain a lightweight phase/cycle protocol;
- derive task occurrences for active phases;
- contain intentional Rest phases that generate no Today task.

Example:

```text
Goal: Master analogue electronics
Project: Circuits Spaced Study
Cycle: 3 Study days → 3 Rest days → repeat
```

Rest days are successful protocol states, not incomplete tasks. Projects/Goals should live on a secondary `Plans` surface and remain visually subordinate to Today. See `PROJECTS_AND_GOALS.md`.

## 3. Reminder policies

`ReminderPolicy` should conceptually include:

```text
mode
initialReminder
realertInterval
maximumRealerts / maximumHorizon
snoozeAllowed
snoozePresets
rescheduleAllowed
soundProfile
vibrationProfile
lockScreenPrivacy
requiresAcknowledgement
completionStopsChain
```

### Normal
Conventional reminder behaviour.

### Persistent
Schedules additional re-alerts while unresolved.

### Strict
Persistent plus stronger UX emphasis and constrained snooze/reschedule options selected by the user.

### Focus
Designed around a time block: start notification, optional midway check, end/unfinished prompt.

### Alarm-grade user-selected reminder
Uses alarm/full-screen capabilities only if the task genuinely qualifies and the app has the required platform/policy access.

---

## 4. Completion behaviour

### Completion
- Mark task complete from Today, task detail, notification/alarm action or widget where supported.
- Completion immediately cancels obsolete future reminder/escalation occurrences.
- Completion is idempotent.

### Undo
A short undo may restore the task, but restoration must also reconcile future scheduling safely.

### Daily closure
When no required unresolved tasks remain:

```text
✓ DAY COMPLETE
```

A new task added to today reopens the day.

---

## 5. Snooze

Snooze is explicit state, not a notification-only trick.

- Record snooze-until timestamp.
- Cancel/replace obsolete escalation event.
- Respect task reminder policy.
- Show exact resulting time before user confirms.
- Avoid unlimited snooze for Strict mode unless the user configured it.

---

## 6. Reschedule

- User can move a task to another time/date when policy allows.
- Time-block reschedule preserves or intentionally changes duration.
- Reschedule updates Room first in a safe transaction, then reconciles alarms.
- Old alarm arrivals must be rejected as stale.

---

## 7. Overdue behaviour

- Deadline becomes overdue after due time if incomplete.
- Time block can become `unfinished` after end time; product may distinguish this from deadline overdue.
- All-day task becomes overdue after day boundary if rollover policy says it remains actionable.
- Recurring tasks require an explicit missed-occurrence policy.

Potential rollover modes later:

```text
KEEP_OVERDUE
MOVE_TO_TODAY
MARK_MISSED_AND_CREATE_NEXT
ASK_USER
```

---

## 8. Quick add

Fast path should support a title plus one common scheduling choice without navigating a large form.

Suggested flow:

```text
+ → title → Today / Time / Interval / More
```

Natural language parsing is a later convenience, not required for reliable task creation.

---

## 9. Task detail

Show:
- title;
- schedule semantics in human-readable form;
- next reminder;
- reminder strictness;
- recurrence/interval preview;
- history (later);
- complete/snooze/reschedule controls.

---

## 10. Alarm diagnostics

A reliability-focused app should expose useful diagnostic state.

Suggested screen:

```text
Exact alarm capability: Available / Unavailable
Notification permission: Granted / Denied
Alarm-style full-screen capability: Available / Unavailable
Next scheduled occurrence: ...
Last delivered alarm: ...
Last scheduling error: ...
Last reconciliation: ...
[Repair schedules]
```

Do not expose raw sensitive task data unnecessarily.

---

## 11. Settings

Early settings:
- theme: system/light/dark;
- dynamic colour on/off;
- default reminder policy;
- alarm sound/vibration defaults;
- privacy on lock screen;
- week start;
- day rollover preference (later);
- diagnostics;
- permission/capability status.

---

## 12. Widget

- unresolved count;
- Now/Next;
- a small number of remaining tasks;
- completion action if robust;
- open Today on tap.

The widget mirrors canonical app state.

---

## 13. Planning intelligence — later

Potential features:

### Capacity warning
“You have 4 h 30 min of planned work and 2 h 45 min available.”

### Recovery plan
When a task runs late, propose a reshuffle without changing fixed commitments.

### Priority-aware recommendation
Suggest next task from priority + deadline + estimated duration + available window.

### Natural-language capture
“Read chapter 4 every weekday at 7 for 45 minutes” → structured draft shown for confirmation.

### Behaviour rules
No hidden psychological scoring or opaque manipulation. Explain the reason for schedule suggestions.

---

## 14. Explicit non-goals for MVP

- team/enterprise project management;
- chat;
- Kanban boards;
- documents/wiki;
- cloud account requirement;
- social feed;
- marketplace;
- autonomous AI changing all schedules;
- cross-platform parity.
