package com.dayloop.app.core.model

import java.util.UUID

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
    val provenance: TaskProvenance? = null,
) {
    init {
        require(title.isNotBlank()) { "Task title must not be blank." }
        require(reminderPolicy.mode != ReminderMode.FOCUS || schedule is TaskSchedule.TimeBlock) {
            "Focus reminder policy requires a time block schedule."
        }
    }

    fun markCompleted(): Task = copy(status = TaskStatus.COMPLETED)

    fun withSchedule(schedule: TaskSchedule): Task = copy(schedule = schedule)

    fun withReminderPolicy(reminderPolicy: ReminderPolicy): Task = copy(reminderPolicy = reminderPolicy)
}

data class TaskProvenance(
    val originType: String, // e.g. "PROJECT_RULE"
    val ruleId: String,
    val version: Long,
)
