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

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `target_date` TEXT,
                `progress_policy` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` TEXT NOT NULL,
                `goal_id` TEXT,
                `title` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `project_id` TEXT")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `provenance_origin_type` TEXT")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `provenance_rule_id` TEXT")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `provenance_version` INTEGER")
    }
}

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_type` TEXT")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_active_days` INTEGER")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_rest_days` INTEGER")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_anchor_date` TEXT")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_task_title` TEXT")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `protocol_task_priority` TEXT")
    }
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `goals` ADD COLUMN `manual_progress` REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `subtasks_json` TEXT")
    }
}

val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Unknown historic completion dates stay null; schedules are not completion evidence.
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `completed_at` INTEGER")
    }
}

val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrence_time` TEXT")
    }
}

val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `clothing_items` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `color_hex` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `outfit_records` (
                `date` TEXT NOT NULL,
                PRIMARY KEY(`date`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `outfit_items` (
                `date` TEXT NOT NULL,
                `clothing_item_id` TEXT NOT NULL,
                PRIMARY KEY(`date`, `clothing_item_id`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_13_14: Migration = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `description` TEXT")
    }
}

val MIGRATION_12_13: Migration = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `clothing_items` ADD COLUMN `is_archived` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_14_15: Migration = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wardrobe_media` (
                `id` TEXT NOT NULL,
                `file_name` TEXT NOT NULL,
                `mime_type` TEXT NOT NULL,
                `width` INTEGER NOT NULL,
                `height` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `clothing_item_media` (
                `clothing_item_id` TEXT NOT NULL,
                `media_id` TEXT NOT NULL,
                `is_primary` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`clothing_item_id`, `media_id`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_15_16: Migration = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `outfit_media` (
                `date` TEXT NOT NULL,
                `media_id` TEXT NOT NULL,
                PRIMARY KEY(`date`, `media_id`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_16_17: Migration = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `wardrobe_media` ADD COLUMN `labels_json` TEXT")
    }
}

/** Additive: optional per-item details. Existing tables and rows are not touched. */
val MIGRATION_17_18: Migration = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `clothing_item_attributes` (
                `item_id` TEXT NOT NULL,
                `garment_type` TEXT,
                `secondary_color_hex` TEXT,
                `pattern` TEXT,
                `material` TEXT,
                `fit` TEXT,
                `warmth` TEXT,
                `occasions` TEXT,
                `model_id` TEXT,
                `model_version` TEXT,
                `type_confidence` REAL,
                `user_corrected` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`item_id`)
            )
            """.trimIndent()
        )
    }
}

/** Additive: what the user did with suggested outfits. Existing tables are not touched. */
val MIGRATION_18_19: Migration = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `outfit_feedback` (
                `id` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `item_ids` TEXT NOT NULL,
                `verdict` TEXT NOT NULL,
                `reason` TEXT,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

/** Additive routine content. Routines stay local planning data and never schedule alarms by themselves. */
val MIGRATION_19_20: Migration = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_routines` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `exercises_payload` TEXT NOT NULL,
                `scheduled_days` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

/** Commercial Note Engine addition (version 20 -> 21) */
val MIGRATION_20_21: Migration = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `is_pinned` INTEGER NOT NULL,
                `is_archived` INTEGER NOT NULL,
                `linked_task_id` TEXT,
                `linked_project_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `tags` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_is_pinned` ON `notes` (`is_pinned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updated_at` ON `notes` (`updated_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_linked_task_id` ON `notes` (`linked_task_id`)")
    }
}

