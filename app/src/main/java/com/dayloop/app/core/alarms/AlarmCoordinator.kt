package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.notifications.ReminderNotifications
import com.dayloop.app.repository.*
import com.dayloop.app.time.TimeProvider
import java.time.Duration
import java.time.ZoneId
import com.dayloop.app.core.model.Task

class AlarmCoordinator(
    private val store: RoomAlarmStateStore,
    private val tasks: TaskRepository,
    private val occurrences: ScheduledOccurrenceRepository,
    private val escalations: PersistentReminderEscalationRepository,
    private val scheduler: AlarmScheduler,
    private val notifications: ReminderNotifications,
    private val diagnostics: AlarmDiagnosticsRepository,
    private val clock: TimeProvider,
) {
    suspend fun deliver(delivery: AlarmDelivery) = AlarmMutationLock.run {
        val accepted = store.accept(delivery, clock.now())
        if (accepted !is AlarmDeliveryResult.Accepted) {
            record(delivery.occurrenceId, null, AlarmDiagnosticEvent.DELIVERY_STALE)
            return@run
        }
        val occurrence = accepted.occurrence
        val task = tasks.getTask(occurrence.taskId)
        record(occurrence.id, occurrence.taskId, AlarmDiagnosticEvent.DELIVERY_ACCEPTED)
        if (task == null || task.status != TaskStatus.ACTIVE) {
            notifications.cancel(occurrence.id)
            return@run
        }
        PersistentReminderEscalationProcessor(tasks, occurrences, escalations, scheduler, clock)
            .processAcceptedDelivery(occurrence)
        val actionOccurrence = occurrences.getOccurrence(occurrence.id) ?: occurrence
        notifications.show(task, occurrence, actionOccurrence)
    }

    suspend fun action(command: AlarmNotificationCommand): AlarmNotificationActionResult = AlarmMutationLock.run {
        val commit = store.act(command, clock.now(), Duration.ofMinutes(10))
        commit.cancelled.forEach { scheduler.cancel(it); notifications.cancel(it) }
        var result = commit.result
        commit.scheduled?.let { scheduled ->
            result = when (val outcome = scheduler.schedule(scheduled)) {
                is AlarmScheduleResult.Scheduled -> AlarmNotificationActionResult.Snoozed(scheduled, outcome.exact)
                is AlarmScheduleResult.Rejected -> AlarmNotificationActionResult.SnoozeRejected(scheduled.id, outcome.reason)
            }
            notifications.cancel(scheduled.id)
        }
        val event = when (result) {
            is AlarmNotificationActionResult.Completed -> AlarmDiagnosticEvent.ACTION_COMPLETED
            is AlarmNotificationActionResult.Snoozed -> AlarmDiagnosticEvent.ACTION_SNOOZED
            is AlarmNotificationActionResult.RescheduleRequested -> AlarmDiagnosticEvent.ACTION_RESCHEDULE_REQUESTED
            else -> AlarmDiagnosticEvent.ACTION_REJECTED
        }
        record(command.delivery.occurrenceId, null, event)
        result
    }

    suspend fun completeTask(taskId: TaskId, generatedTask: Task? = null) = AlarmMutationLock.run {
        store.completeTask(taskId, clock.now(), generatedTask).forEach { scheduler.cancel(it); notifications.cancel(it) }
    }

    suspend fun saveTask(task: Task): String? = AlarmMutationLock.run {
        val previous = tasks.getTask(task.id)
        val timingChanged = previous == null || previous.schedule != task.schedule ||
            previous.reminderPolicy != task.reminderPolicy || previous.status != task.status
        val now = clock.now()
        val next = if (timingChanged) NextOccurrenceCalculator().calculate(task, now, ZoneId.systemDefault()) else null
        store.saveTask(task, now, next).forEach { scheduler.cancel(it); notifications.cancel(it) }
        when (val result = next?.let { scheduler.schedule(it) }) {
            is AlarmScheduleResult.Rejected -> "Task saved. Open More to repair reminder delivery."
            is AlarmScheduleResult.Scheduled -> if (result.exact) null else "Task saved. Reminder timing may be approximate."
            else -> null
        }
    }

    suspend fun deleteTask(taskId: TaskId) = AlarmMutationLock.run {
        store.deleteTask(taskId, clock.now()).forEach { scheduler.cancel(it); notifications.cancel(it) }
    }

    private suspend fun record(id: ScheduledOccurrenceId, taskId: TaskId?, event: AlarmDiagnosticEvent) {
        diagnostics.append(AlarmDiagnosticEntry(occurrenceId = id, taskId = taskId, event = event, createdAt = clock.now()))
    }
}
