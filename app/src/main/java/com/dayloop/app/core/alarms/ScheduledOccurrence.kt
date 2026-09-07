package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import java.time.Instant
import java.util.UUID

@JvmInline
value class ScheduledOccurrenceId(val value: String) {
    init {
        require(value.isNotBlank()) { "ScheduledOccurrenceId must not be blank." }
    }

    companion object {
        fun generate(): ScheduledOccurrenceId = ScheduledOccurrenceId(UUID.randomUUID().toString())
    }
}

enum class ScheduledOccurrenceStatus {
    SCHEDULED,
    FIRED,
    CANCELLED,
}

data class ScheduledOccurrence(
    val id: ScheduledOccurrenceId,
    val taskId: TaskId,
    val fireAt: Instant,
    val generation: Long = 1L,
    val status: ScheduledOccurrenceStatus = ScheduledOccurrenceStatus.SCHEDULED,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
) {
    init {
        require(generation > 0) { "Scheduled occurrence generation must be greater than 0." }
        require(!updatedAt.isBefore(createdAt)) { "Scheduled occurrence updatedAt cannot be before createdAt." }
    }

    fun markFired(at: Instant): ScheduledOccurrence = copy(
        status = ScheduledOccurrenceStatus.FIRED,
        updatedAt = at,
    )

    fun markCancelled(at: Instant): ScheduledOccurrence = copy(
        status = ScheduledOccurrenceStatus.CANCELLED,
        updatedAt = at,
    )
}