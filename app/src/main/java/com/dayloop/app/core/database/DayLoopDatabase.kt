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
        ClothingItemEntity::class,
        OutfitEntity::class,
        OutfitItemCrossRef::class,
        WardrobeMediaEntity::class,
        ClothingMediaEntity::class,
        OutfitMediaEntity::class,
        ClothingAttributesEntity::class,
        OutfitFeedbackEntity::class,
        WorkoutRoutineEntity::class,
        NoteEntity::class,
    ],
    version = 21,
    exportSchema = true,
)
abstract class DayLoopDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun scheduledOccurrenceDao(): ScheduledOccurrenceDao

    abstract fun alarmDiagnosticEntryDao(): AlarmDiagnosticEntryDao

    abstract fun persistentReminderEscalationDao(): PersistentReminderEscalationDao

    abstract fun goalDao(): GoalDao

    abstract fun projectDao(): ProjectDao

    abstract fun clothingDao(): ClothingDao

    abstract fun outfitDao(): OutfitDao

    abstract fun wardrobeMediaDao(): WardrobeMediaDao

    abstract fun clothingAttributesDao(): ClothingAttributesDao

    abstract fun outfitFeedbackDao(): OutfitFeedbackDao

    abstract fun workoutRoutineDao(): WorkoutRoutineDao

    abstract fun noteDao(): NoteDao
}
