package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.FakeTimeProvider
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmNotificationActionProcessorTest {
    @Test
    fun completeMarksActiveTaskCompletedAndOccurrenceFired() = runBlocking {
        val task = baseTask()
        val occurrence = baseOccurrence(taskId = task.id)
        val taskRepository = FakeTaskRepository(task)
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler()
        val processor = processor(taskRepository, occurrenceRepository, scheduler)

        val result = processor.process(command(AlarmNotificationAction.COMPLETE))

        assertEquals(AlarmNotificationActionResult.Completed(occurrence.id, task.id), result)
        assertEquals(TaskStatus.COMPLETED, taskRepository.getTask(task.id)?.status)
        assertEquals(occurrence.markFired(NOW), occurrenceRepository.getOccurrence(occurrence.id))
        assertEquals(listOf(occurrence.id), scheduler.cancelledOccurrenceIds)
    }

    @Test
    fun duplicateCompleteForResolvedOccurrenceIsIgnoredAsStale() = runBlocking {
        val task = baseTask().markCompleted()
        val occurrence = baseOccurrence(taskId = task.id).markFired(NOW)
        val taskRepository = FakeTaskRepository(task)
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val processor = processor(taskRepository, occurrenceRepository, FakeAlarmScheduler())

        val result = processor.process(command(AlarmNotificationAction.COMPLETE))

        assertEquals(AlarmNotificationActionResult.Stale(occurrence.id), result)
        assertEquals(task, taskRepository.getTask(task.id))
        assertEquals(occurrence, occurrenceRepository.getOccurrence(occurrence.id))
    }

    @Test
    fun completeReturnsTaskMissingWithoutMutatingOccurrence() = runBlocking {
        val occurrence = baseOccurrence()
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val processor = processor(FakeTaskRepository(), occurrenceRepository, FakeAlarmScheduler())

        val result = processor.process(command(AlarmNotificationAction.COMPLETE))

        assertEquals(AlarmNotificationActionResult.TaskMissing(occurrence.id, occurrence.taskId), result)
        assertEquals(occurrence, occurrenceRepository.getOccurrence(occurrence.id))
    }

    @Test
    fun snoozeCreatesLaterGenerationAndSchedulesItBeforePersisting() = runBlocking {
        val occurrence = baseOccurrence()
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler()
        val processor = processor(FakeTaskRepository(baseTask()), occurrenceRepository, scheduler)

        val result = processor.process(command(AlarmNotificationAction.SNOOZE))

        val expected = occurrence.copy(
            fireAt = NOW.plus(Duration.ofMinutes(10)),
            generation = 3L,
            updatedAt = NOW,
        )
        assertEquals(AlarmNotificationActionResult.Snoozed(expected, scheduledExact = true), result)
        assertEquals(listOf(expected), scheduler.scheduledOccurrences)
        assertEquals(expected, occurrenceRepository.getOccurrence(occurrence.id))
    }

    @Test
    fun snoozeRejectionDoesNotPersistReplacement() = runBlocking {
        val occurrence = baseOccurrence()
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler(canSchedule = false)
        val processor = processor(FakeTaskRepository(baseTask()), occurrenceRepository, scheduler)

        val result = processor.process(command(AlarmNotificationAction.SNOOZE))

        assertEquals(
            AlarmNotificationActionResult.SnoozeRejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
            ),
            result,
        )
        assertEquals(occurrence, occurrenceRepository.getOccurrence(occurrence.id))
    }

    @Test
    fun rescheduleRequestDoesNotMutateStateYet() = runBlocking {
        val task = baseTask()
        val occurrence = baseOccurrence(taskId = task.id)
        val taskRepository = FakeTaskRepository(task)
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val processor = processor(taskRepository, occurrenceRepository, FakeAlarmScheduler())

        val result = processor.process(command(AlarmNotificationAction.RESCHEDULE))

        assertEquals(AlarmNotificationActionResult.RescheduleRequested(occurrence.id, task.id), result)
        assertEquals(task, taskRepository.getTask(task.id))
        assertEquals(occurrence, occurrenceRepository.getOccurrence(occurrence.id))
    }

    @Test
    fun actionsIgnoreReplacedGenerationAsStale() = runBlocking {
        val occurrence = baseOccurrence(generation = 3L)
        val occurrenceRepository = FakeScheduledOccurrenceRepository(occurrence)
        val processor = processor(FakeTaskRepository(baseTask()), occurrenceRepository, FakeAlarmScheduler())

        val result = processor.process(
            AlarmNotificationCommand(
                action = AlarmNotificationAction.SNOOZE,
                delivery = AlarmDelivery(occurrence.id, generation = 2L),
            ),
        )

        assertEquals(AlarmNotificationActionResult.Stale(occurrence.id), result)
        assertEquals(occurrence, occurrenceRepository.getOccurrence(occurrence.id))
    }

    private fun processor(
        taskRepository: TaskRepository,
        occurrenceRepository: ScheduledOccurrenceRepository,
        scheduler: AlarmScheduler,
    ): AlarmNotificationActionProcessor = AlarmNotificationActionProcessor(
        taskRepository = taskRepository,
        scheduledOccurrenceRepository = occurrenceRepository,
        alarmScheduler = scheduler,
        timeProvider = FakeTimeProvider(NOW),
    )

    private fun command(action: AlarmNotificationAction): AlarmNotificationCommand = AlarmNotificationCommand(
        action = action,
        delivery = AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L),
    )

    private fun baseTask(): Task = Task(
        id = TaskId("task-1"),
        title = "Review plan",
    )

    private fun baseOccurrence(
        taskId: TaskId = TaskId("task-1"),
        generation: Long = 2L,
    ): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId("occurrence-1"),
        taskId = taskId,
        fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        generation = generation,
        createdAt = Instant.parse("2026-09-06T08:00:00Z"),
    )

    private class FakeTaskRepository(
        vararg tasks: Task,
    ) : TaskRepository {
        private val stored = tasks.associateBy { it.id }.toMutableMap()
        private val observed = MutableStateFlow(stored.values.toList())

        override fun observeTasks(): Flow<List<Task>> = observed

        override fun observeTask(id: TaskId): Flow<Task?> = MutableStateFlow(stored[id])

        override suspend fun getTask(id: TaskId): Task? = stored[id]

        override suspend fun saveTask(task: Task) {
            stored[task.id] = task
            observed.value = stored.values.toList()
        }

        override suspend fun deleteTask(id: TaskId): Boolean = stored.remove(id) != null
    }

    private class FakeScheduledOccurrenceRepository(
        vararg occurrences: ScheduledOccurrence,
    ) : ScheduledOccurrenceRepository {
        private val stored = occurrences.associateBy { it.id }.toMutableMap()
        private val observed = MutableStateFlow(stored.values.toList())

        override fun observeOccurrences(): Flow<List<ScheduledOccurrence>> = observed

        override suspend fun getOccurrence(id: ScheduledOccurrenceId): ScheduledOccurrence? = stored[id]

        override suspend fun getOccurrencesForTask(taskId: TaskId): List<ScheduledOccurrence> =
            stored.values.filter { it.taskId == taskId }

        override suspend fun getScheduledOccurrences(): List<ScheduledOccurrence> =
            stored.values.filter { it.status == ScheduledOccurrenceStatus.SCHEDULED }

        override suspend fun saveOccurrence(occurrence: ScheduledOccurrence) {
            stored[occurrence.id] = occurrence
            observed.value = stored.values.toList()
        }

        override suspend fun deleteOccurrence(id: ScheduledOccurrenceId): Boolean = stored.remove(id) != null

        override suspend fun deleteOccurrencesForTask(taskId: TaskId): Int {
            val idsToRemove = stored.values.filter { it.taskId == taskId }.map { it.id }
            idsToRemove.forEach { stored.remove(it) }
            return idsToRemove.size
        }
    }

    private class FakeAlarmScheduler(
        private val canSchedule: Boolean = true,
    ) : AlarmScheduler {
        val scheduledOccurrences = mutableListOf<ScheduledOccurrence>()
        val cancelledOccurrenceIds = mutableListOf<ScheduledOccurrenceId>()

        override suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult {
            return if (canSchedule) {
                scheduledOccurrences += occurrence
                AlarmScheduleResult.Scheduled(occurrence.id, exact = true)
            } else {
                AlarmScheduleResult.Rejected(
                    occurrenceId = occurrence.id,
                    reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
                )
            }
        }

        override suspend fun cancel(occurrenceId: ScheduledOccurrenceId): AlarmCancelResult {
            cancelledOccurrenceIds += occurrenceId
            return AlarmCancelResult.Cancelled(occurrenceId)
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-06T09:05:00Z")
    }
}