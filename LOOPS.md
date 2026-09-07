# DayLoop Loops and State Machines

The word “loop” is used in two different ways in this project:

1. **runtime loops** — repeated task/reminder behaviour implemented as persisted state + scheduled one-shot events;
2. **agent development loops** — iterative plan → implement → test → review cycles used by coding agents.

Do not implement an immortal in-memory loop for alarm persistence. Android may kill the process at any time.

---

## 1. Reminder Delivery Loop

Conceptual behaviour:

```text
Task/reminder saved
      ↓
Compute next occurrence
      ↓
Persist occurrence
      ↓
Schedule platform alarm
      ↓
Android delivers alarm signal
      ↓
Reload current task + occurrence from DB
      ↓
Still valid and unresolved?
   ┌──┴───┐
  no     yes
   │       ↓
 stop   show notification/alarm UI
           ↓
       user resolves?
       ┌───┴────┐
      yes      no
       │        ↓
 cancel     policy allows re-alert?
 future       ┌──┴───┐
 events      no     yes
              │       ↓
             stop  compute next escalation
                       ↓
                    persist
                       ↓
                    schedule one-shot alarm
```

Pseudo-Kotlin domain logic:

```kotlin
suspend fun onAlarmSignal(occurrenceId: OccurrenceId) {
    val occurrence = occurrenceRepository.get(occurrenceId) ?: return
    val task = taskRepository.get(occurrence.taskId) ?: return

    if (!occurrence.isCurrent || task.isCompleted) {
        alarmScheduler.cancel(occurrenceId)
        return
    }

    reminderPresenter.present(task, occurrence)
    occurrenceRepository.markDelivered(occurrenceId, clock.now())

    val next = escalationPolicy.nextOccurrence(task, occurrence, clock.now())
    if (next != null) {
        occurrenceRepository.save(next)
        alarmScheduler.schedule(next)
    }
}
```

The receiver may hand work to a short-lived component, but it must not assume the process remains alive.

---

## 2. Persistent Completion Loop

A persistent reminder is a state machine, not a repeated notification timer.

### States

```text
PENDING
SCHEDULED
ALERTING
ACKNOWLEDGED
SNOOZED
RESCHEDULED
COMPLETED
EXPIRED
CANCELLED
```

### Typical transitions

```text
PENDING → SCHEDULED
SCHEDULED → ALERTING
ALERTING → COMPLETED
ALERTING → SNOOZED → SCHEDULED
ALERTING → RESCHEDULED → SCHEDULED
ALERTING → ACKNOWLEDGED
ALERTING → SCHEDULED         # escalation/re-alert
SCHEDULED → CANCELLED        # task deleted/disabled
SCHEDULED → EXPIRED          # policy horizon exceeded
```

Every transition should be idempotent where possible.

### Important distinction

`ACKNOWLEDGED` does not necessarily equal `COMPLETED`.

A product decision is required for each reminder contract:

- Does acknowledgement silence the chain permanently?
- Does it only silence the current alert?
- Is completion required to close the task?

Store this in policy rather than scattering assumptions through UI code.

---

## 3. Interval Task Loop

Example: every 50 minutes between 08:00 and 20:00 on weekdays.

```text
Create/enable interval rule
      ↓
Compute next eligible timestamp
      ↓
Schedule one-shot event
      ↓
Deliver
      ↓
Record occurrence
      ↓
Compute next eligible timestamp
      ↓
Schedule next one-shot event
```

Benefits:

- no always-on service;
- no drift caused by a long-running timer;
- next event can respect day/time windows;
- easy to reconstruct from persistent rule after reboot.

The next occurrence should be computed from policy and intended schedule, not blindly from `now + interval`, unless the product explicitly defines “interval since acknowledgement”. These are different behaviours.

Support both semantics only if made explicit:

```text
CLOCK-ANCHORED: 08:00, 08:50, 09:40 ...
ACK-ANCHORED: next = acknowledgement time + 50 min
```

---

## 4. Recurring Task Loop

A recurring task is calendar-driven.

```text
Series rule
  ↓
Materialise/derive occurrence
  ↓
Task is completed or missed
  ↓
Advance to next recurrence according to rule
```

Do not confuse recurrence with an elapsed interval. “Every Monday at 09:00” must remain Monday at 09:00 even if the prior occurrence was completed late.

---

## 5. Daily State Loop

The Today model is recalculated from source-of-truth state and the current clock.

```text
start of day / DB change / clock boundary
        ↓
query relevant tasks
        ↓
classify overdue / now / next / anytime / completed
        ↓
calculate progress
        ↓
remaining required tasks == 0 ?
     ┌─────────┴────────┐
    yes                no
     ↓                  ↓
DAY COMPLETE          DAY OPEN
```

At midnight, completion history remains history. The new day gets a new snapshot; recurring tasks generate/derive the next applicable occurrence.

---


## 5A. Project Protocol Loop

A phase-based Project is evaluated deterministically. Example: three Study days followed by three Rest days.

```text
Project protocol + anchor date
        ↓
Evaluate phase for current date
        ↓
   ACTIVE phase?
   ┌────┴────┐
  yes       no (REST)
   │          ↓
 derive     generate no required task
 task         ↓
 occurrence project remains ACTIVE
   ↓
normal Task scheduler/reminder loop
```

The Project engine must never directly schedule Android alarms. Generated occurrences include project/rule provenance and generation metadata so edits can invalidate stale derived work.

When a protocol is edited, apply an explicit anchor policy rather than silently guessing whether the new cycle restarts or continues.

## 6. Permission Recovery Loop

```text
Need privileged alarm behaviour
       ↓
Capability available?
   ┌───┴────┐
  yes      no
   │        ↓
 schedule  use documented fallback
           + explain limitation
           + offer settings path when appropriate
```

When capability changes:

```text
permission/capability change signal
      ↓
re-read current state
      ↓
reconcile scheduled occurrences
      ↓
repair/cancel/re-register as required
```

Never repeatedly nag a user into granting a privileged permission.

---

## 7. Optional Planning Agent Loop

The planning agent is advisory:

```text
OBSERVE
TodaySnapshot + calendar constraints + preferences
      ↓
ASSESS
conflicts, overdue work, insufficient free time
      ↓
PROPOSE
structured schedule changes
      ↓
VALIDATE
pure deterministic domain validator
      ↓
CONFIRM
user accepts/rejects/edits
      ↓
APPLY
repository transaction
      ↓
RESCHEDULE
alarm scheduler reconciles occurrences
      ↓
OBSERVE AGAIN LATER
```

Suggested structured response:

```json
{
  "reason": "Two tasks overlap and the day is over capacity by 45 minutes.",
  "proposals": [
    {
      "taskId": "...",
      "action": "MOVE_TIME_BLOCK",
      "newStart": "...",
      "newEnd": "..."
    }
  ]
}
```

Never parse free-form prose directly into irreversible schedule mutations.

---

## 8. Coding-Agent Development Loop

Every coding agent follows the Guided Build loop:

```text
READ STATE + COMPACT PHASE BRIEF + CONTEXT MANIFEST
   ↓
READ LEARNED_CONCEPTS + ONLY DECLARED DEPENDENCIES
   ↓
SELECT ONE TASK
   ↓
INSPECT ONLY RELEVANT EXISTING CODE
   ↓
TEACH COMPONENT
 purpose • architecture • Kotlin/Android concepts
 inputs/outputs • lifecycle • failure cases • tests
   ↓
PREVIEW DESIGN + EXACT FILE CHANGES
   ↓
IMPLEMENT SMALLEST CHANGE
   ↓
RUN NARROW TESTS
   ↓
FAIL? ──yes──→ diagnose → explain → change → retest
   ↓ no
RUN RELEVANT SUITE / BUILD
   ↓
SELF-REVIEW
   ↓
EXPLAIN WRITTEN CODE + LEARNING CHECKPOINT
   ↓
UPDATE STATE + TASKS + LEARNED_CONCEPTS
   ↓
COMPACT COMPLETION PACKET
   ↓
STOP
```

`BUILD_LEARNING_PROTOCOL.md` defines the teaching gate; `TOKEN_EFFICIENCY.md` defines minimum context and compact output. Use `LEARNED_CONCEPTS.md` to avoid repeating deep teaching. An explicit user request for fast generation may bypass the lesson only for that scoped task.

### Stop conditions
The agent stops when one scoped task is complete, blocked by an external requirement, or would require an architectural decision not yet authorised.

### Never-do behaviour
- Do not silently broaden scope.
- Do not skip tests because the change is “small”.
- Do not solve build errors by deleting tests or reducing type safety.
- Do not replace deterministic scheduling with an AI call.
- Do not read the whole repository or echo complete generated files without a concrete need.
- Do not automatically continue into the next task/phase after verification.
