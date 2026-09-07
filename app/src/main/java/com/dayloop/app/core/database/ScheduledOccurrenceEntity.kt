package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_occurrences",
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["status", "fire_at_epoch_millis"]),
    ],
)
data class ScheduledOccurrenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "fire_at_epoch_millis")
    val fireAtEpochMillis: Long,
    @ColumnInfo(name = "generation")
    val generation: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)