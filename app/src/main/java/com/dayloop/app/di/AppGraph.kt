package com.dayloop.app.di

import android.content.Context
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.time.TimeProvider

interface AppGraph {
    val applicationContext: Context
    val timeProvider: TimeProvider
    val taskRepository: TaskRepository
    val scheduledOccurrenceRepository: ScheduledOccurrenceRepository
    val persistentReminderEscalationRepository: PersistentReminderEscalationRepository
    val alarmDiagnosticsRepository: AlarmDiagnosticsRepository
    val alarmScheduler: AlarmScheduler
}
