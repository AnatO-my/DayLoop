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
class PersistentReminderEscalationDaoTest {
    private lateinit var database: DayLoopDatabase
    private lateinit var escalationDao: PersistentReminderEscalationDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            DayLoopDatabase::class.java,
        ).allowMainThreadQueries().build()
        escalationDao = database.persistentReminderEscalationDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndReadEscalationEntityRoundTripsChainColumns() = runBlocking {
        val escalation = baseEscalationEntity(
            id = "escalation-1",
            taskId = "task-1",
            nextRealertAtEpochMillis = 1_788_681_700_000L,
        )

        escalationDao.upsert(escalation)

        assertEquals(escalation, escalationDao.getById("escalation-1"))
        assertEquals(listOf(escalation), escalationDao.getForTask("task-1"))
        assertEquals(listOf(escalation), escalationDao.getActive())
        assertEquals(listOf(escalation), escalationDao.observeAll().first())
    }

    @Test
    fun inactiveEscalationIsExcludedFromActiveQuery() = runBlocking {
        val active = baseEscalationEntity(id = "escalation-1", status = "ACTIVE")
        val resolved = baseEscalationEntity(
            id = "escalation-2",
            nextRealertAtEpochMillis = null,
            status = "RESOLVED",
            updatedAtEpochMillis = 1_788_681_000_000L,
        )

        escalationDao.upsert(active)
        escalationDao.upsert(resolved)

        assertEquals(listOf(active), escalationDao.getActive())
    }

    @Test
    fun deleteForTaskRemovesOnlyMatchingEscalations() = runBlocking {
        val first = baseEscalationEntity(id = "escalation-1", taskId = "task-1")
        val second = baseEscalationEntity(id = "escalation-2", taskId = "task-1")
        val otherTask = baseEscalationEntity(id = "escalation-3", taskId = "task-2")

        escalationDao.upsert(first)
        escalationDao.upsert(second)
        escalationDao.upsert(otherTask)

        assertEquals(2, escalationDao.deleteForTask("task-1"))
        assertEquals(emptyList<PersistentReminderEscalationEntity>(), escalationDao.getForTask("task-1"))
        assertEquals(listOf(otherTask), escalationDao.getForTask("task-2"))
    }

    @Test
    fun deleteByIdRemovesOnlyMatchingEscalation() = runBlocking {
        val first = baseEscalationEntity(id = "escalation-1", taskId = "task-1")
        val second = baseEscalationEntity(id = "escalation-2", taskId = "task-2")

        escalationDao.upsert(first)
        escalationDao.upsert(second)

        assertEquals(1, escalationDao.deleteById("escalation-1"))
        assertNull(escalationDao.getById("escalation-1"))
        assertEquals(listOf(second), escalationDao.observeAll().first())
    }

    private fun baseEscalationEntity(
        id: String = "escalation-1",
        taskId: String = "task-1",
        sourceOccurrenceId: String = "occurrence-1",
        nextRealertAtEpochMillis: Long? = 1_788_681_700_000L,
        realertIntervalMillis: Long = 15 * 60 * 1000L,
        maximumRealerts: Int = 3,
        realertsDelivered: Int = 0,
        status: String = "ACTIVE",
        createdAtEpochMillis: Long = 1_788_680_800_000L,
        updatedAtEpochMillis: Long = createdAtEpochMillis,
    ): PersistentReminderEscalationEntity = PersistentReminderEscalationEntity(
        id = id,
        taskId = taskId,
        sourceOccurrenceId = sourceOccurrenceId,
        nextRealertAtEpochMillis = nextRealertAtEpochMillis,
        realertIntervalMillis = realertIntervalMillis,
        maximumRealerts = maximumRealerts,
        realertsDelivered = realertsDelivered,
        status = status,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}
