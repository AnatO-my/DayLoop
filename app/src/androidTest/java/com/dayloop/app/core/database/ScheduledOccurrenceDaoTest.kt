package com.dayloop.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledOccurrenceDaoTest {
    private lateinit var database: DayLoopDatabase
    private lateinit var scheduledOccurrenceDao: ScheduledOccurrenceDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            DayLoopDatabase::class.java,
        ).allowMainThreadQueries().build()
        scheduledOccurrenceDao = database.scheduledOccurrenceDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndReadOccurrenceEntityRoundTripsAlarmIdentityColumns() = runBlocking {
        val occurrence = baseOccurrenceEntity(
            id = "occurrence-1",
            taskId = "task-1",
            fireAtEpochMillis = 1_788_680_800_000L,
            generation = 2L,
        )

        scheduledOccurrenceDao.upsert(occurrence)

        assertEquals(occurrence, scheduledOccurrenceDao.getById("occurrence-1"))
        assertEquals(listOf(occurrence), scheduledOccurrenceDao.getForTask("task-1"))
        assertEquals(listOf(occurrence), scheduledOccurrenceDao.getScheduled())
        assertEquals(listOf(occurrence), scheduledOccurrenceDao.observeAll().first())
    }

    @Test
    fun upsertReplacesExistingOccurrenceEntity() = runBlocking {
        val original = baseOccurrenceEntity(id = "occurrence-1", status = "SCHEDULED")
        val updated = baseOccurrenceEntity(id = "occurrence-1", status = "FIRED", updatedAtEpochMillis = 1_788_681_100_000L)

        scheduledOccurrenceDao.upsert(original)
        scheduledOccurrenceDao.upsert(updated)

        assertEquals(updated, scheduledOccurrenceDao.getById("occurrence-1"))
        assertEquals(emptyList<ScheduledOccurrenceEntity>(), scheduledOccurrenceDao.getScheduled())
    }

    @Test
    fun deleteByIdRemovesOnlyMatchingOccurrenceEntity() = runBlocking {
        val first = baseOccurrenceEntity(id = "occurrence-1", taskId = "task-1")
        val second = baseOccurrenceEntity(id = "occurrence-2", taskId = "task-2")

        scheduledOccurrenceDao.upsert(first)
        scheduledOccurrenceDao.upsert(second)

        assertEquals(1, scheduledOccurrenceDao.deleteById("occurrence-1"))
        assertNull(scheduledOccurrenceDao.getById("occurrence-1"))
        assertEquals(listOf(second), scheduledOccurrenceDao.observeAll().first())
    }

    @Test
    fun deleteForTaskRemovesOnlyMatchingTaskOccurrences() = runBlocking {
        val first = baseOccurrenceEntity(id = "occurrence-1", taskId = "task-1")
        val second = baseOccurrenceEntity(id = "occurrence-2", taskId = "task-1")
        val otherTask = baseOccurrenceEntity(id = "occurrence-3", taskId = "task-2")

        scheduledOccurrenceDao.upsert(first)
        scheduledOccurrenceDao.upsert(second)
        scheduledOccurrenceDao.upsert(otherTask)

        assertEquals(2, scheduledOccurrenceDao.deleteForTask("task-1"))
        assertEquals(emptyList<ScheduledOccurrenceEntity>(), scheduledOccurrenceDao.getForTask("task-1"))
        assertEquals(listOf(otherTask), scheduledOccurrenceDao.getForTask("task-2"))
    }

    private fun baseOccurrenceEntity(
        id: String = "occurrence-1",
        taskId: String = "task-1",
        fireAtEpochMillis: Long = 1_788_680_800_000L,
        generation: Long = 1L,
        status: String = "SCHEDULED",
        createdAtEpochMillis: Long = 1_788_677_200_000L,
        updatedAtEpochMillis: Long = createdAtEpochMillis,
    ): ScheduledOccurrenceEntity = ScheduledOccurrenceEntity(
        id = id,
        taskId = taskId,
        fireAtEpochMillis = fireAtEpochMillis,
        generation = generation,
        status = status,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}