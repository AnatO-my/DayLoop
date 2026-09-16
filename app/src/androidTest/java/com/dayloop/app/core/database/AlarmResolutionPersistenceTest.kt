package com.dayloop.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayloop.app.core.alarms.*
import com.dayloop.app.core.model.*
import com.dayloop.app.repository.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmResolutionPersistenceTest {
    private lateinit var database: DayLoopDatabase
    private lateinit var store: RoomAlarmStateStore
    private val now = Instant.parse("2026-09-07T09:00:00Z")
    private val task = Task(TaskId("resolution-task"), "Resolve reminder",
        reminderPolicy = ReminderPolicy.strict(Duration.ofMinutes(5), 3))
    private val occurrence = ScheduledOccurrence(ScheduledOccurrenceId("resolution-occurrence"),
        task.id, now, createdAt = now)
    private val chain = PersistentReminderEscalation.start(task.reminderPolicy, task.id, occurrence.id, now)

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            DayLoopDatabase::class.java).build()
        store = RoomAlarmStateStore(database)
        store.saveTask(task, now)
        RoomScheduledOccurrenceRepository(database.scheduledOccurrenceDao()).saveOccurrence(occurrence)
        RoomPersistentReminderEscalationRepository(database).saveEscalation(chain)
    }

    @After fun close() { database.close() }

    @Test fun completeStopsChainAndDuplicateIsStale() = runBlocking {
        val command = AlarmNotificationCommand(AlarmNotificationAction.COMPLETE, AlarmDelivery(occurrence.id, 1))
        val result = store.act(command, now, Duration.ofMinutes(10))
        assertTrue(result.result is AlarmNotificationActionResult.Completed)
        assertEquals(TaskStatus.COMPLETED, RoomTaskRepository(database.taskDao()).getTask(task.id)!!.status)
        assertTrue(RoomPersistentReminderEscalationRepository(database).getActiveEscalations().isEmpty())
        assertTrue(RoomScheduledOccurrenceRepository(database.scheduledOccurrenceDao()).getScheduledOccurrences().isEmpty())
        assertTrue(store.act(command, now, Duration.ofMinutes(10)).result is AlarmNotificationActionResult.Stale)
    }

    @Test fun strictSnoozeDoesNotChangeEitherRecord() = runBlocking {
        val result = store.act(AlarmNotificationCommand(AlarmNotificationAction.SNOOZE,
            AlarmDelivery(occurrence.id, 1)), now, Duration.ofMinutes(10))
        assertTrue(result.result is AlarmNotificationActionResult.ActionNotAllowed)
        assertEquals(chain, RoomPersistentReminderEscalationRepository(database).getEscalation(chain.id))
        assertEquals(occurrence, RoomScheduledOccurrenceRepository(database.scheduledOccurrenceDao()).getOccurrence(occurrence.id))
    }

    @Test fun scheduleEditStopsOldChainAndInvalidatesOldDelivery() = runBlocking {
        store.saveTask(task.copy(schedule = TaskSchedule.Deadline(LocalDateTime.parse("2026-09-08T09:00:00"))), now)
        assertTrue(RoomPersistentReminderEscalationRepository(database).getActiveEscalations().isEmpty())
        assertTrue(store.accept(AlarmDelivery(occurrence.id, 1), now) is AlarmDeliveryResult.StaleGeneration)
    }

    @Test fun firedFinalReminderCanStillBeCompleted() = runBlocking {
        store.accept(AlarmDelivery(occurrence.id, 1), now)
        assertTrue(store.act(AlarmNotificationCommand(AlarmNotificationAction.COMPLETE,
            AlarmDelivery(occurrence.id, 1)), now, Duration.ofMinutes(10)).result is AlarmNotificationActionResult.Completed)
    }
}
