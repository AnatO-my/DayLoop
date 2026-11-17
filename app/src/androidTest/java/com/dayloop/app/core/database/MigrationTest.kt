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

    @Test
    @Throws(IOException::class)
    fun migrate8To11() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL("INSERT INTO tasks (id, title, status, priority) VALUES ('task-8', 'Task 8', 'ACTIVE', 'NORMAL')")
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11
        )

        helper.createDatabase(TEST_DB, 11).apply {
            val cursor = query("SELECT * FROM tasks WHERE id = 'task-8'")
            assert(cursor.moveToFirst())
            assert(cursor.getColumnIndex("subtasks_json") != -1)
            assert(cursor.getColumnIndex("completed_at") != -1)
            assert(cursor.getColumnIndex("recurrence_time") != -1)
            cursor.close()
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate11To12() {
        helper.createDatabase(TEST_DB, 11).apply {
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12)

        helper.createDatabase(TEST_DB, 12).apply {
            // Verify new tables exist
            query("SELECT * FROM clothing_items").close()
            query("SELECT * FROM outfit_records").close()
            query("SELECT * FROM outfit_items").close()
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate12To13() {
        helper.createDatabase(TEST_DB, 12).apply {
            execSQL("INSERT INTO clothing_items (id, name, category) VALUES ('item-1', 'Shirt', 'TOP')")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 13, true, MIGRATION_12_13)

        helper.createDatabase(TEST_DB, 13).apply {
            val cursor = query("SELECT * FROM clothing_items WHERE id = 'item-1'")
            assert(cursor.moveToFirst())
            val archivedIdx = cursor.getColumnIndex("is_archived")
            assert(archivedIdx != -1)
            // Default should be 0 (false)
            assert(cursor.getInt(archivedIdx) == 0)
            cursor.close()
            close()
        }
    }

    /** Authored 2026-11-10; needs schema 18.json from a build, not yet run. */
    @Test
    @Throws(IOException::class)
    fun migrate17To18() {
        helper.createDatabase(TEST_DB, 17).apply {
            execSQL("INSERT INTO clothing_items (id, name, category, is_archived) VALUES ('item-1', 'Shirt', 'TOP', 0)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 18, true, MIGRATION_17_18).apply {
            // Existing wardrobe rows survive and start without details.
            val items = query("SELECT name FROM clothing_items WHERE id = 'item-1'")
            assert(items.moveToFirst() && items.getString(0) == "Shirt")
            items.close()
            val details = query("SELECT COUNT(*) FROM clothing_item_attributes")
            assert(details.moveToFirst() && details.getInt(0) == 0)
            details.close()
            execSQL("INSERT INTO clothing_item_attributes (item_id, garment_type, user_corrected, updated_at) VALUES ('item-1', 'SHIRT', 0, 1)")
            close()
        }
    }

    /** Authored 2026-11-10; needs schema 19.json from a build, not yet run. */
    @Test
    @Throws(IOException::class)
    fun migrate18To19() {
        helper.createDatabase(TEST_DB, 18).apply {
            execSQL("INSERT INTO clothing_item_attributes (item_id, garment_type, user_corrected, updated_at) VALUES ('item-1', 'SHIRT', 0, 1)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 19, true, MIGRATION_18_19).apply {
            val details = query("SELECT garment_type FROM clothing_item_attributes WHERE item_id = 'item-1'")
            assert(details.moveToFirst() && details.getString(0) == "SHIRT")
            details.close()
            execSQL("INSERT INTO outfit_feedback (id, date, item_ids, verdict, created_at) VALUES ('f1', '2026-11-10', 'item-1', 'NOT_FOR_ME', 1)")
            val feedback = query("SELECT COUNT(*) FROM outfit_feedback")
            assert(feedback.moveToFirst() && feedback.getInt(0) == 1)
            feedback.close()
            close()
        }
    }
}
