package com.dayloop.app.core.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val linkedTaskId: String? = null,
    val linkedProjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
) {
    val tagListFormatted: String
        get() = tags.joinToString(", ")
}
