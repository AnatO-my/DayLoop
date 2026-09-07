package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persistent_reminder_escalations",
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["source_occurrence_id"]),
        Index(value = ["status", "next_realert_at_epoch_millis"]),
    ],
)
data class PersistentReminderEscalationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "source_occurrence_id")
    val sourceOccurrenceId: String,
    @ColumnInfo(name = "next_realert_at_epoch_millis")
    val nextRealertAtEpochMillis: Long?,
    @ColumnInfo(name = "realert_interval_millis")
    val realertIntervalMillis: Long,
    @ColumnInfo(name = "maximum_realerts")
    val maximumRealerts: Int,
    @ColumnInfo(name = "realerts_delivered")
    val realertsDelivered: Int,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
