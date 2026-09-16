package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "goal_id")
    val goalId: String? = null,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "protocol_type")
    val protocolType: String? = null,
    @ColumnInfo(name = "protocol_active_days")
    val protocolActiveDays: Int? = null,
    @ColumnInfo(name = "protocol_rest_days")
    val protocolRestDays: Int? = null,
    @ColumnInfo(name = "protocol_anchor_date")
    val protocolAnchorDate: String? = null,
    @ColumnInfo(name = "protocol_task_title")
    val protocolTaskTitle: String? = null,
    @ColumnInfo(name = "protocol_task_priority")
    val protocolTaskPriority: String? = null,
    @ColumnInfo(name = "version")
    val version: Long = 1L,
)
