package com.dayloop.app.repository

import com.dayloop.app.core.alarms.PersistentReminderEscalation
import com.dayloop.app.core.alarms.PersistentReminderEscalationId
import com.dayloop.app.core.alarms.PersistentReminderEscalationStatus
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.database.PersistentReminderEscalationEntity
import com.dayloop.app.core.model.TaskId
import java.time.Duration
import java.time.Instant

internal fun PersistentReminderEscalation.toEntity(): PersistentReminderEscalationEntity =
    PersistentReminderEscalationEntity(
        id = id.value,
        taskId = taskId.value,
        sourceOccurrenceId = sourceOccurrenceId.value,
        nextRealertAtEpochMillis = nextRealertAt?.toEpochMilli(),
        realertIntervalMillis = realertInterval.toMillis(),
        maximumRealerts = maximumRealerts,
        realertsDelivered = realertsDelivered,
        status = status.name,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
    )

internal fun PersistentReminderEscalationEntity.toDomain(): PersistentReminderEscalation =
    PersistentReminderEscalation(
        id = PersistentReminderEscalationId(id),
        taskId = TaskId(taskId),
        sourceOccurrenceId = ScheduledOccurrenceId(sourceOccurrenceId),
        nextRealertAt = nextRealertAtEpochMillis?.let(Instant::ofEpochMilli),
        realertInterval = Duration.ofMillis(realertIntervalMillis),
        maximumRealerts = maximumRealerts,
        realertsDelivered = realertsDelivered,
        status = PersistentReminderEscalationStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    )
