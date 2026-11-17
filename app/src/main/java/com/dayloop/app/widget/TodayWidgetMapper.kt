package com.dayloop.app.widget

import com.dayloop.app.today.TodaySnapshot
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WidgetDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

fun TodaySnapshot.toWidgetState(): TodayWidgetState {
    val unresolved = this.unresolved
    
    val nowItem = unresolved.firstOrNull()
    val nextItem = unresolved.drop(1).firstOrNull()
    fun section(item: com.dayloop.app.today.TodayItem): String = when (item.taskId) {
        in overdue.map { it.taskId } -> "OVERDUE"
        in now.map { it.taskId } -> "NOW"
        in next.map { it.taskId } -> "NEXT"
        in missed.map { it.taskId } -> "MISSED"
        else -> "ANYTIME"
    }

    return TodayWidgetState(
        unresolvedCount = unresolved.size,
        nowTask = nowItem?.let { WidgetTask(it.taskId, it.task.title, section(it)) },
        nextTask = nextItem?.let { WidgetTask(it.taskId, it.task.title, section(it)) },
        completedCount = completedCount,
        dateLabel = date.format(WidgetDateFormatter)
    )
}
