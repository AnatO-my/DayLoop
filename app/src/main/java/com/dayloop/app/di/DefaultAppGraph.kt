package com.dayloop.app.di

import android.content.Context
import androidx.room.Room
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.core.alarms.AndroidAlarmScheduler
import com.dayloop.app.core.database.DayLoopDatabase
import com.dayloop.app.core.database.MIGRATION_1_2
import com.dayloop.app.core.database.MIGRATION_2_3
import com.dayloop.app.core.database.MIGRATION_3_4
import com.dayloop.app.core.database.MIGRATION_4_5
import com.dayloop.app.core.database.MIGRATION_5_6
import com.dayloop.app.core.database.MIGRATION_6_7
import com.dayloop.app.core.database.MIGRATION_7_8
import com.dayloop.app.core.database.MIGRATION_8_9
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.RoomAlarmDiagnosticsRepository
import com.dayloop.app.repository.RoomPersistentReminderEscalationRepository
import com.dayloop.app.repository.RoomScheduledOccurrenceRepository
import com.dayloop.app.repository.RoomTaskRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.SystemTimeProvider
import com.dayloop.app.time.TimeProvider

class DefaultAppGraph(
    applicationContext: Context,
) : AppGraph {
    override val applicationContext: Context = applicationContext.applicationContext
    override val timeProvider: TimeProvider = SystemTimeProvider()

    private val database: DayLoopDatabase by lazy {
        Room.databaseBuilder(
            context = this.applicationContext,
            klass = DayLoopDatabase::class.java,
            name = DATABASE_NAME,
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()
    }

    override val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.taskDao())
    }

    override val scheduledOccurrenceRepository: ScheduledOccurrenceRepository by lazy {
        RoomScheduledOccurrenceRepository(database.scheduledOccurrenceDao())
    }

    override val persistentReminderEscalationRepository: PersistentReminderEscalationRepository by lazy {
        RoomPersistentReminderEscalationRepository(database)
    }

    override val alarmDiagnosticsRepository: AlarmDiagnosticsRepository by lazy {
        RoomAlarmDiagnosticsRepository(database.alarmDiagnosticEntryDao())
    }

    override val alarmScheduler: AlarmScheduler by lazy {
        AndroidAlarmScheduler(
            context = applicationContext,
            diagnosticsRepository = alarmDiagnosticsRepository,
            timeProvider = timeProvider,
        )
    }

    val alarmStateStore by lazy { com.dayloop.app.repository.RoomAlarmStateStore(database) }

    override val reminderNotifications by lazy {
        com.dayloop.app.core.notifications.ReminderNotifications(
            applicationContext, settingsRepository, alarmDiagnosticsRepository, timeProvider)
    }

    override val alarmCoordinator by lazy {
        com.dayloop.app.core.alarms.AlarmCoordinator(alarmStateStore, taskRepository,
            scheduledOccurrenceRepository, persistentReminderEscalationRepository,
            alarmScheduler, reminderNotifications, alarmDiagnosticsRepository, timeProvider)
    }

    override val scheduleReconciler by lazy {
        com.dayloop.app.core.reconciliation.ScheduleReconciler(
            scheduledOccurrenceRepository, alarmScheduler, alarmDiagnosticsRepository, timeProvider)
    }

    override val capabilityRepairFlow by lazy {
        com.dayloop.app.core.notifications.CapabilityRepairFlow(applicationContext)
    }

    override val widgetStateReconciler by lazy {
        com.dayloop.app.widget.WidgetStateReconciler(applicationContext, taskRepository, settingsRepository, timeProvider)
    }

    override val goalRepository: com.dayloop.app.repository.GoalRepository by lazy {
        com.dayloop.app.repository.RoomGoalRepository(database.goalDao())
    }

    override val projectRepository: com.dayloop.app.repository.ProjectRepository by lazy {
        com.dayloop.app.repository.RoomProjectRepository(database.projectDao())
    }

    private companion object {
        const val DATABASE_NAME = "dayloop.db"
    }
}
