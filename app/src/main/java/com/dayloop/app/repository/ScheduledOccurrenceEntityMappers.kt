package com.dayloop.app.repository

import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.alarms.ScheduledOccurrenceStatus
import com.dayloop.app.core.database.ScheduledOccurrenceEntity
import com.dayloop.app.core.model.TaskId
import java.time.Instant

internal fun ScheduledOccurrence.toEntity(): ScheduledOccurrenceEntity = ScheduledOccurrenceEntity(
    id = id.value,
    taskId = taskId.value,
    fireAtEpochMillis = fireAt.toEpochMilli(),
    generation = generation,
    status = status.name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

internal fun ScheduledOccurrenceEntity.toDomain(): ScheduledOccurrence = ScheduledOccurrence(
    id = ScheduledOccurrenceId(id),
    taskId = TaskId(taskId),
    fireAt = Instant.ofEpochMilli(fireAtEpochMillis),
    generation = generation,
    status = ScheduledOccurrenceStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)