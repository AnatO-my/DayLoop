package com.dayloop.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        ScheduledOccurrenceEntity::class,
        AlarmDiagnosticEntryEntity::class,
        PersistentReminderEscalationEntity::class,
        GoalEntity::class,
        ProjectEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class DayLoopDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun scheduledOccurrenceDao(): ScheduledOccurrenceDao

    abstract fun alarmDiagnosticEntryDao(): AlarmDiagnosticEntryDao

    abstract fun persistentReminderEscalationDao(): PersistentReminderEscalationDao

    abstract fun goalDao(): GoalDao

    abstract fun projectDao(): ProjectDao
}
