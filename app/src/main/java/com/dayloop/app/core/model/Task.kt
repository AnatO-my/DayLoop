package com.dayloop.app.core.model

import java.util.UUID
import java.time.Instant
import java.time.LocalDate

@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank." }
    }

    companion object {
        fun generate(): TaskId = TaskId(UUID.randomUUID().toString())
    }
}

data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

enum class TaskStatus {
    ACTIVE,
    COMPLETED,
    SKIPPED,
    SOMEDAY,
}

enum class TaskPriority(val rank: Int) {
    LOW(rank = 0),
    NORMAL(rank = 1),
    HIGH(rank = 2);

    fun isHigherThan(other: TaskPriority): Boolean = rank > other.rank
}

data class Task(
    val id: TaskId,
    val title: String,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val schedule: TaskSchedule? = null,
    val reminderPolicy: ReminderPolicy = ReminderPolicy.normal(),
    val projectId: ProjectId? = null,
    val subtasks: List<Subtask> = emptyList(),
    val description: String? = null,
    val provenance: TaskProvenance? = null,
    val completedAt: Instant? = null,
) {
    init {
        require(title.isNotBlank()) { "Task title must not be blank." }
        require(reminderPolicy.mode != ReminderMode.FOCUS || schedule is TaskSchedule.TimeBlock) {
            "Focus reminder policy requires a time block schedule."
        }
    }

    fun markCompleted(at: Instant): Task = if (status == TaskStatus.COMPLETED) this
        else copy(status = TaskStatus.COMPLETED, completedAt = at)

    fun skip(): Task = copy(status = TaskStatus.SKIPPED, completedAt = null)

    fun moveToSomeday(): Task = copy(status = TaskStatus.SOMEDAY, schedule = null,
        completedAt = null, reminderPolicy = untimedReminderPolicy())

    fun keepForToday(date: LocalDate): Task = copy(status = TaskStatus.ACTIVE,
        schedule = TaskSchedule.AllDay(date), completedAt = null, reminderPolicy = untimedReminderPolicy())

    private fun untimedReminderPolicy(): ReminderPolicy =
        if (reminderPolicy.mode == ReminderMode.FOCUS) ReminderPolicy.normal() else reminderPolicy

    fun reopen(): Task = copy(status = TaskStatus.ACTIVE, completedAt = null)

    fun withSchedule(schedule: TaskSchedule): Task = copy(schedule = schedule)

    fun withReminderPolicy(reminderPolicy: ReminderPolicy): Task = copy(reminderPolicy = reminderPolicy)
}

data class TaskProvenance(
    val originType: String, // e.g. "PROJECT_RULE"
    val ruleId: String,
    val version: Long,
)
