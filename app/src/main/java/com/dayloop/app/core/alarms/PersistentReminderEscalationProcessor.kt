package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.TimeProvider

class PersistentReminderEscalationProcessor(
    private val taskRepository: TaskRepository,
    private val scheduledOccurrenceRepository: ScheduledOccurrenceRepository,
    private val escalationRepository: PersistentReminderEscalationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val timeProvider: TimeProvider,
) {
    suspend fun processAcceptedDelivery(delivered: ScheduledOccurrence): PersistentReminderEscalationResult {
        val occurrence = scheduledOccurrenceRepository.getOccurrence(delivered.id)
        if (occurrence == null || occurrence.generation != delivered.generation ||
            occurrence.taskId != delivered.taskId || occurrence.status != ScheduledOccurrenceStatus.FIRED ||
            delivered.status != ScheduledOccurrenceStatus.FIRED
        ) {
            return PersistentReminderEscalationResult.Stale(delivered.id)
        }
        val task = taskRepository.getTask(occurrence.taskId)
        if (task == null) {
            resolveActiveEscalationsForOccurrence(occurrence)
            return PersistentReminderEscalationResult.TaskMissing(occurrence.id)
        }

        if (task.status != TaskStatus.ACTIVE || !task.reminderPolicy.mode.supportsRealerts) {
            resolveActiveEscalationsForOccurrence(occurrence)
            return PersistentReminderEscalationResult.NotEligible(occurrence.id)
        }

        val activeEscalation = escalationRepository.getEscalationsForTask(occurrence.taskId)
            .firstOrNull { it.sourceOccurrenceId == occurrence.id }
        if (activeEscalation != null && !activeEscalation.isActive) {
            return PersistentReminderEscalationResult.Inactive(occurrence.id)
        }
        if (activeEscalation != null && activeEscalation.nextRealertAt != occurrence.fireAt) {
            return PersistentReminderEscalationResult.Stale(occurrence.id)
        }
        val nextEscalation = if (activeEscalation == null) {
            PersistentReminderEscalation.start(
                policy = task.reminderPolicy,
                taskId = task.id,
                sourceOccurrenceId = occurrence.id,
                deliveredAt = timeProvider.now(),
            )
        } else {
            activeEscalation.recordRealertDelivered(timeProvider.now())
        }
        if (!nextEscalation.isActive) {
            if (!escalationRepository.advanceAfterDelivery(occurrence, activeEscalation, nextEscalation, null)) {
                return PersistentReminderEscalationResult.Stale(occurrence.id)
            }
            return PersistentReminderEscalationResult.Capped(nextEscalation)
        }

        return scheduleNextRealert(occurrence, activeEscalation, nextEscalation, task)
    }

    private suspend fun scheduleNextRealert(
        occurrence: ScheduledOccurrence,
        previous: PersistentReminderEscalation?,
        escalation: PersistentReminderEscalation,
        task: Task,
    ): PersistentReminderEscalationResult {
        val now = timeProvider.now()
        val scheduled = occurrence.copy(
            taskId = task.id,
            fireAt = requireNotNull(escalation.nextRealertAt),
            generation = occurrence.generation + 1,
            status = ScheduledOccurrenceStatus.SCHEDULED,
            updatedAt = now,
        )

        if (!escalationRepository.advanceAfterDelivery(occurrence, previous, escalation, scheduled)) {
            return PersistentReminderEscalationResult.Stale(occurrence.id)
        }
        return when (val result = alarmScheduler.schedule(scheduled)) {
            is AlarmScheduleResult.Scheduled -> {
                PersistentReminderEscalationResult.Scheduled(
                    escalation = escalation,
                    occurrence = scheduled,
                    scheduledExact = result.exact,
                )
            }
            is AlarmScheduleResult.Rejected -> PersistentReminderEscalationResult.ScheduleRejected(
                escalation = escalation,
                occurrenceId = scheduled.id,
                reason = result.reason,
            )
        }
    }

    private suspend fun resolveActiveEscalationsForOccurrence(occurrence: ScheduledOccurrence) {
        val now = timeProvider.now()
        escalationRepository.getActiveEscalations()
            .filter { escalation ->
                escalation.sourceOccurrenceId == occurrence.id || escalation.taskId == occurrence.taskId
            }
            .forEach { escalation ->
                escalationRepository.saveEscalation(escalation.resolve(now))
            }
    }

}

sealed interface PersistentReminderEscalationResult {
    val occurrenceId: ScheduledOccurrenceId

    data class Stale(override val occurrenceId: ScheduledOccurrenceId) : PersistentReminderEscalationResult

    data class Inactive(override val occurrenceId: ScheduledOccurrenceId) : PersistentReminderEscalationResult

    data class Scheduled(
        val escalation: PersistentReminderEscalation,
        val occurrence: ScheduledOccurrence,
        val scheduledExact: Boolean,
    ) : PersistentReminderEscalationResult {
        override val occurrenceId: ScheduledOccurrenceId = occurrence.id
    }

    data class Capped(
        val escalation: PersistentReminderEscalation,
    ) : PersistentReminderEscalationResult {
        override val occurrenceId: ScheduledOccurrenceId = escalation.sourceOccurrenceId
    }

    data class ScheduleRejected(
        val escalation: PersistentReminderEscalation,
        override val occurrenceId: ScheduledOccurrenceId,
        val reason: AlarmScheduleRejectionReason,
    ) : PersistentReminderEscalationResult

    data class TaskMissing(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : PersistentReminderEscalationResult

    data class NotEligible(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : PersistentReminderEscalationResult
}

internal val ReminderMode.supportsAlarmGradeSurface: Boolean
    get() = this == ReminderMode.STRICT || this == ReminderMode.PERSISTENT
