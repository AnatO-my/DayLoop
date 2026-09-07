package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import java.time.Instant
import java.util.UUID

data class AlarmDiagnosticEntryId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Alarm diagnostic entry id must not be blank." }
    }
}

data class AlarmDiagnosticEntry(
    val id: AlarmDiagnosticEntryId = AlarmDiagnosticEntryId(UUID.randomUUID().toString()),
    val occurrenceId: ScheduledOccurrenceId?,
    val taskId: TaskId?,
    val event: AlarmDiagnosticEvent,
    val exact: Boolean? = null,
    val message: String? = null,
    val createdAt: Instant,
) {
    init {
        require(message == null || message.isNotBlank()) { "Alarm diagnostic message must not be blank." }
    }
}

enum class AlarmDiagnosticEvent {
    SCHEDULE_EXACT,
    SCHEDULE_INEXACT_FALLBACK,
    SCHEDULE_REJECTED,
    CANCELLED,
    CANCEL_NOT_FOUND,
    CANCEL_FAILED,
    DELIVERY_ACCEPTED,
    DELIVERY_STALE,
    ACTION_COMPLETED,
    ACTION_SNOOZED,
    ACTION_REJECTED,
    ACTION_RESCHEDULE_REQUESTED,
}
