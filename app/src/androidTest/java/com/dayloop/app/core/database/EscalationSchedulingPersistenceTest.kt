package com.dayloop.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayloop.app.core.alarms.PersistentReminderEscalation
import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.alarms.ScheduledOccurrenceStatus
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.RoomPersistentReminderEscalationRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EscalationSchedulingPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "escalation-test-" + UUID.randomUUID() + ".db"
    private lateinit var database: DayLoopDatabase
    private val now = Instant.parse("2026-09-07T09:00:00Z")
    private val delivered = ScheduledOccurrence(
        ScheduledOccurrenceId("occurrence-1"), TaskId("task-1"), now,
        status = ScheduledOccurrenceStatus.FIRED, createdAt = now,
    )
    private val chain = PersistentReminderEscalation.start(
        ReminderPolicy.persistent(Duration.ofMinutes(5), 2), delivered.taskId, delivered.id, now,
    )
    private val successor = delivered.copy(
        generation = 2, fireAt = requireNotNull(chain.nextRealertAt),
        status = ScheduledOccurrenceStatus.SCHEDULED,
    )

    @Before
    fun openDatabase() {
        database = Room.databaseBuilder(context, DayLoopDatabase::class.java, databaseName).build()
    }

    @After
    fun cleanUp() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun committedChainAndSuccessorSurviveDatabaseReopen() = runBlocking {
        insertDelivered()
        assertTrue(repository().advanceAfterDelivery(delivered, null, chain, successor))
        database.close()
        openDatabase()

        assertEquals(chain, repository().getEscalation(chain.id))
        val saved = database.scheduledOccurrenceDao().getById(delivered.id.value)!!
        assertEquals(2L, saved.generation)
        assertEquals(successor.fireAt.toEpochMilli(), saved.fireAtEpochMillis)
        assertEquals("SCHEDULED", saved.status)
        assertFalse(repository().advanceAfterDelivery(delivered, null, chain, successor))
        assertEquals(1, repository().getEscalationsForTask(delivered.taskId).size)
    }

    @Test
    fun failureWritingSuccessorRollsBackChainWrite() = runBlocking {
        insertDelivered()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_successor BEFORE INSERT ON scheduled_occurrences " +
                "BEGIN SELECT RAISE(ABORT, 'test write failure'); END",
        )
        val result = runCatching {
            repository().advanceAfterDelivery(delivered, null, chain, successor)
        }
        assertTrue(result.isFailure)
        assertNull(repository().getEscalation(chain.id))
        assertEquals(1L, database.scheduledOccurrenceDao().getById(delivered.id.value)!!.generation)
    }

    @Test
    fun concurrentlyResolvedChainRejectsStaleTransition() = runBlocking {
        insertDelivered()
        repository().saveEscalation(chain.resolve(now))
        assertFalse(repository().advanceAfterDelivery(
            delivered, chain, chain.recordRealertDelivered(now), successor,
        ))
        assertTrue(database.scheduledOccurrenceDao().getScheduled().isEmpty())
    }

    @Test
    fun staleOccurrenceCannotWriteEitherRecord() = runBlocking {
        insertDelivered()
        assertFalse(repository().advanceAfterDelivery(
            delivered.copy(generation = 2), null, chain, successor,
        ))
        assertNull(repository().getEscalation(chain.id))
        assertTrue(database.scheduledOccurrenceDao().getScheduled().isEmpty())
    }

    private fun repository() = RoomPersistentReminderEscalationRepository(database)

    private suspend fun insertDelivered() {
        database.scheduledOccurrenceDao().upsert(ScheduledOccurrenceEntity(
            id = delivered.id.value,
            taskId = delivered.taskId.value,
            fireAtEpochMillis = now.toEpochMilli(),
            generation = 1,
            status = "FIRED",
            createdAtEpochMillis = now.toEpochMilli(),
            updatedAtEpochMillis = now.toEpochMilli(),
        ))
    }
}

