package com.dayloop.app.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.RoomTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DescriptionMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), DayLoopDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun preservesExistingTaskAndDescriptionAfterReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "description-migration-test"
        context.deleteDatabase(name)
        helper.createDatabase(name, 13).apply {
            execSQL("""INSERT INTO tasks
                (id, title, status, priority, reminder_mode, maximum_realerts, snooze_allowed, reschedule_allowed)
                VALUES ('existing', 'Read', 'ACTIVE', 'NORMAL', 'NORMAL', 0, 1, 1)""")
            close()
        }
        fun open() = Room.databaseBuilder(context, DayLoopDatabase::class.java, name)
            .addMigrations(MIGRATION_13_14).build()
        var db = open()
        try {
            val id = TaskId("existing")
            val repository = RoomTaskRepository(db.taskDao())
            val legacy = requireNotNull(repository.getTask(id))
            assertEquals("Read", legacy.title)
            assertNull(legacy.description)
            repository.saveTask(legacy.copy(description = "First line\nSecond line"))
            db.close()
            db = open()
            val reopened = RoomTaskRepository(db.taskDao())
            assertEquals("First line\nSecond line", reopened.getTask(id)?.description)
            reopened.saveTask(requireNotNull(reopened.getTask(id)).copy(description = null))
            assertNull(reopened.getTask(id)?.description)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
