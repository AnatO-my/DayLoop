package com.dayloop.app.di

import android.content.Context
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.core.backup.BackupManager
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.PersistentReminderEscalationRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.repository.TaskRepository
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
    val todayAssistant: TodayAssistant
        get() = TodayAssistant()
    val backupManager: BackupManager
        get() = BackupManager(this)
    val naturalIntentParser: NaturalIntentParser
        get() = NaturalIntentParser()
}
