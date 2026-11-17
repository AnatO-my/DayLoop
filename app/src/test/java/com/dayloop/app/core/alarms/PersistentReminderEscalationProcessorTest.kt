package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.FakeTimeProvider
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersistentReminderEscalationProcessorTest {
    @Test
    fun persistsOneSuccessorBeforeSchedulingAndPreservesFallback() = runBlocking {
        for (policy in listOf(
            ReminderPolicy.persistent(Duration.ofMinutes(5), 2),
            ReminderPolicy.strict(Duration.ofMinutes(5), 2),
        )) {
            val fixture = Fixture(policy)
            fixture.exact = false
            val result = fixture.processor().processAcceptedDelivery(fixture.occurrence)
                as PersistentReminderEscalationResult.Scheduled

            assertFalse(result.scheduledExact)
            assertEquals(Instant.parse("2026-09-07T09:05:00Z"), result.occurrence.fireAt)
            assertEquals(2L, result.occurrence.generation)
            assertEquals(0, result.escalation.realertsDelivered)
            assertEquals(listOf(result.occurrence), fixture.scheduled)
            assertEquals(result.occurrence, fixture.occurrence)
        }
    }

    @Test
    fun newProcessorResumesPersistedCountThenCapsWithoutRestarting() = runBlocking {
        val fixture = Fixture()
        val first = fixture.processor().processAcceptedDelivery(fixture.occurrence)
            as PersistentReminderEscalationResult.Scheduled
        fixture.fireNext()
        val second = fixture.processor().processAcceptedDelivery(fixture.occurrence)
            as PersistentReminderEscalationResult.Scheduled

        assertEquals(first.escalation.id, second.escalation.id)
        assertEquals(1, second.escalation.realertsDelivered)
        assertEquals(3L, second.occurrence.generation)
        fixture.fireNext()
        val last = fixture.processor().processAcceptedDelivery(fixture.occurrence)
            as PersistentReminderEscalationResult.Capped
        assertEquals(2, last.escalation.realertsDelivered)
        assertEquals(PersistentReminderEscalationStatus.CAPPED, fixture.chain!!.status)
        assertEquals(2, fixture.scheduled.size)
        assertTrue(fixture.processor().processAcceptedDelivery(fixture.occurrence)
            is PersistentReminderEscalationResult.Inactive)
        assertEquals(2, fixture.scheduled.size)
    }

    @Test
    fun duplicateAndUnacceptedDeliveriesDoNotAdvance() = runBlocking {
        val fixture = Fixture()
        val original = fixture.occurrence
        fixture.processor().processAcceptedDelivery(original)
        assertTrue(fixture.processor().processAcceptedDelivery(original)
            is PersistentReminderEscalationResult.Stale)
        assertTrue(fixture.processor().processAcceptedDelivery(fixture.occurrence)
            is PersistentReminderEscalationResult.Stale)
        assertEquals(0, fixture.chain!!.realertsDelivered)
        assertEquals(1, fixture.scheduled.size)
    }

    @Test
    fun oldAcceptedRecordCannotConsumeNextRealert() = runBlocking {
        val fixture = Fixture()
        val oldDelivery = fixture.occurrence
        fixture.processor().processAcceptedDelivery(oldDelivery)
        fixture.occurrence = oldDelivery
        assertTrue(fixture.processor().processAcceptedDelivery(oldDelivery)
            is PersistentReminderEscalationResult.Stale)
        assertEquals(0, fixture.chain!!.realertsDelivered)
        assertEquals(1, fixture.scheduled.size)
    }

    @Test
    fun rejectedScheduleLeavesDurableIntentAndDuplicateCannotIncrementCount() = runBlocking {
        val fixture = Fixture()
        fixture.reject = true
        val original = fixture.occurrence
        val result = fixture.processor().processAcceptedDelivery(original)
            as PersistentReminderEscalationResult.ScheduleRejected
        assertEquals(AlarmScheduleRejectionReason.SCHEDULE_FAILED, result.reason)
        assertEquals(ScheduledOccurrenceStatus.SCHEDULED, fixture.occurrence.status)
        assertEquals(fixture.chain!!.nextRealertAt, fixture.occurrence.fireAt)
        assertTrue(fixture.processor().processAcceptedDelivery(original)
            is PersistentReminderEscalationResult.Stale)
        assertEquals(1, fixture.scheduled.size)
        assertEquals(0, fixture.chain!!.realertsDelivered)
    }

    @Test
    fun unsuccessfulAtomicClaimNeverSchedules() = runBlocking {
        val fixture = Fixture()
        fixture.acceptTransition = false
        assertTrue(fixture.processor().processAcceptedDelivery(fixture.occurrence)
            is PersistentReminderEscalationResult.Stale)
        assertNull(fixture.chain)
        assertTrue(fixture.scheduled.isEmpty())
    }

    @Test
    fun missingCompletedAndNormalTasksCannotStartChain() = runBlocking {
        val fixture = Fixture()
        val originalTask = fixture.task!!
        for (task in listOf(null, originalTask.markCompleted(java.time.Instant.EPOCH),
            originalTask.withReminderPolicy(ReminderPolicy.normal()))) {
            fixture.task = task
            val result = fixture.processor().processAcceptedDelivery(fixture.occurrence)
            assertTrue(result is PersistentReminderEscalationResult.TaskMissing ||
                result is PersistentReminderEscalationResult.NotEligible)
            assertNull(fixture.chain)
            assertTrue(fixture.scheduled.isEmpty())
        }
    }

    @Test
    fun resolvedAndCancelledChainsCannotRestart() = runBlocking {
        for (status in listOf(PersistentReminderEscalationStatus.RESOLVED,
            PersistentReminderEscalationStatus.CANCELLED)) {
            val fixture = Fixture()
            fixture.chain = PersistentReminderEscalation.start(
                fixture.task!!.reminderPolicy, fixture.task!!.id, fixture.occurrence.id, fixture.clock.now(),
            ).copy(status = status, nextRealertAt = null)
            assertTrue(fixture.processor().processAcceptedDelivery(fixture.occurrence)
                is PersistentReminderEscalationResult.Inactive)
            assertTrue(fixture.scheduled.isEmpty())
        }
    }

    @Test
    fun usesStoredTimestampPrecisionForAcceptedDelivery() = runBlocking {
        val fixture = Fixture()
        val delivered = fixture.occurrence.copy(updatedAt = fixture.occurrence.updatedAt.plusNanos(123))
        assertTrue(fixture.processor().processAcceptedDelivery(delivered)
            is PersistentReminderEscalationResult.Scheduled)
    }

    @Test
    fun deliveryGateAndFreshProcessorsRejectOldGenerationAcrossRealerts() = runBlocking {
        val fixture = Fixture()
        fixture.occurrence = fixture.occurrence.copy(status = ScheduledOccurrenceStatus.SCHEDULED)
        val delivery = AlarmDelivery(fixture.occurrence.id, fixture.occurrence.generation)
        val accepted = AlarmDeliveryProcessor(fixture.occurrences, fixture.clock).process(delivery)
            as AlarmDeliveryResult.Accepted
        fixture.processor().processAcceptedDelivery(accepted.occurrence)
        assertTrue(AlarmDeliveryProcessor(fixture.occurrences, fixture.clock).process(delivery)
            is AlarmDeliveryResult.StaleGeneration)
        assertEquals(1, fixture.scheduled.size)
    }

    private class Fixture(
        policy: ReminderPolicy = ReminderPolicy.persistent(Duration.ofMinutes(5), 2),
    ) {
        val clock = FakeTimeProvider(Instant.parse("2026-09-07T09:00:00Z"))
        var task: Task? = Task(TaskId("task-1"), "Review plan", reminderPolicy = policy)
        var occurrence = ScheduledOccurrence(
            ScheduledOccurrenceId("occurrence-1"), TaskId("task-1"), clock.now(),
            status = ScheduledOccurrenceStatus.FIRED, createdAt = clock.now(),
        )
        var chain: PersistentReminderEscalation? = null
        var acceptTransition = true
        var exact = true
        var reject = false
        val scheduled = mutableListOf<ScheduledOccurrence>()

        val tasks = object : TaskRepository {
            override fun observeTasks() = flowOf(listOfNotNull(task))
            override fun observeTask(id: TaskId) = flowOf(task?.takeIf { it.id == id })
            override suspend fun getTask(id: TaskId) = task?.takeIf { it.id == id }
            override suspend fun saveTask(task: Task) { this@Fixture.task = task }
            override suspend fun getTasksForProject(projectId: com.dayloop.app.core.model.ProjectId): List<Task> = emptyList()
            override fun observeTasksForProject(projectId: com.dayloop.app.core.model.ProjectId): Flow<List<Task>> = flowOf(emptyList())
            override suspend fun deleteTask(id: TaskId): Boolean = error("Unused")
        }
        val occurrences = object : ScheduledOccurrenceRepository {
            override fun observeOccurrences() = flowOf(listOf(occurrence))
            override suspend fun getOccurrence(id: ScheduledOccurrenceId) = occurrence.takeIf { it.id == id }
            override suspend fun getOccurrencesForTask(taskId: TaskId) = listOf(occurrence).filter { it.taskId == taskId }
            override suspend fun getScheduledOccurrences() = listOf(occurrence).filter {
                it.status == ScheduledOccurrenceStatus.SCHEDULED
            }
            override suspend fun saveOccurrence(occurrence: ScheduledOccurrence) { this@Fixture.occurrence = occurrence }
            override suspend fun deleteOccurrence(id: ScheduledOccurrenceId): Boolean = error("Unused")
            override suspend fun deleteOccurrencesForTask(taskId: TaskId): Int = error("Unused")
        }
        val escalations = object : PersistentReminderEscalationRepository {
            override fun observeEscalations() = flowOf(listOfNotNull(chain))
            override suspend fun getEscalation(id: PersistentReminderEscalationId) = chain?.takeIf { it.id == id }
            override suspend fun getEscalationsForTask(taskId: TaskId) = listOfNotNull(chain).filter { it.taskId == taskId }
            override suspend fun getActiveEscalations() = listOfNotNull(chain).filter { it.isActive }
            override suspend fun saveEscalation(escalation: PersistentReminderEscalation) { chain = escalation }
            override suspend fun advanceAfterDelivery(
                delivered: ScheduledOccurrence,
                previous: PersistentReminderEscalation?,
                next: PersistentReminderEscalation,
                nextOccurrence: ScheduledOccurrence?,
            ): Boolean {
                if (!acceptTransition || occurrence != delivered || chain != previous) return false
                chain = next
                nextOccurrence?.let { occurrence = it }
                return true
            }
            override suspend fun deleteEscalation(id: PersistentReminderEscalationId): Boolean = error("Unused")
            override suspend fun deleteEscalationsForTask(taskId: TaskId): Int = error("Unused")
        }
        val scheduler = object : AlarmScheduler {
            override suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult {
                assertEquals(occurrence, this@Fixture.occurrence)
                assertEquals(occurrence.fireAt, chain!!.nextRealertAt)
                scheduled += occurrence
                return if (reject) AlarmScheduleResult.Rejected(occurrence.id, AlarmScheduleRejectionReason.SCHEDULE_FAILED)
                else AlarmScheduleResult.Scheduled(occurrence.id, exact)
            }
            override suspend fun cancel(occurrenceId: ScheduledOccurrenceId) = AlarmCancelResult.Cancelled(occurrenceId)
        }

        fun processor() = PersistentReminderEscalationProcessor(tasks, occurrences, escalations, scheduler, clock)

        fun fireNext() {
            clock.setNow(occurrence.fireAt)
            occurrence = occurrence.markFired(clock.now())
        }
    }
}
