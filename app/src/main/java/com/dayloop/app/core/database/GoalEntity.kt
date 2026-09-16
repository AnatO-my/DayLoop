package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "target_date")
    val targetDate: String? = null,
    @ColumnInfo(name = "progress_policy")
    val progressPolicy: String,
    @ColumnInfo(name = "status")
    val status: String,
)
