package com.dayloop.app.di

import android.content.Context
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.core.backup.BackupManager
import com.dayloop.app.core.calendar.CalendarContractManager
import com.dayloop.app.core.database.ClothingDao
import com.dayloop.app.core.database.OutfitDao
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.AndroidCalendarSyncRepository
import com.dayloop.app.repository.CalendarSyncRepository
import com.dayloop.app.repository.NoteRepository
import com.dayloop.app.repository.OutfitRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.RoomNoteRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.repository.WardrobeMediaRepository
import com.dayloop.app.time.TimeProvider
import com.dayloop.app.today.TodayAssistant
import com.dayloop.app.today.NaturalIntentParser

interface AppGraph {
    val applicationContext: Context
    val timeProvider: TimeProvider
    val taskRepository: TaskRepository
    val scheduledOccurrenceRepository: ScheduledOccurrenceRepository
    val persistentReminderEscalationRepository: PersistentReminderEscalationRepository
    val alarmDiagnosticsRepository: AlarmDiagnosticsRepository
    val alarmScheduler: AlarmScheduler
    val settingsRepository: com.dayloop.app.settings.SettingsRepository
        get() = com.dayloop.app.settings.AppSettingsStore.get(applicationContext)
    val reminderNotifications: com.dayloop.app.core.notifications.ReminderNotifications
        get() = error("Notification services are not configured")
    val alarmCoordinator: com.dayloop.app.core.alarms.AlarmCoordinator
        get() = error("Alarm coordinator is not configured")
    val scheduleReconciler: com.dayloop.app.core.reconciliation.ScheduleReconciler
        get() = error("Schedule reconciler is not configured")
    val capabilityRepairFlow: com.dayloop.app.core.notifications.CapabilityRepairFlow
        get() = error("Capability repair flow is not configured")
    val widgetStateReconciler: com.dayloop.app.widget.WidgetStateReconciler
        get() = error("Widget state reconciler is not configured")
    val goalRepository: com.dayloop.app.repository.GoalRepository
    val projectRepository: com.dayloop.app.repository.ProjectRepository
    val workoutRoutineRepository: com.dayloop.app.repository.WorkoutRoutineRepository
        get() = error("Workout routines are not configured")
    val outfitRepository: OutfitRepository
    val wardrobeMediaRepository: WardrobeMediaRepository
    val noteRepository: NoteRepository
        get() = RoomNoteRepository(error("NoteDao not provided"))
    val calendarSyncRepository: CalendarSyncRepository
        get() = AndroidCalendarSyncRepository(
            CalendarContractManager(applicationContext)
        )
    val wardrobeAttributesRepository: com.dayloop.app.repository.WardrobeAttributesRepository
        get() = error("Wardrobe attributes are not configured")
    val outfitFeedbackRepository: com.dayloop.app.repository.OutfitFeedbackRepository
        get() = error("Outfit feedback is not configured")
    val garmentRecognizer: com.dayloop.app.core.outfit.recognition.GarmentRecognizer
        get() = error("Garment recognition is not configured")
    val clothingDao: ClothingDao
    val outfitDao: OutfitDao
    val todayAssistant: TodayAssistant
        get() = TodayAssistant()
    val backupManager: BackupManager
        get() = BackupManager(this)
    val naturalIntentParser: NaturalIntentParser
        get() = NaturalIntentParser()
}
