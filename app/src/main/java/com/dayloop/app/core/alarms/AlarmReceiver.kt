package com.dayloop.app.core.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayloop.app.DayLoopApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as DayLoopApplication
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmIntentIdentity.ACTION_ALARM_DUE -> {
                        val delivery = intent.toAlarmDelivery() ?: return@launch
                        AlarmDeliveryProcessor(
                            scheduledOccurrenceRepository = application.appGraph.scheduledOccurrenceRepository,
                            timeProvider = application.appGraph.timeProvider,
                            diagnosticsRepository = application.appGraph.alarmDiagnosticsRepository,
                        ).process(delivery)
                    }
                    AlarmIntentIdentity.ACTION_NOTIFICATION_COMPLETE,
                    AlarmIntentIdentity.ACTION_NOTIFICATION_SNOOZE,
                    AlarmIntentIdentity.ACTION_NOTIFICATION_RESCHEDULE -> {
                        val command = intent.toAlarmNotificationCommand() ?: return@launch
                        AlarmNotificationActionProcessor(
                            taskRepository = application.appGraph.taskRepository,
                            scheduledOccurrenceRepository = application.appGraph.scheduledOccurrenceRepository,
                            alarmScheduler = application.appGraph.alarmScheduler,
                            timeProvider = application.appGraph.timeProvider,
                            diagnosticsRepository = application.appGraph.alarmDiagnosticsRepository,
                        ).process(command)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun Intent.toAlarmNotificationCommand(): AlarmNotificationCommand? {
        val actionType = when (action) {
            AlarmIntentIdentity.ACTION_NOTIFICATION_COMPLETE -> AlarmNotificationAction.COMPLETE
            AlarmIntentIdentity.ACTION_NOTIFICATION_SNOOZE -> AlarmNotificationAction.SNOOZE
            AlarmIntentIdentity.ACTION_NOTIFICATION_RESCHEDULE -> AlarmNotificationAction.RESCHEDULE
            else -> return null
        }
        return AlarmNotificationCommand(
            action = actionType,
            delivery = toAlarmDelivery() ?: return null,
        )
    }

    private fun Intent.toAlarmDelivery(): AlarmDelivery? {
        val occurrenceIdValue = getStringExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val generation = getLongExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_GENERATION, 0L)
        if (generation <= 0L) return null

        return AlarmDelivery(
            occurrenceId = ScheduledOccurrenceId(occurrenceIdValue),
            generation = generation,
        )
    }
}
