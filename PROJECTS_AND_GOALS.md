# Projects and Goals — Product and Domain Design

DayLoop remains a **Today-first execution app**, but not every useful commitment should be modelled as an isolated task.

This document introduces two higher-level concepts:

```text
GOAL
  ↓ why / desired outcome
PROJECT
  ↓ organised path / behavioural protocol
TASK / OCCURRENCE
  ↓ concrete action shown on Today
COMPLETION RECORD
```

The hierarchy should remain deliberately lightweight. DayLoop must not become a project-management suite.

---

## 1. Task

A **Task** is an executable unit that may appear on Today.

Examples:

- Study circuits for 60 minutes.
- Submit laboratory report by 17:00.
- Read pages 40–60.
- Drink water.

Tasks answer:

> What do I need to do?

---

## 2. Project

A **Project** coordinates multiple tasks, phases, rules, or repeated behaviours toward a coherent activity.

Projects answer:

> What organised process am I currently carrying out?

Examples:

- Prepare for EEE 203 examination.
- Build DayLoop MVP.
- Complete literature review.
- Six-week fitness programme.
- Three-days-study / three-days-rest memory cycle.

A Project can be intentionally less prominent than Today. It should generate or organise executable work without occupying the home screen with management clutter.

### Suggested UI placement

```text
Today       ← default launch
Plans       ← secondary surface
  Projects
  Goals
History
Settings
```

`Plans` may eventually be represented by one navigation destination containing both Projects and Goals rather than two permanent bottom-navigation tabs.

---

## 3. Goal

A **Goal** describes a desired outcome, not an alarm schedule.

Examples:

- Master analogue electronics before the examination.
- Read 24 research papers this semester.
- Complete DayLoop MVP.
- Build a consistent morning study habit.

Goals answer:

> What outcome am I trying to reach?

A Goal may contain:

- zero or more Projects;
- directly linked Tasks;
- target date;
- optional measurable target;
- notes/motivation;
- progress policy.

Goals should not automatically create alarms merely because they exist.

---

# 4. Why Projects are needed

Without Projects, a behavioural protocol can become an ugly pile of individually scheduled tasks.

Suppose the user wants:

```text
Study for 3 days
Rest from that subject for 3 days
Repeat
```

A naive task manager requires manually entering:

```text
Mon  Study
Tue  Study
Wed  Study
Thu  no study
Fri  no study
Sat  no study
Sun  Study
...
```

The rest days are not tasks and should not appear as failures or incomplete obligations.

DayLoop should instead allow a Project to own a **cycle rule**.

---

## 5. Project cycle / protocol model

Conceptually:

```text
Project: Circuits Spaced Study
Goal: Master circuit analysis

Cycle:
  Phase A — STUDY
    duration: 3 days
    daily action: Study circuits
    target duration: 60 min/day
    reminder: Persistent at 19:00

  Phase B — REST_FROM_PROJECT
    duration: 3 days
    creates no task

Repeat until:
  2026-10-20
```

This is not the same as a normal calendar recurrence. The **phase state matters**.

### Possible phase types

```text
ACTIVE_TASK_PHASE
REST_PHASE
CHECKPOINT_PHASE
CUSTOM_PHASE        # later
```

For MVP of Projects, only Active and Rest may be necessary.

---

## 6. Important semantic distinction

These concepts must stay separate:

### Recurring Task
“Study every Monday, Wednesday and Friday.”

Calendar rule. Each occurrence exists independently.

### Interval Task
“Take a break every 50 minutes between 08:00 and 18:00.”

Elapsed/window rule.

### Project Cycle
“Study for 3 days, then deliberately avoid this study for 3 days, then repeat.”

Phase-based behavioural protocol. A Rest phase is meaningful state, not missing work.

---

## 7. Generated vs linked tasks

A project may contain two kinds of tasks.

### Linked task
A normal task manually attached to the Project.

Example:

```text
Project: DayLoop MVP
Task: Implement AlarmScheduler interface
```

### Generated occurrence/task
Created or derived by a Project rule.

Example:

```text
Project cycle says today is Study Day 2
→ derive "Study circuits for 60 min"
```

The implementation should prefer **derived/generated occurrences with provenance** rather than creating endless future rows unnecessarily.

Store enough metadata to know:

```text
originType = PROJECT_RULE
projectId
projectRuleId
generation/version
```

---

## 8. Project state

Suggested states:

```text
DRAFT
ACTIVE
PAUSED
COMPLETED
ARCHIVED
```

`PAUSED` means the project itself is suspended by the user. This differs from an intentional `REST_PHASE` inside an active project cycle.

---

## 9. Goal progress

Do not force all goals into one progress model.

Potential progress policies:

### Binary
Complete / not complete.

### Count
`12 / 24 papers read`.

### Project-derived
Progress derived from completed project milestones.

### Manual
User periodically updates progress.

### Time-based
Possible later, but should not reward meaningless logged hours.

Avoid fake precision such as claiming a learning goal is “67% mastered” without a defensible metric.

---

## 10. Today integration

Today should not become hierarchical clutter.

A task produced by a Project can show a small provenance label:

```text
Study circuits · 60 min
Circuits Spaced Study
```

The Project itself does not need a giant card above today's tasks.

If today is a Rest phase:

- no study task is created;
- the project may show a subtle `Rest day` status only inside Plans/Project detail;
- Day progress is not penalised;
- strict reminders do not fire for the absent task.

---

## 11. Project detail screen

Possible information hierarchy:

```text
Circuits Spaced Study
Goal: Master analogue electronics
Status: ACTIVE
Current phase: REST · Day 2 of 3
Next active day: Sunday

Cycle
[ Study × 3 days ] → [ Rest × 3 days ] → repeat

Upcoming generated actions
...

Linked tasks
...

History
...
```

Keep project editing progressively disclosed.

---

## 12. Goal detail screen

Possible hierarchy:

```text
Master analogue electronics
Target: 20 October
Progress: 8 / 12 planned topics

Projects
- Circuits Spaced Study
- Past Questions Revision

Direct tasks
- Collect lecturer's problem set

Notes / motivation
```

A goal is context. It is not another Today list.

---

## 13. Domain sketch

Illustrative only:

```kotlin
data class Goal(
    val id: GoalId,
    val title: String,
    val targetDate: LocalDate?,
    val progressPolicy: GoalProgressPolicy,
    val status: GoalStatus
)

data class Project(
    val id: ProjectId,
    val goalId: GoalId?,
    val title: String,
    val status: ProjectStatus,
    val protocol: ProjectProtocol?
)

sealed interface ProjectProtocol {
    data class PhaseCycle(
        val phases: List<ProjectPhase>,
        val anchorDate: LocalDate,
        val endCondition: ProjectEndCondition?
    ) : ProjectProtocol
}

sealed interface ProjectPhase {
    data class ActiveTaskPhase(
        val durationDays: Int,
        val taskTemplate: TaskTemplate
    ) : ProjectPhase

    data class RestPhase(
        val durationDays: Int
    ) : ProjectPhase
}
```

Do not commit to these exact Kotlin types until the corresponding phase is being taught and implemented.

---

## 14. Scheduling rule

The Project engine does **not** schedule Android alarms directly.

```text
Project protocol
      ↓
Project occurrence generator
      ↓
Task/Occurrence domain model
      ↓
normal Scheduler
      ↓
AlarmManager adapter
```

This preserves the deterministic boundary: all alarm behaviour still passes through the same validated scheduling engine.

---

## 15. AI relationship

AI may later help create a project protocol from natural language:

> “For the next six weeks, let me study electronics for three days and leave it alone for three days.”

The agent may return a structured **draft**:

```text
Active phase: 3 days
Rest phase: 3 days
Repeat: yes
End date: ...
```

The user reviews it. The deterministic ProjectProtocol validator decides whether it is valid.

AI does not operate the cycle clock directly.

---

## 16. Initial scope recommendation

Projects and Goals should be introduced after core task semantics and scheduling are reliable.

First useful Project feature:

1. create a Project;
2. link normal tasks to it;
3. optionally define one Active/Rest phase cycle;
4. derive today's generated task;
5. show project provenance on Today;
6. expose project state in a secondary Plans screen.

First useful Goal feature:

1. create Goal;
2. optional target date;
3. attach Projects;
4. choose simple Binary/Count/Manual progress;
5. keep Goals off the Today surface unless a linked task is due.

This provides the power of long-term organisation without turning DayLoop into Jira, Notion, or a conventional project dashboard.
