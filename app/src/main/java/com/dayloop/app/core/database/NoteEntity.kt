package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["is_pinned"]),
        Index(value = ["updated_at"]),
        Index(value = ["linked_task_id"]),
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "linked_task_id") val linkedTaskId: String? = null,
    @ColumnInfo(name = "linked_project_id") val linkedProjectId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "tags") val tags: String = "", // Comma-separated tags
)
