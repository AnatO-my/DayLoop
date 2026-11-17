# DayLoop Proposed Architecture

## 1. Architectural objective

The system must remain reliable when:

- the app process is dead;
- the device is offline;
- Android delays non-exact background work;
- notification/exact-alarm/full-screen capabilities are unavailable;
- the device reboots;
- an old alarm signal arrives after a task has changed.

This favours an offline-first architecture with persistent source-of-truth state and small deterministic scheduling components.

## 2. Layer model

```text
┌──────────────────────────────────────────┐
│                Compose UI                │
│ Today • Plans • Editor • Settings • Diag │
└───────────────────┬──────────────────────┘
                    │
┌───────────────────▼──────────────────────┐
│              Application Use Cases       │
│ GetToday • Complete • Snooze • Reschedule│
│ CreateTask • ReconcileSchedules          │
└───────────────────┬──────────────────────┘
                    │
┌───────────────────▼──────────────────────┐
│                 Domain                   │
│ Task • Project • Goal • Schedules        │
│ ReminderPolicy • Protocol • DayState      │
└───────────────┬───────────────┬──────────┘
                │               │
        ┌───────▼──────┐ ┌──────▼─────────┐
        │ Persistence   │ │ Platform        │
        │ Room/DataStore│ │ AlarmManager    │
        │               │ │ Notifications   │
        └───────────────┘ │ WorkManager      │
                          └──────────────────┘

       Optional later:
┌──────────────────────────────────────────┐
│ Planner Agent → structured proposal only │
└───────────────────┬──────────────────────┘
                    ▼
           deterministic validator
```

## 3. Suggested packages/modules

```text
app
  MainActivity
  DayLoopApplication
  navigation

core:model
  Task
  TaskId
  TaskStatus
  TaskSchedule
  ReminderPolicy
  RecurrenceRule
  ScheduledOccurrence
  Project
  Goal
  ProjectProtocol
  DayState

core:time
  AppClock
  TimeZoneProvider
  DateBoundaryCalculator

core:database
  DayLoopDatabase
  TaskEntity
  OccurrenceEntity
  CompletionEntity
  AlarmLogEntity
  NoteEntity                   # Added in Note Engine Upgrade
  GoalEntity
  ProjectEntity
  ClothingItemEntity
  OutfitEntity
  DAOs
  mappers

core:calendar                  # Added in Calendar Integration Upgrade
  CalendarContractManager
  CalendarEvent
  CalendarSyncRepository

core:notes                     # Added in Note Engine Upgrade
  Note
  NoteRepository
  NoteDao

core:recurrence
  NextOccurrenceCalculator
  IntervalCalculator
  CalendarRecurrenceCalculator

core:projects                 # introduced later
  ProjectProtocolEvaluator
  ProjectOccurrenceGenerator
  ProjectProgressCalculator

core:alarms
  AlarmScheduler
  AndroidAlarmScheduler
  AlarmReceiver
  BootReceiver
  ScheduleReconciler

core:notifications
  NotificationChannels
  ReminderNotificationFactory
  AlarmActivity
  NotificationActionReceiver

core:design
  theme
  typography
  shape
  urgency tokens

feature:today
  TodayViewModel
  GetTodaySnapshot
  TodayScreen

feature:taskeditor
  TaskEditorViewModel
  TaskEditorScreen

feature:plans                 # introduced later
  PlansScreen
  ProjectDetailScreen
  GoalDetailScreen

feature:settings
  SettingsRepository
  SettingsScreen

feature:diagnostics
  AlarmDiagnosticsScreen

widget
  TodayWidget

agent (late phase)
  PlanningAgent
  PlanningProposal
  PlanningProposalValidator
```

## 4. Domain model sketch

```kotlin
data class Task(
    val id: TaskId,
    val title: String,
    val notes: String?,
    val status: TaskStatus,
    val schedule: TaskSchedule,
    val reminderPolicy: ReminderPolicy,
    val priority: Priority,
    val createdAt: Instant,
    val updatedAt: Instant
)

sealed interface TaskSchedule {
    data class AllDay(val date: LocalDate) : TaskSchedule
    data class Deadline(val dueAt: ZonedDateTime) : TaskSchedule
    data class TimeBlock(
        val startAt: ZonedDateTime,
        val endAt: ZonedDateTime
    ) : TaskSchedule
    data class Interval(
        val interval: Duration,
        val activeWindow: LocalTimeRange,
        val eligibleDays: Set<DayOfWeek>,
        val anchorMode: IntervalAnchorMode
    ) : TaskSchedule
    data class Recurring(
        val rule: RecurrenceRule,
        val localTime: LocalTime?,
        val zoneId: ZoneId
    ) : TaskSchedule
}
```

The exact Kotlin types may change after a time-model ADR. The key requirement is semantic separation.


## 4A. Task → Project → Goal boundary

```text
Goal
  ↓ outcome/context
Project
  ↓ organised process or phase protocol
Task / derived occurrence
  ↓ executable work
Scheduler
  ↓
Alarm delivery
```

Projects and Goals are not alternative alarm engines. A Project may derive a task occurrence from a rule such as `3 active days → 3 rest days → repeat`, but that occurrence enters the normal Task/Scheduler path. Intentional Rest phases create no required Today task.

Keep Project/Goal UI on a secondary `Plans` surface. Today may show a small project provenance label on a task, but should not become a nested project dashboard. See `PROJECTS_AND_GOALS.md` and proposed ADR 0003.

## 5. Scheduled occurrence model

Persist alarms as derived records so platform scheduling can be reconciled:

```text
ScheduledOccurrence
- id
- taskId
- intendedAt
- occurrenceType
- generation/version
- status
- scheduledAt
- deliveredAt?
- resolvedAt?
- parentOccurrenceId?
```

A generation/version helps identify alarms that became stale after task edits.

## 6. Alarm scheduling strategy

### Exact alarms
Use only for genuinely user-facing, precise alarm/reminder behaviour where Android policy permits it.

### Inexact/deferrable work
Use WorkManager for:

- cleanup;
- non-urgent reconciliation;
- telemetry/diagnostic upload if ever added;
- sync work;
- database maintenance.

Do not use WorkManager to promise an exact 08:00 alarm.

### Re-alert chain
Schedule each escalation as a new one-shot occurrence. This survives process death and avoids a long-lived service loop.

## 7. Reconciliation

`ScheduleReconciler` compares desired future occurrences from Room against what should be registered with the OS.

Run reconciliation after:

- task create/update/delete;
- completion/reschedule;
- boot;
- app upgrade;
- relevant permission/capability changes;
- timezone/time changes where appropriate;
- manual repair action.

The reconciler should be safe to run repeatedly.

## 8. Today projection

The Today screen should observe a projection/use case rather than query tables directly.

Suggested output:

```kotlin
data class TodaySnapshot(
    val date: LocalDate,
    val overdue: List<TaskPresentation>,
    val now: List<TaskPresentation>,
    val next: List<TaskPresentation>,
    val anytime: List<TaskPresentation>,
    val completed: List<TaskPresentation>,
    val completedCount: Int,
    val totalRequiredCount: Int,
    val state: DayState
)
```

## 9. UI state

Use unidirectional data flow:

```text
Room/Repositories → Use Cases → ViewModel StateFlow → Compose
Compose event → ViewModel → Use Case → Repository → state updates
```

Do not mutate UI lists optimistically without a clear reconciliation strategy for alarm cancellation/completion side effects.

## 10. Theme architecture — Calm Pressure

Build on Material 3 tokens but add semantic urgency tokens:

```text
Normal
Upcoming
Active
Overdue
Persistent
Strict
Completed
```

Tokens may control container emphasis, iconography, motion and typography. Avoid using colour alone to communicate urgency.

Material 3 Expressive can be selectively used for larger shapes, motion and emphasis, but consistency and comfort outrank novelty.

## 11. Optional agent architecture

The agent gets a read-only planning context and returns proposals:

```text
PlanningContext
- now
- TodaySnapshot
- immutable constraints
- user preferences
- optional calendar busy blocks

PlanningProposal
- rationale
- list of typed proposed changes
- confidence/uncertainty metadata
```

`PlanningProposalValidator` checks:

- valid time ranges;
- no impossible duration;
- user-fixed tasks not moved;
- no changes outside allowed horizon;
- recurrence semantics preserved;
- task IDs exist;
- proposal is still based on current generation/version.

Only after validation and confirmation does normal deterministic application code apply changes and invoke `ScheduleReconciler`.
