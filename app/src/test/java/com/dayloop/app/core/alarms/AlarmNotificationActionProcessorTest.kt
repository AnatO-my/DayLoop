package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.FakeTimeProvider
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

    @Test
    fun forbiddenSnoozeLeavesAllStateUntouchedEvenWhenRepeated() = runBlocking {
        for (policy in listOf(
            ReminderPolicy.strict(Duration.ofMinutes(5), 3),
            ReminderPolicy.normal().copy(snoozeAllowed = false),
        )) {
            val task = baseTask(policy)
            val occurrence = baseOccurrence()
            val tasks = FakeTaskRepository(task)
            val occurrences = FakeScheduledOccurrenceRepository(occurrence)
            val scheduler = FakeAlarmScheduler()
            val escalations = FakeEscalationRepository()
            val processor = processor(tasks, occurrences, scheduler, escalations)

            repeat(2) {
                assertEquals(
                    AlarmNotificationActionResult.ActionNotAllowed(occurrence.id, AlarmNotificationAction.SNOOZE),
                    processor.process(command(AlarmNotificationAction.SNOOZE)),
                )
            }

            assertEquals(task, tasks.getTask(task.id))
            assertEquals(occurrence, occurrences.getOccurrence(occurrence.id))
            assertEquals(0, tasks.saveCalls)
            assertEquals(0, occurrences.saveCalls)
            assertEquals(0, scheduler.scheduleAttempts)
            assertTrue(scheduler.cancelledOccurrenceIds.isEmpty())
            assertEquals(0, escalations.calls)
        }
    }

    @Test
    fun allowedPoliciesPreserveTaskAndReportFallback() = runBlocking {
        for (policy in listOf(
            ReminderPolicy.normal(),
            ReminderPolicy.persistent(Duration.ofMinutes(5), 3),
            ReminderPolicy.strict(Duration.ofMinutes(5), 3, snoozeAllowed = true),
        )) {
            val task = baseTask(policy)
            val tasks = FakeTaskRepository(task)
            val occurrences = FakeScheduledOccurrenceRepository(baseOccurrence())
            val scheduler = FakeAlarmScheduler(exact = false)
            val result = processor(tasks, occurrences, scheduler)
                .process(command(AlarmNotificationAction.SNOOZE)) as AlarmNotificationActionResult.Snoozed

            assertEquals(NOW.plus(Duration.ofMinutes(10)), result.occurrence.fireAt)
            assertEquals(3L, result.occurrence.generation)
            assertEquals(false, result.scheduledExact)
            assertEquals(task, tasks.getTask(task.id))
            assertEquals(0, tasks.saveCalls)
            assertEquals(1, occurrences.saveCalls)
            assertEquals(1, scheduler.scheduleAttempts)
            assertTrue(scheduler.cancelledOccurrenceIds.isEmpty())
        }
    }

    @Test
    fun allowedStrictSnoozeOnlyPostponesMatchingChainAndDuplicateIsStale() = runBlocking {
        val task = baseTask(ReminderPolicy.strict(Duration.ofMinutes(5), 3, snoozeAllowed = true))
        val occurrence = baseOccurrence()
        val chain = PersistentReminderEscalation.start(
            task.reminderPolicy, task.id, occurrence.id, NOW.minus(Duration.ofMinutes(10)),
        )
        val other = chain.copy(
            id = PersistentReminderEscalationId("other-chain"),
            sourceOccurrenceId = ScheduledOccurrenceId("other-occurrence"),
        )
        val escalations = FakeEscalationRepository(chain, other)
        val tasks = FakeTaskRepository(task)
        val occurrences = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler()
        val processor = processor(tasks, occurrences, scheduler, escalations)

        val result = processor.process(command(AlarmNotificationAction.SNOOZE))
            as AlarmNotificationActionResult.Snoozed
        assertEquals(
            chain.copy(nextRealertAt = result.occurrence.fireAt, updatedAt = NOW),
            escalations.getEscalation(chain.id),
        )
        assertEquals(other, escalations.getEscalation(other.id))
        assertEquals(AlarmNotificationActionResult.Stale(occurrence.id),
            processor.process(command(AlarmNotificationAction.SNOOZE)))
        assertEquals(1, occurrences.saveCalls)
        assertEquals(1, scheduler.scheduleAttempts)
        assertEquals(2, escalations.calls)
    }

    @Test
    fun rejectedStrictSnoozePreservesOccurrenceAndChain() = runBlocking {
        val task = baseTask(ReminderPolicy.strict(Duration.ofMinutes(5), 3, snoozeAllowed = true))
        val occurrence = baseOccurrence()
        val chain = PersistentReminderEscalation.start(
            task.reminderPolicy, task.id, occurrence.id, NOW.minus(Duration.ofMinutes(10)),
        )
        val escalations = FakeEscalationRepository(chain)
        val tasks = FakeTaskRepository(task)
        val occurrences = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler(canSchedule = false)

        val result = processor(tasks, occurrences, scheduler, escalations)
            .process(command(AlarmNotificationAction.SNOOZE))

        assertTrue(result is AlarmNotificationActionResult.SnoozeRejected)
        assertEquals(occurrence, occurrences.getOccurrence(occurrence.id))
        assertEquals(chain, escalations.getEscalation(chain.id))
        assertEquals(0, tasks.saveCalls)
        assertEquals(0, occurrences.saveCalls)
        assertEquals(0, escalations.calls)
        assertEquals(1, scheduler.scheduleAttempts)
    }

    @Test
    fun completedTaskCannotBeSnoozedDespiteAllowingPolicy() = runBlocking {
        val task = baseTask(ReminderPolicy.strict(Duration.ofMinutes(5), 3, snoozeAllowed = true)).markCompleted()
        val occurrence = baseOccurrence()
        val tasks = FakeTaskRepository(task)
        val occurrences = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler()
        val escalations = FakeEscalationRepository()

        assertEquals(
            AlarmNotificationActionResult.ActionNotAllowed(occurrence.id, AlarmNotificationAction.SNOOZE),
            processor(tasks, occurrences, scheduler, escalations).process(command(AlarmNotificationAction.SNOOZE)),
        )
        assertEquals(0, tasks.saveCalls)
        assertEquals(0, occurrences.saveCalls)
        assertEquals(0, scheduler.scheduleAttempts)
        assertEquals(0, escalations.calls)
    }

    @Test
    fun missingTaskSnoozeHasNoSchedulingOrPersistenceEffects() = runBlocking {
        val occurrence = baseOccurrence()
        val tasks = FakeTaskRepository()
        val occurrences = FakeScheduledOccurrenceRepository(occurrence)
        val scheduler = FakeAlarmScheduler()
        val escalations = FakeEscalationRepository()

        assertEquals(
            AlarmNotificationActionResult.TaskMissing(occurrence.id, occurrence.taskId),
            processor(tasks, occurrences, scheduler, escalations).process(command(AlarmNotificationAction.SNOOZE)),
        )
        assertEquals(0, occurrences.saveCalls)
        assertEquals(0, scheduler.scheduleAttempts)
        assertEquals(0, escalations.calls)
    }

    @Test
    fun obsoleteOccurrencesCannotBeSnoozedEvenWithOptIn() = runBlocking {
        val task = baseTask(ReminderPolicy.strict(Duration.ofMinutes(5), 3, snoozeAllowed = true))
        for (occurrence in listOf(
            baseOccurrence(generation = 3),
            baseOccurrence().markFired(NOW),
            baseOccurrence().markCancelled(NOW),
        )) {
            val occurrences = FakeScheduledOccurrenceRepository(occurrence)
            val scheduler = FakeAlarmScheduler()
            val escalations = FakeEscalationRepository()
            assertEquals(
                AlarmNotificationActionResult.Stale(occurrence.id),
                processor(FakeTaskRepository(task), occurrences, scheduler, escalations)
                    .process(command(AlarmNotificationAction.SNOOZE)),
            )
            assertEquals(0, occurrences.saveCalls)
            assertEquals(0, scheduler.scheduleAttempts)
            assertEquals(0, escalations.calls)
        }
    }

    @Test
    fun nonPositiveSnoozeDurationIsRejected() {
        for (duration in listOf(Duration.ZERO, Duration.ofMinutes(-1))) {
            val failure = runCatching {
                processor(FakeTaskRepository(), FakeScheduledOccurrenceRepository(),
                    FakeAlarmScheduler(), snoozeDuration = duration)
            }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
        }
    }

    private fun processor(
        taskRepository: TaskRepository,
        occurrenceRepository: ScheduledOccurrenceRepository,
        scheduler: AlarmScheduler,
        escalationRepository: PersistentReminderEscalationRepository? = null,
        snoozeDuration: Duration = Duration.ofMinutes(10),
    ): AlarmNotificationActionProcessor = AlarmNotificationActionProcessor(
        taskRepository = taskRepository,
        scheduledOccurrenceRepository = occurrenceRepository,
        alarmScheduler = scheduler,
        timeProvider = FakeTimeProvider(NOW),
        escalationRepository = escalationRepository,
        snoozeDuration = snoozeDuration,
    )

    private fun command(action: AlarmNotificationAction): AlarmNotificationCommand = AlarmNotificationCommand(
        action = action,
        delivery = AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L),
    )

    private fun baseTask(policy: ReminderPolicy = ReminderPolicy.normal()): Task = Task(
        id = TaskId("task-1"),
        title = "Review plan",
        reminderPolicy = policy,
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
        var saveCalls = 0
        private val stored = tasks.associateBy { it.id }.toMutableMap()
        private val observed = MutableStateFlow(stored.values.toList())

        override fun observeTasks(): Flow<List<Task>> = observed

        override fun observeTask(id: TaskId): Flow<Task?> = MutableStateFlow(stored[id])

        override suspend fun getTask(id: TaskId): Task? = stored[id]

        override suspend fun saveTask(task: Task) {
            saveCalls++
            stored[task.id] = task
            observed.value = stored.values.toList()
        }

        override suspend fun getTasksForProject(projectId: ProjectId): List<Task> =
            stored.values.filter { it.projectId == projectId }

        override fun observeTasksForProject(projectId: ProjectId): Flow<List<Task>> =
            observed.map { tasks -> tasks.filter { it.projectId == projectId } }

        override suspend fun deleteTask(id: TaskId): Boolean = stored.remove(id) != null
    }

    private class FakeScheduledOccurrenceRepository(
        vararg occurrences: ScheduledOccurrence,
    ) : ScheduledOccurrenceRepository {
        var saveCalls = 0
        private val stored = occurrences.associateBy { it.id }.toMutableMap()
        private val observed = MutableStateFlow(stored.values.toList())

        override fun observeOccurrences(): Flow<List<ScheduledOccurrence>> = observed

        override suspend fun getOccurrence(id: ScheduledOccurrenceId): ScheduledOccurrence? = stored[id]

        override suspend fun getOccurrencesForTask(taskId: TaskId): List<ScheduledOccurrence> =
            stored.values.filter { it.taskId == taskId }

        override suspend fun getScheduledOccurrences(): List<ScheduledOccurrence> =
            stored.values.filter { it.status == ScheduledOccurrenceStatus.SCHEDULED }

        override suspend fun saveOccurrence(occurrence: ScheduledOccurrence) {
            saveCalls++
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

    private class FakeEscalationRepository(
        vararg chains: PersistentReminderEscalation,
    ) : PersistentReminderEscalationRepository {
        private val stored = chains.associateBy { it.id }.toMutableMap()
        var calls = 0
        override fun observeEscalations(): Flow<List<PersistentReminderEscalation>> =
            MutableStateFlow(stored.values.toList())
        override suspend fun getEscalation(id: PersistentReminderEscalationId) = stored[id]
        override suspend fun getEscalationsForTask(taskId: TaskId) = stored.values.filter { it.taskId == taskId }
        override suspend fun getActiveEscalations(): List<PersistentReminderEscalation> {
            calls++
            return stored.values.filter { it.isActive }
        }
        override suspend fun saveEscalation(escalation: PersistentReminderEscalation) {
            calls++
            stored[escalation.id] = escalation
        }
        override suspend fun advanceAfterDelivery(
            delivered: ScheduledOccurrence,
            previous: PersistentReminderEscalation?,
            next: PersistentReminderEscalation,
            nextOccurrence: ScheduledOccurrence?,
        ): Boolean = error("Snooze must not advance the delivered re-alert count.")
        override suspend fun deleteEscalation(id: PersistentReminderEscalationId): Boolean = error("Unexpected delete")
        override suspend fun deleteEscalationsForTask(taskId: TaskId): Int = error("Unexpected delete")
    }

    private class FakeAlarmScheduler(
        private val canSchedule: Boolean = true,
        private val exact: Boolean = true,
    ) : AlarmScheduler {
        val scheduledOccurrences = mutableListOf<ScheduledOccurrence>()
        var scheduleAttempts = 0
        val cancelledOccurrenceIds = mutableListOf<ScheduledOccurrenceId>()

        override suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult {
            scheduleAttempts++
            return if (canSchedule) {
                scheduledOccurrences += occurrence
                AlarmScheduleResult.Scheduled(occurrence.id, exact = exact)
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
