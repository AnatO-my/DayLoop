package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.TimeProvider
import java.time.Duration

class AlarmNotificationActionProcessor(
    private val taskRepository: TaskRepository,
    private val scheduledOccurrenceRepository: ScheduledOccurrenceRepository,
    private val alarmScheduler: AlarmScheduler,
    private val timeProvider: TimeProvider,
    private val snoozeDuration: Duration = DEFAULT_SNOOZE_DURATION,
    private val diagnosticsRepository: AlarmDiagnosticsRepository? = null,
    private val escalationRepository: PersistentReminderEscalationRepository? = null,
) {
    init {
        require(!snoozeDuration.isZero && !snoozeDuration.isNegative) {
            "Snooze duration must be positive."
        }
    }

    suspend fun process(command: AlarmNotificationCommand): AlarmNotificationActionResult = when (command.action) {
        AlarmNotificationAction.COMPLETE -> complete(command.delivery)
        AlarmNotificationAction.SNOOZE -> snooze(command.delivery)
        AlarmNotificationAction.RESCHEDULE -> reschedule(command.delivery)
    }

    private suspend fun complete(delivery: AlarmDelivery): AlarmNotificationActionResult {
        val fresh = loadFreshOccurrence(delivery) ?: return staleResult(delivery)
        val task = taskRepository.getTask(fresh.taskId)
        if (task == null) {
            val result = AlarmNotificationActionResult.TaskMissing(fresh.id, fresh.taskId)
            record(fresh, AlarmDiagnosticEvent.ACTION_REJECTED, "TASK_MISSING")
            return result
        }

        val now = timeProvider.now()
        if (task.status == TaskStatus.ACTIVE) {
            taskRepository.saveTask(task.markCompleted(now))
        }

        val fired = fresh.markFired(now)
        scheduledOccurrenceRepository.saveOccurrence(fired)
        resolveActiveEscalationsForTask(fresh.taskId, now)
        alarmScheduler.cancel(fresh.id)
        val result = AlarmNotificationActionResult.Completed(fired.id, task.id)
        record(fired, AlarmDiagnosticEvent.ACTION_COMPLETED)
        return result
    }

    private suspend fun snooze(delivery: AlarmDelivery): AlarmNotificationActionResult {
        val fresh = loadFreshOccurrence(delivery) ?: return staleResult(delivery)
        val task = taskRepository.getTask(fresh.taskId)
        if (task == null) {
            val result = AlarmNotificationActionResult.TaskMissing(fresh.id, fresh.taskId)
            record(fresh, AlarmDiagnosticEvent.ACTION_REJECTED, "TASK_MISSING")
            return result
        }
        if (task.status != TaskStatus.ACTIVE || !task.reminderPolicy.snoozeAllowed) {
            val result = AlarmNotificationActionResult.ActionNotAllowed(
                occurrenceId = fresh.id,
                action = AlarmNotificationAction.SNOOZE,
            )
            record(
                fresh,
                AlarmDiagnosticEvent.ACTION_REJECTED,
                if (task.status != TaskStatus.ACTIVE) "TASK_NOT_ACTIVE" else "SNOOZE_NOT_ALLOWED",
            )
            return result
        }

        val now = timeProvider.now()
        val snoozed = fresh.copy(
            fireAt = now.plus(snoozeDuration),
            generation = fresh.generation + 1,
            updatedAt = now,
        )

        return when (val scheduleResult = alarmScheduler.schedule(snoozed)) {
            is AlarmScheduleResult.Scheduled -> {
                scheduledOccurrenceRepository.saveOccurrence(snoozed)
                postponeActiveEscalationsForOccurrence(snoozed, now)
                val result = AlarmNotificationActionResult.Snoozed(
                    occurrence = snoozed,
                    scheduledExact = scheduleResult.exact,
                )
                record(snoozed, AlarmDiagnosticEvent.ACTION_SNOOZED)
                result
            }
            is AlarmScheduleResult.Rejected -> {
                val result = AlarmNotificationActionResult.SnoozeRejected(
                    occurrenceId = fresh.id,
                    reason = scheduleResult.reason,
                )
                record(fresh, AlarmDiagnosticEvent.ACTION_REJECTED, scheduleResult.reason.name)
                result
            }
        }
    }

    private suspend fun reschedule(delivery: AlarmDelivery): AlarmNotificationActionResult {
        val fresh = loadFreshOccurrence(delivery) ?: return staleResult(delivery)
        val task = taskRepository.getTask(fresh.taskId)
        if (task == null) {
            val result = AlarmNotificationActionResult.TaskMissing(fresh.id, fresh.taskId)
            record(fresh, AlarmDiagnosticEvent.ACTION_REJECTED, "TASK_MISSING")
            return result
        }
        if (!task.reminderPolicy.rescheduleAllowed) {
            val result = AlarmNotificationActionResult.ActionNotAllowed(
                occurrenceId = fresh.id,
                action = AlarmNotificationAction.RESCHEDULE,
            )
            record(fresh, AlarmDiagnosticEvent.ACTION_REJECTED, "RESCHEDULE_NOT_ALLOWED")
            return result
        }

        resolveActiveEscalationsForTask(fresh.taskId, timeProvider.now())
        alarmScheduler.cancel(fresh.id)
        val result = AlarmNotificationActionResult.RescheduleRequested(fresh.id, fresh.taskId)
        record(fresh, AlarmDiagnosticEvent.ACTION_RESCHEDULE_REQUESTED)
        return result
    }

    private suspend fun loadFreshOccurrence(delivery: AlarmDelivery): ScheduledOccurrence? {
        val persisted = scheduledOccurrenceRepository.getOccurrence(delivery.occurrenceId) ?: return null
        if (persisted.generation != delivery.generation) return null
        if (persisted.status != ScheduledOccurrenceStatus.SCHEDULED) return null
        return persisted
    }

    private suspend fun resolveActiveEscalationsForTask(taskId: com.dayloop.app.core.model.TaskId, at: java.time.Instant) {
        val repository = escalationRepository ?: return
        repository.getActiveEscalations()
            .filter { escalation -> escalation.taskId == taskId }
            .forEach { escalation -> repository.saveEscalation(escalation.resolve(at)) }
    }

    private suspend fun postponeActiveEscalationsForOccurrence(
        occurrence: ScheduledOccurrence,
        at: java.time.Instant,
    ) {
        val repository = escalationRepository ?: return
        repository.getActiveEscalations()
            .filter { escalation ->
                escalation.taskId == occurrence.taskId && escalation.sourceOccurrenceId == occurrence.id
            }
            .forEach { escalation ->
                repository.saveEscalation(
                    escalation.copy(
                        nextRealertAt = occurrence.fireAt,
                        updatedAt = at,
                    ),
                )
            }
    }

    private suspend fun staleResult(delivery: AlarmDelivery): AlarmNotificationActionResult {
        val result = AlarmNotificationActionResult.Stale(delivery.occurrenceId)
        record(delivery.occurrenceId, AlarmDiagnosticEvent.ACTION_REJECTED, "STALE_DELIVERY")
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

    private companion object {
        val DEFAULT_SNOOZE_DURATION: Duration = Duration.ofMinutes(10)
    }
}

data class AlarmNotificationCommand(
    val action: AlarmNotificationAction,
    val delivery: AlarmDelivery,
)

enum class AlarmNotificationAction {
    COMPLETE,
    SNOOZE,
    RESCHEDULE,
}

sealed interface AlarmNotificationActionResult {
    val occurrenceId: ScheduledOccurrenceId

    data class Completed(
        override val occurrenceId: ScheduledOccurrenceId,
        val taskId: com.dayloop.app.core.model.TaskId,
    ) : AlarmNotificationActionResult

    data class Snoozed(
        val occurrence: ScheduledOccurrence,
        val scheduledExact: Boolean,
    ) : AlarmNotificationActionResult {
        override val occurrenceId: ScheduledOccurrenceId = occurrence.id
    }

    data class SnoozeRejected(
        override val occurrenceId: ScheduledOccurrenceId,
        val reason: AlarmScheduleRejectionReason,
    ) : AlarmNotificationActionResult

    data class RescheduleRequested(
        override val occurrenceId: ScheduledOccurrenceId,
        val taskId: com.dayloop.app.core.model.TaskId,
    ) : AlarmNotificationActionResult

    data class ActionNotAllowed(
        override val occurrenceId: ScheduledOccurrenceId,
        val action: AlarmNotificationAction,
    ) : AlarmNotificationActionResult

    data class TaskMissing(
        override val occurrenceId: ScheduledOccurrenceId,
        val taskId: com.dayloop.app.core.model.TaskId,
    ) : AlarmNotificationActionResult

    data class Stale(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : AlarmNotificationActionResult
}
