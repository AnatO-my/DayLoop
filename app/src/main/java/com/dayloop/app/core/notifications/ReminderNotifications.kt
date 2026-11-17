package com.dayloop.app.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.dayloop.app.R
import com.dayloop.app.core.alarms.*
import com.dayloop.app.core.model.Task
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.settings.SettingsRepository
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.first

data class ReminderCapabilities(
    val notificationsAllowed: Boolean,
    val reminderChannelEnabled: Boolean,
    val alarmChannelEnabled: Boolean,
    val alarmChannelHighImportance: Boolean,
    val fullScreenAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
)

/** Android framework checks reduced to a pure policy seam for regression coverage. */
internal object ReminderCapabilityPolicy {
    fun notificationsAllowed(apiLevel: Int, runtimeGranted: Boolean, systemEnabled: Boolean): Boolean =
        (apiLevel < 33 || runtimeGranted) && systemEnabled

    fun exactAlarmsAllowed(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean =
        apiLevel < 31 || canScheduleExactAlarms
}

class ReminderNotifications(
    private val context: Context,
    private val settings: SettingsRepository,
    private val diagnostics: AlarmDiagnosticsRepository,
    private val clock: TimeProvider,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Reminders",
            NotificationManager.IMPORTANCE_HIGH).apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            enableVibration(true)
        })
        manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL, "Alarm reminders",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Opt-in alarm-style reminders"
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
        })
    }

    fun capabilities(): ReminderCapabilities {
        createChannels()
        val notificationPermissionGranted = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val alarmChannel = manager.getNotificationChannel(ALARM_CHANNEL)
        val canScheduleExactAlarms = Build.VERSION.SDK_INT < 31 ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        return ReminderCapabilities(
            notificationsAllowed = ReminderCapabilityPolicy.notificationsAllowed(
                Build.VERSION.SDK_INT, notificationPermissionGranted, manager.areNotificationsEnabled()),
            reminderChannelEnabled = manager.getNotificationChannel(REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE,
            alarmChannelEnabled = alarmChannel?.importance != NotificationManager.IMPORTANCE_NONE,
            alarmChannelHighImportance = (alarmChannel?.importance ?: 0) >= NotificationManager.IMPORTANCE_HIGH,
            fullScreenAllowed = Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent(),
            exactAlarmsAllowed = ReminderCapabilityPolicy.exactAlarmsAllowed(
                Build.VERSION.SDK_INT, canScheduleExactAlarms),
        )
    }

    suspend fun show(task: Task, delivered: ScheduledOccurrence, actionOccurrence: ScheduledOccurrence = delivered) {
        val preferences = settings.settings.first()
        val capability = capabilities()
        val alarmStyle = preferences.alarmGradeEnabled && task.reminderPolicy.mode.supportsRealerts
        if (!capability.notificationsAllowed ||
            !(if (alarmStyle) capability.alarmChannelEnabled else capability.reminderChannelEnabled)) {
            record(delivered, "NOTIFICATION_BLOCKED")
            return
        }
        val channel = if (alarmStyle) ALARM_CHANNEL else REMINDER_CHANNEL
        val content = PendingIntent.getActivity(context, 0,
            Intent(context, ReminderActivity::class.java)
                .setData(Uri.parse("dayloop-reminder://view/${Uri.encode(actionOccurrence.id.value)}/${actionOccurrence.generation}"))
                .putExtra(EXTRA_ID, actionOccurrence.id.value)
                .putExtra(EXTRA_GENERATION, actionOccurrence.generation),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_dayloop_notification)
            .setContentTitle("DayLoop reminder").setContentText("Open DayLoop to view.")
            .setContentIntent(content).build()
        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_dayloop_notification)
            .setContentTitle(task.title)
            .setContentText("Your reminder is due.")
            .setCategory(if (alarmStyle) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .setVisibility(if (preferences.lockScreenPrivacyEnabled) Notification.VISIBILITY_PRIVATE else Notification.VISIBILITY_PUBLIC)
            .setPublicVersion(publicVersion)
            .setContentIntent(content)
            .setOngoing(alarmStyle)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
        
        builder.addAction(action("Done", AlarmNotificationAction.COMPLETE, actionOccurrence))
        if (task.reminderPolicy.snoozeAllowed) {
            builder.addAction(action("Snooze", AlarmNotificationAction.SNOOZE, actionOccurrence))
        }
        if (task.reminderPolicy.rescheduleAllowed) {
            builder.addAction(action("Reschedule", AlarmNotificationAction.RESCHEDULE, actionOccurrence))
        }
        val fullScreen = alarmStyle &&
            task.reminderPolicy.mode == com.dayloop.app.core.model.ReminderMode.STRICT &&
            capability.alarmChannelHighImportance && capability.fullScreenAllowed
        if (fullScreen) builder.setFullScreenIntent(content, true)
        try {
            manager.notify(tag(delivered.id), 1, builder.build())
            record(delivered, if (fullScreen) "FULL_SCREEN_REQUESTED" else "NOTIFICATION_POSTED")
        } catch (_: SecurityException) {
            record(delivered, "NOTIFICATION_PERMISSION_CHANGED")
        }
    }

    private fun action(label: String, action: AlarmNotificationAction, occurrence: ScheduledOccurrence): Notification.Action {
        if (action == AlarmNotificationAction.RESCHEDULE) {
            val openEditor = Intent(context, com.dayloop.app.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("dayloop-edit://${Uri.encode(occurrence.id.value)}/${occurrence.generation}"))
                .putExtra("edit_task_id", occurrence.taskId.value)
                .putExtra(EXTRA_ID, occurrence.id.value)
                .putExtra(EXTRA_GENERATION, occurrence.generation)
            return Notification.Action.Builder(null, label, PendingIntent.getActivity(context, 0, openEditor,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()
        }
        val actionName = when (action) {
            AlarmNotificationAction.COMPLETE -> AlarmIntentIdentity.ACTION_NOTIFICATION_COMPLETE
            AlarmNotificationAction.SNOOZE -> AlarmIntentIdentity.ACTION_NOTIFICATION_SNOOZE
            AlarmNotificationAction.RESCHEDULE -> AlarmIntentIdentity.ACTION_NOTIFICATION_RESCHEDULE
        }
        val intent = Intent(context, AlarmReceiver::class.java).setAction(actionName)
            .setData(Uri.parse("dayloop-action://${action.name}/${Uri.encode(occurrence.id.value)}/${occurrence.generation}"))
            .putExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_ID, occurrence.id.value)
            .putExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_GENERATION, occurrence.generation)
        val pending = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null, label, pending).build()
    }

    fun cancel(id: ScheduledOccurrenceId) { manager.cancel(tag(id), 1) }
    fun clearAll() { manager.cancelAll() }

    private suspend fun record(occurrence: ScheduledOccurrence, message: String) {
        diagnostics.append(AlarmDiagnosticEntry(occurrenceId = occurrence.id, taskId = occurrence.taskId,
            event = AlarmDiagnosticEvent.NOTIFICATION_STATUS, message = message, createdAt = clock.now()))
    }

    private fun tag(id: ScheduledOccurrenceId) = "reminder:${id.value}"

    companion object {
        const val REMINDER_CHANNEL = "dayloop.reminders.v2"
        const val ALARM_CHANNEL = "dayloop.alarm_reminders.v2"
        const val EXTRA_ID = "reminder_occurrence_id"
        const val EXTRA_GENERATION = "reminder_generation"
    }
}
