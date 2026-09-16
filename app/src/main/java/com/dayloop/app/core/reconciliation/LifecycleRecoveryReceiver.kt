package com.dayloop.app.core.reconciliation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayloop.app.DayLoopApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LifecycleRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as DayLoopApplication
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_SET,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        val reconciler = ScheduleReconciler(
                            application.appGraph.scheduledOccurrenceRepository,
                            application.appGraph.alarmScheduler,
                            application.appGraph.alarmDiagnosticsRepository,
                            application.appGraph.timeProvider
                        )
                        reconciler.reconcile()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
