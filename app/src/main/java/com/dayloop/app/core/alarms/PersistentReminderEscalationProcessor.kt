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
    suspend fun processAcceptedDelivery(occurrence: ScheduledOccurrence): PersistentReminderEscalationResult {
        val task = taskRepository.getTask(occurrence.taskId)
        if (task == null) {
            resolveActiveEscalationsForOccurrence(occurrence)
            return PersistentReminderEscalationResult.TaskMissing(occurrence.id)
        }

        if (task.status != TaskStatus.ACTIVE || !task.reminderPolicy.mode.supportsRealerts) {
            resolveActiveEscalationsForOccurrence(occurrence)
            return PersistentReminderEscalationResult.NotEligible(occurrence.id)
        }

        val activeEscalation = activeEscalationFor(occurrence)
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
        escalationRepository.saveEscalation(nextEscalation)

        if (!nextEscalation.isActive) {
            return PersistentReminderEscalationResult.Capped(nextEscalation)
        }

        return scheduleNextRealert(occurrence, nextEscalation, task)
    }

    private suspend fun scheduleNextRealert(
        occurrence: ScheduledOccurrence,
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

        return when (val result = alarmScheduler.schedule(scheduled)) {
            is AlarmScheduleResult.Scheduled -> {
                scheduledOccurrenceRepository.saveOccurrence(scheduled)
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

    private suspend fun activeEscalationFor(occurrence: ScheduledOccurrence): PersistentReminderEscalation? =
        escalationRepository.getActiveEscalations().firstOrNull { escalation ->
            escalation.sourceOccurrenceId == occurrence.id && escalation.taskId == occurrence.taskId
        }
}

sealed interface PersistentReminderEscalationResult {
    val occurrenceId: ScheduledOccurrenceId

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
