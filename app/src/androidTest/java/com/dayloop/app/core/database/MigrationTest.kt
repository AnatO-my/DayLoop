package com.dayloop.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DayLoopDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            execSQL("INSERT INTO tasks (id, title, status, priority) VALUES ('task-1', 'Task 1', 'ACTIVE', 'NORMAL')")
            close()
        }

        // Re-open the database with version 2 and provide MIGRATION_1_2
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To8() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO tasks (id, title, status, priority) VALUES ('task-1', 'Task 1', 'ACTIVE', 'NORMAL')")
            close()
        }

        // Should run all intermediate migrations
        helper.runMigrationsAndValidate(
            TEST_DB, 
            8, 
            true, 
            MIGRATION_4_5, 
            MIGRATION_5_6, 
            MIGRATION_6_7, 
            MIGRATION_7_8
        )
        
        // Verify data survived
        helper.createDatabase(TEST_DB, 8).apply {
            val cursor = query("SELECT * FROM tasks WHERE id = 'task-1'")
            assert(cursor.moveToFirst())
            assert(cursor.getString(cursor.getColumnIndex("title")) == "Task 1")
            // Verify new columns exist
            assert(cursor.getColumnIndex("project_id") != -1)
            cursor.close()
            close()
        }
    }
}
