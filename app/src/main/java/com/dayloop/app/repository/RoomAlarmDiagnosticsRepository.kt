package com.dayloop.app.repository

import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import com.dayloop.app.core.alarms.AlarmDiagnosticEntryId
import com.dayloop.app.core.alarms.AlarmDiagnosticEvent
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.database.AlarmDiagnosticEntryDao
import com.dayloop.app.core.database.AlarmDiagnosticEntryEntity
import com.dayloop.app.core.model.TaskId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmDiagnosticsRepository(
    private val alarmDiagnosticEntryDao: AlarmDiagnosticEntryDao,
) : AlarmDiagnosticsRepository {
    override fun observeEntries(): Flow<List<AlarmDiagnosticEntry>> = alarmDiagnosticEntryDao.observeAll()
        .map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun getRecentEntries(limit: Int): List<AlarmDiagnosticEntry> {
        require(limit > 0) { "Alarm diagnostic query limit must be greater than 0." }
        return alarmDiagnosticEntryDao.getRecent(limit).map { row -> row.toDomain() }
    }

    override suspend fun append(entry: AlarmDiagnosticEntry) {
        alarmDiagnosticEntryDao.insert(entry.toEntity())
    }

    override suspend fun clearAllEntries() {
        alarmDiagnosticEntryDao.deleteAll()
    }
}

internal fun AlarmDiagnosticEntry.toEntity(): AlarmDiagnosticEntryEntity = AlarmDiagnosticEntryEntity(
    id = id.value,
    occurrenceId = occurrenceId?.value,
    taskId = taskId?.value,
    event = event.name,
    exact = exact,
    message = message,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

internal fun AlarmDiagnosticEntryEntity.toDomain(): AlarmDiagnosticEntry = AlarmDiagnosticEntry(
    id = AlarmDiagnosticEntryId(id),
    occurrenceId = occurrenceId?.let(::ScheduledOccurrenceId),
    taskId = taskId?.let(::TaskId),
    event = AlarmDiagnosticEvent.valueOf(event),
    exact = exact,
    message = message,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)
