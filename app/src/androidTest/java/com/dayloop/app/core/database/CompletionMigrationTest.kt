package com.dayloop.app.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.RoomAlarmStateStore
import com.dayloop.app.repository.RoomTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CompletionMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), DayLoopDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun preservesLegacyRowsAndPersistsNewCompletionTimes() = runBlocking {
        val name = "completion-migration-test"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        helper.createDatabase(name, 9).apply {
            for (status in listOf("ACTIVE", "COMPLETED")) {
                execSQL("""INSERT INTO tasks
                    (id, title, status, priority, reminder_mode, maximum_realerts, snooze_allowed, reschedule_allowed)
                    VALUES ('$status', 'Read', '$status', 'NORMAL', 'NORMAL', 0, 1, 1)""")
            }
            close()
        }
        val db = Room.databaseBuilder(context, DayLoopDatabase::class.java, name)
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14).build()
        try {
            val repository = RoomTaskRepository(db.taskDao())
            val store = RoomAlarmStateStore(db)
            val legacy = requireNotNull(db.taskDao().getById("COMPLETED"))
            assertEquals("COMPLETED", legacy.status)
            assertNull(legacy.completedAt)
            assertNull(requireNotNull(db.taskDao().getById("ACTIVE")).completedAt)
            val id = TaskId("ACTIVE")
            val at = Instant.parse("2026-09-20T23:30:00Z")
            store.completeTask(id, at)
            store.completeTask(id, at.plusSeconds(60))
            val completed = requireNotNull(repository.getTask(id))
            assertEquals(at, completed.completedAt)
            store.saveTask(completed.copy(title = "Read more"), at.plusSeconds(120))
            assertEquals(at, repository.getTask(id)?.completedAt)
            store.saveTask(completed.reopen(), at.plusSeconds(180))
            assertNull(repository.getTask(id)?.completedAt)
            store.completeTask(id, at.plusSeconds(240))
            assertEquals(at.plusSeconds(240), repository.getTask(id)?.completedAt)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
