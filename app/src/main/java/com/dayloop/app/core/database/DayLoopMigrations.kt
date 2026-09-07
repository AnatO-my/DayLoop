package com.dayloop.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scheduled_occurrences` (
                `id` TEXT NOT NULL,
                `task_id` TEXT NOT NULL,
                `fire_at_epoch_millis` INTEGER NOT NULL,
                `generation` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_scheduled_occurrences_task_id` ON `scheduled_occurrences` (`task_id`)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_scheduled_occurrences_status_fire_at_epoch_millis`
            ON `scheduled_occurrences` (`status`, `fire_at_epoch_millis`)
            """.trimIndent(),
        )
    }
}
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `alarm_diagnostic_entries` (
                `id` TEXT NOT NULL,
                `occurrence_id` TEXT,
                `task_id` TEXT,
                `event` TEXT NOT NULL,
                `exact` INTEGER,
                `message` TEXT,
                `created_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_alarm_diagnostic_entries_occurrence_id` ON `alarm_diagnostic_entries` (`occurrence_id`)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_alarm_diagnostic_entries_created_at_epoch_millis`
            ON `alarm_diagnostic_entries` (`created_at_epoch_millis`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `persistent_reminder_escalations` (
                `id` TEXT NOT NULL,
                `task_id` TEXT NOT NULL,
                `source_occurrence_id` TEXT NOT NULL,
                `next_realert_at_epoch_millis` INTEGER,
                `realert_interval_millis` INTEGER NOT NULL,
                `maximum_realerts` INTEGER NOT NULL,
                `realerts_delivered` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_persistent_reminder_escalations_task_id` ON `persistent_reminder_escalations` (`task_id`)")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_persistent_reminder_escalations_source_occurrence_id`
            ON `persistent_reminder_escalations` (`source_occurrence_id`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_persistent_reminder_escalations_status_next_realert_at_epoch_millis`
            ON `persistent_reminder_escalations` (`status`, `next_realert_at_epoch_millis`)
            """.trimIndent(),
        )
    }
}
