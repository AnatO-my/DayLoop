package com.dayloop.app.repository

import androidx.room.withTransaction
import com.dayloop.app.core.alarms.*
import com.dayloop.app.core.database.DayLoopDatabase
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.rust.NoOpRustTaskTransitionShadowObserver
import com.dayloop.app.core.rust.RustTaskTransitionProtocol
import com.dayloop.app.core.rust.RustTaskTransitionShadowObserver
import java.time.Duration
import java.time.Instant

data class AlarmActionCommit(
    val result: AlarmNotificationActionResult,
    val cancelled: List<ScheduledOccurrenceId> = emptyList(),
    val scheduled: ScheduledOccurrence? = null,
)

class RoomAlarmStateStore(private val database: DayLoopDatabase) {
    private var rustShadow: RustTaskTransitionShadowObserver = NoOpRustTaskTransitionShadowObserver

    /** Internal-only developer/test seam; production construction always starts disabled. */
    internal constructor(
        database: DayLoopDatabase,
        rustShadow: RustTaskTransitionShadowObserver,
    ) : this(database) {
        this.rustShadow = rustShadow
    }
    suspend fun accept(delivery: AlarmDelivery, now: Instant): AlarmDeliveryResult = database.withTransaction {
        val row = database.scheduledOccurrenceDao().getById(delivery.occurrenceId.value)
            ?: return@withTransaction AlarmDeliveryResult.StaleMissing(delivery.occurrenceId)
        val occurrence = row.toDomain()
        if (occurrence.generation != delivery.generation) return@withTransaction AlarmDeliveryResult.StaleGeneration(
            occurrence.id, occurrence.generation, delivery.generation)
        if (occurrence.status != ScheduledOccurrenceStatus.SCHEDULED) return@withTransaction AlarmDeliveryResult.StaleStatus(
            occurrence.id, occurrence.status)
        val fired = occurrence.markFired(maxOf(now, occurrence.createdAt))
        database.scheduledOccurrenceDao().upsert(fired.toEntity())
        AlarmDeliveryResult.Accepted(fired)
    }

    suspend fun act(command: AlarmNotificationCommand, now: Instant, snooze: Duration): AlarmActionCommit =
        database.withTransaction {
            require(!snooze.isNegative && !snooze.isZero)
            val occurrence = database.scheduledOccurrenceDao().getById(command.delivery.occurrenceId.value)?.toDomain()
            if (occurrence == null || occurrence.generation != command.delivery.generation ||
                occurrence.status == ScheduledOccurrenceStatus.CANCELLED) {
                return@withTransaction AlarmActionCommit(AlarmNotificationActionResult.Stale(command.delivery.occurrenceId))
            }
            val task = database.taskDao().getById(occurrence.taskId.value)?.toDomain()
                ?: return@withTransaction AlarmActionCommit(AlarmNotificationActionResult.TaskMissing(occurrence.id, occurrence.taskId))
            if (task.status != TaskStatus.ACTIVE) return@withTransaction AlarmActionCommit(
                AlarmNotificationActionResult.Stale(occurrence.id))
            when (command.action) {
                AlarmNotificationAction.COMPLETE -> {
                    rustShadow.observe(task, RustTaskTransitionProtocol.Command.COMPLETE, now)
                    database.taskDao().upsert(task.markCompleted(now).toEntity())
                    AlarmActionCommit(AlarmNotificationActionResult.Completed(occurrence.id, task.id), cancelTaskRows(task.id, now))
                }
                AlarmNotificationAction.SNOOZE -> {
                    if (!task.reminderPolicy.snoozeAllowed) return@withTransaction AlarmActionCommit(
                        AlarmNotificationActionResult.ActionNotAllowed(occurrence.id, command.action))
                    val scheduled = occurrence.copy(fireAt = now.plus(snooze),
                        generation = occurrence.generation + 1, status = ScheduledOccurrenceStatus.SCHEDULED,
                        updatedAt = maxOf(now, occurrence.createdAt))
                    database.scheduledOccurrenceDao().upsert(scheduled.toEntity())
                    database.persistentReminderEscalationDao().getForTask(task.id.value)
                        .filter { it.sourceOccurrenceId == occurrence.id.value && it.status == "ACTIVE" }
                        .forEach {
                            val chain = it.toDomain()
                            database.persistentReminderEscalationDao().upsert(chain.copy(
                                nextRealertAt = scheduled.fireAt, updatedAt = maxOf(now, chain.createdAt)).toEntity())
                        }
                    AlarmActionCommit(AlarmNotificationActionResult.Snoozed(scheduled, false), scheduled = scheduled)
                }
                AlarmNotificationAction.RESCHEDULE -> {
                    if (!task.reminderPolicy.rescheduleAllowed) AlarmActionCommit(
                        AlarmNotificationActionResult.ActionNotAllowed(occurrence.id, command.action))
                    else AlarmActionCommit(AlarmNotificationActionResult.RescheduleRequested(occurrence.id, task.id))
                }
            }
        }

    suspend fun completeTask(taskId: TaskId, now: Instant, generatedTask: Task? = null): List<ScheduledOccurrenceId> = database.withTransaction {
        require(generatedTask == null || generatedTask.id == taskId)
        val stored = database.taskDao().getById(taskId.value)?.toDomain()
        val task = stored?.takeIf { generatedTask == null || it.provenance == generatedTask.provenance }
            ?: generatedTask ?: return@withTransaction emptyList()
        stored?.let { rustShadow.observe(it, RustTaskTransitionProtocol.Command.COMPLETE, now) }
        database.taskDao().upsert(task.markCompleted(now).toEntity())
        cancelTaskRows(taskId, now)
    }

    suspend fun saveTask(task: Task, now: Instant, next: ScheduledOccurrence? = null): List<ScheduledOccurrenceId> = database.withTransaction {
        val previous = database.taskDao().getById(task.id.value)?.toDomain()
        previous?.shadowCommandFor(task)?.let { rustShadow.observe(previous, it, now) }
        val invalidates = previous != null && (previous.schedule != task.schedule ||
            previous.reminderPolicy != task.reminderPolicy || previous.status != task.status)
        val cancelled = if (invalidates) cancelTaskRows(task.id, now) else emptyList()
        database.taskDao().upsert(task.toEntity())
        next?.let { database.scheduledOccurrenceDao().upsert(it.toEntity()) }
        cancelled
    }

    suspend fun deleteTask(taskId: TaskId, now: Instant): List<ScheduledOccurrenceId> = database.withTransaction {
        val cancelled = cancelTaskRows(taskId, now)
        database.taskDao().deleteById(taskId.value)
        cancelled
    }

    private suspend fun cancelTaskRows(taskId: TaskId, now: Instant): List<ScheduledOccurrenceId> {
        val rows = database.scheduledOccurrenceDao().getForTask(taskId.value).map { it.toDomain() }
        rows.filter { it.status != ScheduledOccurrenceStatus.CANCELLED }.forEach {
            database.scheduledOccurrenceDao().upsert(it.copy(status = ScheduledOccurrenceStatus.CANCELLED,
                generation = it.generation + 1, updatedAt = maxOf(now, it.createdAt)).toEntity())
        }
        database.persistentReminderEscalationDao().getForTask(taskId.value)
            .filter { it.status == "ACTIVE" }.forEach {
                val chain = it.toDomain()
                database.persistentReminderEscalationDao().upsert(chain.resolve(maxOf(now, chain.createdAt)).toEntity())
            }
        return rows.map { it.id }
    }

    private fun Task.shadowCommandFor(updated: Task): RustTaskTransitionProtocol.Command? = when {
        status != TaskStatus.COMPLETED && updated.status == TaskStatus.COMPLETED ->
            RustTaskTransitionProtocol.Command.COMPLETE
        status != TaskStatus.ACTIVE && updated.status == TaskStatus.ACTIVE ->
            RustTaskTransitionProtocol.Command.REOPEN
        status != TaskStatus.SKIPPED && updated.status == TaskStatus.SKIPPED ->
            RustTaskTransitionProtocol.Command.SKIP
        else -> null
    }
}
