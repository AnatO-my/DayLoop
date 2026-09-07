package com.dayloop.app.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.time.SystemTimeProvider
import com.dayloop.app.time.TimeProvider
import java.nio.charset.StandardCharsets
import java.util.Base64

class AndroidAlarmScheduler internal constructor(
    private val platform: ExactAlarmPlatform,
    private val diagnosticsRepository: AlarmDiagnosticsRepository? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) : AlarmScheduler {
    constructor(
        context: Context,
        diagnosticsRepository: AlarmDiagnosticsRepository? = null,
        timeProvider: TimeProvider = SystemTimeProvider(),
    ) : this(AndroidExactAlarmPlatform(context.applicationContext), diagnosticsRepository, timeProvider)

    override suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult {
        if (occurrence.status != ScheduledOccurrenceStatus.SCHEDULED) {
            val result = AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.NOT_SCHEDULED_STATUS,
            )
            record(
                occurrence = occurrence,
                event = AlarmDiagnosticEvent.SCHEDULE_REJECTED,
                message = result.reason.name,
            )
            return result
        }
        val canScheduleExact = platform.canScheduleExactAlarms()

        return try {
            if (canScheduleExact) {
                platform.scheduleExactAndAllowWhileIdle(
                    occurrence = occurrence,
                    triggerAtEpochMillis = occurrence.fireAt.toEpochMilli(),
                )
            } else {
                platform.scheduleInexactAndAllowWhileIdle(
                    occurrence = occurrence,
                    triggerAtEpochMillis = occurrence.fireAt.toEpochMilli(),
                )
            }
            val result = AlarmScheduleResult.Scheduled(occurrence.id, exact = canScheduleExact)
            record(
                occurrence = occurrence,
                event = if (canScheduleExact) {
                    AlarmDiagnosticEvent.SCHEDULE_EXACT
                } else {
                    AlarmDiagnosticEvent.SCHEDULE_INEXACT_FALLBACK
                },
                exact = canScheduleExact,
            )
            result
        } catch (_: SecurityException) {
            val result = AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
            )
            record(
                occurrence = occurrence,
                event = AlarmDiagnosticEvent.SCHEDULE_REJECTED,
                message = result.reason.name,
            )
            result
        } catch (_: RuntimeException) {
            val result = AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.SCHEDULE_FAILED,
            )
            record(
                occurrence = occurrence,
                event = AlarmDiagnosticEvent.SCHEDULE_REJECTED,
                message = result.reason.name,
            )
            result
        }
    }

    override suspend fun cancel(occurrenceId: ScheduledOccurrenceId): AlarmCancelResult = try {
        if (platform.cancel(occurrenceId)) {
            val result = AlarmCancelResult.Cancelled(occurrenceId)
            record(occurrenceId = occurrenceId, event = AlarmDiagnosticEvent.CANCELLED)
            result
        } else {
            val result = AlarmCancelResult.NotFound(occurrenceId)
            record(occurrenceId = occurrenceId, event = AlarmDiagnosticEvent.CANCEL_NOT_FOUND)
            result
        }
    } catch (_: RuntimeException) {
        val result = AlarmCancelResult.Failed(occurrenceId)
        record(occurrenceId = occurrenceId, event = AlarmDiagnosticEvent.CANCEL_FAILED)
        result
    }

    private suspend fun record(
        occurrence: ScheduledOccurrence,
        event: AlarmDiagnosticEvent,
        exact: Boolean? = null,
        message: String? = null,
    ) {
        record(
            occurrenceId = occurrence.id,
            taskId = occurrence.taskId,
            event = event,
            exact = exact,
            message = message,
        )
    }

    private suspend fun record(
        occurrenceId: ScheduledOccurrenceId,
        event: AlarmDiagnosticEvent,
    ) {
        record(
            occurrenceId = occurrenceId,
            taskId = null,
            event = event,
        )
    }

    private suspend fun record(
        occurrenceId: ScheduledOccurrenceId,
        taskId: com.dayloop.app.core.model.TaskId?,
        event: AlarmDiagnosticEvent,
        exact: Boolean? = null,
        message: String? = null,
    ) {
        val repository = diagnosticsRepository ?: return
        runCatching {
            repository.append(
                AlarmDiagnosticEntry(
                    occurrenceId = occurrenceId,
                    taskId = taskId,
                    event = event,
                    exact = exact,
                    message = message,
                    createdAt = timeProvider.now(),
                ),
            )
        }
    }
}

internal interface ExactAlarmPlatform {
    fun canScheduleExactAlarms(): Boolean

    fun scheduleExactAndAllowWhileIdle(
        occurrence: ScheduledOccurrence,
        triggerAtEpochMillis: Long,
    )

    fun scheduleInexactAndAllowWhileIdle(
        occurrence: ScheduledOccurrence,
        triggerAtEpochMillis: Long,
    )

    fun cancel(occurrenceId: ScheduledOccurrenceId): Boolean
}

private class AndroidExactAlarmPlatform(
    context: Context,
) : ExactAlarmPlatform {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntentFactory = ScheduledOccurrencePendingIntentFactory(appContext)

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun scheduleExactAndAllowWhileIdle(
        occurrence: ScheduledOccurrence,
        triggerAtEpochMillis: Long,
    ) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtEpochMillis,
            pendingIntentFactory.create(occurrence),
        )
    }

    override fun scheduleInexactAndAllowWhileIdle(
        occurrence: ScheduledOccurrence,
        triggerAtEpochMillis: Long,
    ) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtEpochMillis,
            pendingIntentFactory.create(occurrence),
        )
    }

    override fun cancel(occurrenceId: ScheduledOccurrenceId): Boolean {
        val pendingIntent = pendingIntentFactory.findExisting(occurrenceId) ?: return false
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        return true
    }
}

private class ScheduledOccurrencePendingIntentFactory(
    private val context: Context,
) {
    fun create(occurrence: ScheduledOccurrence): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            AlarmIntentIdentity.REQUEST_CODE,
            buildIntent(occurrence.id, occurrence.generation),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun findExisting(occurrenceId: ScheduledOccurrenceId): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            AlarmIntentIdentity.REQUEST_CODE,
            buildIntent(occurrenceId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun buildIntent(
        occurrenceId: ScheduledOccurrenceId,
        generation: Long? = null,
    ): Intent = Intent(AlarmIntentIdentity.ACTION_ALARM_DUE)
        .setPackage(context.packageName)
        .setClassName(context.packageName, AlarmIntentIdentity.receiverClassName(context.packageName))
        .setData(Uri.parse(AlarmIntentIdentity.dataUri(context.packageName, occurrenceId)))
        .putExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_ID, occurrenceId.value)
        .also { intent ->
            if (generation != null) {
                intent.putExtra(AlarmIntentIdentity.EXTRA_OCCURRENCE_GENERATION, generation)
            }
        }
}

internal object AlarmIntentIdentity {
    const val ACTION_ALARM_DUE = "com.dayloop.app.action.ALARM_DUE"
    const val ACTION_NOTIFICATION_COMPLETE = "com.dayloop.app.action.NOTIFICATION_COMPLETE"
    const val ACTION_NOTIFICATION_SNOOZE = "com.dayloop.app.action.NOTIFICATION_SNOOZE"
    const val ACTION_NOTIFICATION_RESCHEDULE = "com.dayloop.app.action.NOTIFICATION_RESCHEDULE"
    const val EXTRA_OCCURRENCE_ID = "com.dayloop.app.extra.OCCURRENCE_ID"
    const val EXTRA_OCCURRENCE_GENERATION = "com.dayloop.app.extra.OCCURRENCE_GENERATION"
    const val REQUEST_CODE = 0

    fun receiverClassName(packageName: String): String {
        require(packageName.isNotBlank()) { "Package name must not be blank." }
        return "$packageName.core.alarms.AlarmReceiver"
    }

    fun dataUri(
        packageName: String,
        occurrenceId: ScheduledOccurrenceId,
    ): String {
        require(packageName.isNotBlank()) { "Package name must not be blank." }
        val encodedId = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(occurrenceId.value.toByteArray(StandardCharsets.UTF_8))
        return "dayloop-alarm://$packageName/occurrences/$encodedId"
    }
}
