package com.dayloop.app.di

import android.content.Context
import androidx.room.Room
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.core.alarms.AndroidAlarmScheduler
import com.dayloop.app.core.database.DayLoopDatabase
import com.dayloop.app.core.database.MIGRATION_1_2
import com.dayloop.app.core.database.MIGRATION_2_3
import com.dayloop.app.core.database.MIGRATION_3_4
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    override val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(database.taskDao())
    }

    override val scheduledOccurrenceRepository: ScheduledOccurrenceRepository by lazy {
        RoomScheduledOccurrenceRepository(database.scheduledOccurrenceDao())
    }

    override val persistentReminderEscalationRepository: PersistentReminderEscalationRepository by lazy {
        RoomPersistentReminderEscalationRepository(database.persistentReminderEscalationDao())
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

    private companion object {
        const val DATABASE_NAME = "dayloop.db"
    }
}
