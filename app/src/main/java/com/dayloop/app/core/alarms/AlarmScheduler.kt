package com.dayloop.app.core.alarms

interface AlarmScheduler {
    suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult

    suspend fun cancel(occurrenceId: ScheduledOccurrenceId): AlarmCancelResult
}

sealed interface AlarmScheduleResult {
    val occurrenceId: ScheduledOccurrenceId

    data class Scheduled(
        override val occurrenceId: ScheduledOccurrenceId,
        val exact: Boolean,
    ) : AlarmScheduleResult

    data class Rejected(
        override val occurrenceId: ScheduledOccurrenceId,
        val reason: AlarmScheduleRejectionReason,
    ) : AlarmScheduleResult
}

enum class AlarmScheduleRejectionReason {
    NOT_SCHEDULED_STATUS,
    EXACT_ALARM_PERMISSION_MISSING,
    NOTIFICATION_PERMISSION_MISSING,
    SCHEDULE_FAILED,
}

sealed interface AlarmCancelResult {
    val occurrenceId: ScheduledOccurrenceId

    data class Cancelled(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : AlarmCancelResult

    data class NotFound(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : AlarmCancelResult

    data class Failed(
        override val occurrenceId: ScheduledOccurrenceId,
    ) : AlarmCancelResult
}