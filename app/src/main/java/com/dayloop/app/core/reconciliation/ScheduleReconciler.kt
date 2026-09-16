package com.dayloop.app.core.reconciliation

import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import com.dayloop.app.core.alarms.AlarmDiagnosticEvent
import com.dayloop.app.core.alarms.AlarmScheduler
import com.dayloop.app.core.alarms.ScheduledOccurrenceStatus
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleReconciler(
    private val occurrenceRepository: ScheduledOccurrenceRepository,
    private val alarmScheduler: AlarmScheduler,
    private val diagnosticsRepository: AlarmDiagnosticsRepository,
    private val timeProvider: TimeProvider,
) {
    suspend fun reconcile() = withContext(Dispatchers.IO) {
        val scheduled = occurrenceRepository.getScheduledOccurrences()
        val now = timeProvider.now()
        
        diagnosticsRepository.append(
            AlarmDiagnosticEntry(
                occurrenceId = null,
                taskId = null,
                event = AlarmDiagnosticEvent.RECONCILIATION_STARTED,
                message = "Reconciling ${scheduled.size} occurrences",
                createdAt = now
            )
        )

        scheduled.forEach { occurrence ->
            // Re-scheduling is idempotent because of stable PendingIntent identity.
            // If the time is in the past, AlarmManager will trigger it immediately.
            alarmScheduler.schedule(occurrence)
        }

        diagnosticsRepository.append(
            AlarmDiagnosticEntry(
                occurrenceId = null,
                taskId = null,
                event = AlarmDiagnosticEvent.RECONCILIATION_COMPLETED,
                message = "Reconciled ${scheduled.size} occurrences",
                createdAt = timeProvider.now()
            )
        )
    }
}
