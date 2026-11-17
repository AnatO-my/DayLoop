package com.dayloop.app.widget

import com.dayloop.app.core.model.TaskId

data class TodayWidgetState(
    val unresolvedCount: Int = 0,
    val nowTask: WidgetTask? = null,
    val nextTask: WidgetTask? = null,
    val dateLabel: String = "",
    val completedCount: Int = 0,
)

data class WidgetTask(
    val id: TaskId,
    val title: String,
    val section: String,
)
