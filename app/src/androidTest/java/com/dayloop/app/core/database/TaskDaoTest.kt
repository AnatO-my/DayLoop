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
class TaskDaoTest {
    private lateinit var database: DayLoopDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            DayLoopDatabase::class.java,
        ).allowMainThreadQueries().build()
        taskDao = database.taskDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndReadTaskEntityRoundTripsScheduleAndReminderColumns() = runBlocking {
        val task = TaskEntity(
            id = "task-1",
            title = "Hydrate",
            status = "ACTIVE",
            priority = "HIGH",
            scheduleKind = "INTERVAL",
            intervalEveryMillis = 90 * 60 * 1000L,
            intervalWindowStartsAt = "09:00",
            intervalWindowEndsAt = "17:00",
            intervalEligibleDays = "MONDAY,WEDNESDAY",
            intervalStartsOn = "2026-08-31",
            intervalEndsOn = "2026-09-30",
            intervalAnchor = "COMPLETION",
            reminderMode = "PERSISTENT",
            realertIntervalMillis = 15 * 60 * 1000L,
            maximumRealerts = 3,
            snoozeAllowed = true,
            rescheduleAllowed = false,
        )

        taskDao.upsert(task)

        assertEquals(task, taskDao.getById("task-1"))
        assertEquals(listOf(task), taskDao.getAll())
        assertEquals(listOf(task), taskDao.observeAll().first())
    }

    @Test
    fun upsertReplacesExistingTaskEntity() = runBlocking {
        val original = baseTaskEntity(title = "Original")
        val updated = baseTaskEntity(title = "Updated", status = "COMPLETED")

        taskDao.upsert(original)
        taskDao.upsert(updated)

        assertEquals(updated, taskDao.getById("task-1"))
        assertEquals(updated, taskDao.observeById("task-1").first())
        assertEquals(listOf(updated), taskDao.getAll())
    }

    @Test
    fun deleteByIdRemovesOnlyMatchingTaskEntity() = runBlocking {
        val first = baseTaskEntity(id = "task-1")
        val second = baseTaskEntity(id = "task-2", title = "Second")

        taskDao.upsert(first)
        taskDao.upsert(second)

        assertEquals(1, taskDao.deleteById("task-1"))
        assertNull(taskDao.getById("task-1"))
        assertEquals(listOf(second), taskDao.getAll())
    }

    private fun baseTaskEntity(
        id: String = "task-1",
        title: String = "Plan the day",
        status: String = "ACTIVE",
    ): TaskEntity = TaskEntity(
        id = id,
        title = title,
        status = status,
        priority = "NORMAL",
        scheduleKind = "ALL_DAY",
        allDayDate = "2026-08-31",
    )
}
