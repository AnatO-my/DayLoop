package com.dayloop.app.core.alarms

import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.time.TimeProvider

class AlarmDeliveryProcessor(
    private val scheduledOccurrenceRepository: ScheduledOccurrenceRepository,
    private val timeProvider: TimeProvider,
    private val diagnosticsRepository: AlarmDiagnosticsRepository? = null,
) {
    suspend fun process(delivery: AlarmDelivery): AlarmDeliveryResult {
        val persisted = scheduledOccurrenceRepository.getOccurrence(delivery.occurrenceId)
        if (persisted == null) {
            val result = AlarmDeliveryResult.StaleMissing(delivery.occurrenceId)
            record(delivery.occurrenceId, AlarmDiagnosticEvent.DELIVERY_STALE, "MISSING_OCCURRENCE")
            return result
        }

        if (persisted.generation != delivery.generation) {
            val result = AlarmDeliveryResult.StaleGeneration(
                occurrenceId = delivery.occurrenceId,
                expectedGeneration = persisted.generation,
                deliveredGeneration = delivery.generation,
            )
            record(persisted, AlarmDiagnosticEvent.DELIVERY_STALE, "GENERATION_MISMATCH")
            return result
        }

        if (persisted.status != ScheduledOccurrenceStatus.SCHEDULED) {
            val result = AlarmDeliveryResult.StaleStatus(
                occurrenceId = delivery.occurrenceId,
                status = persisted.status,
            )
            record(persisted, AlarmDiagnosticEvent.DELIVERY_STALE, persisted.status.name)
            return result
        }

        val fired = persisted.markFired(timeProvider.now())
        scheduledOccurrenceRepository.saveOccurrence(fired)
        val result = AlarmDeliveryResult.Accepted(fired)
        record(fired, AlarmDiagnosticEvent.DELIVERY_ACCEPTED)
        return result
    }

    private suspend fun record(
        occurrence: ScheduledOccurrence,
        event: AlarmDiagnosticEvent,
        message: String? = null,
    ) {
        record(
            occurrenceId = occurrence.id,
            taskId = occurrence.taskId,
            event = event,
            message = message,
        )
    }

    private suspend fun record(
        occurrenceId: ScheduledOccurrenceId,
        event: AlarmDiagnosticEvent,
        message: String,
    ) {
        record(
            occurrenceId = occurrenceId,
            taskId = null,
            event = event,
            message = message,
        )
    }

    private suspend fun record(
        occurrenceId: ScheduledOccurrenceId,
        taskId: com.dayloop.app.core.model.TaskId?,
        event: AlarmDiagnosticEvent,
        message: String? = null,
    ) {
        val repository = diagnosticsRepository ?: return
        runCatching {
            repository.append(
                AlarmDiagnosticEntry(
                    occurrenceId = occurrenceId,
                    taskId = taskId,
                    event = event,
                    message = message,
                    createdAt = timeProvider.now(),
                ),
            )
        }
    }
}

data class AlarmDelivery(
    val occurrenceId: ScheduledOccurrenceId,
    val generation: Long,
) {
    init {
        require(generation > 0) { "Alarm delivery generation must be greater than 0." }
    }
}

sealed interface AlarmDeliveryResult {
    val occurrenceId: ScheduledOccurrenceId

    data class Accepted(
        val occurrence: ScheduledOccurrence,
    ) : AlarmDeliveryResult {
        override val occurrenceId: ScheduledOccurrenceId = occurrence.id
    }

    data class StaleMissing(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : AlarmDeliveryResult

    data class StaleGeneration(
        override val occurrenceId: ScheduledOccurrenceId,
        val expectedGeneration: Long,
        val deliveredGeneration: Long,
    ) : AlarmDeliveryResult

    data class StaleStatus(
        override val occurrenceId: ScheduledOccurrenceId,
        val status: ScheduledOccurrenceStatus,
    ) : AlarmDeliveryResult
}
