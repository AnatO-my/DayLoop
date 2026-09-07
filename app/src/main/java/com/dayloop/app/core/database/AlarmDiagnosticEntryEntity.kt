package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_diagnostic_entries",
    indices = [
        Index(value = ["occurrence_id"]),
        Index(value = ["created_at_epoch_millis"]),
    ],
)
data class AlarmDiagnosticEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "occurrence_id")
    val occurrenceId: String?,
    @ColumnInfo(name = "task_id")
    val taskId: String?,
    @ColumnInfo(name = "event")
    val event: String,
    @ColumnInfo(name = "exact")
    val exact: Boolean?,
    @ColumnInfo(name = "message")
    val message: String?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)
