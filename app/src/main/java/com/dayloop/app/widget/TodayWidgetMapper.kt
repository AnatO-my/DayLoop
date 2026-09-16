package com.dayloop.app.widget

import com.dayloop.app.today.TodaySnapshot
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WidgetDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

fun TodaySnapshot.toWidgetState(): TodayWidgetState {
    val unresolved = (overdue + now + next + anytime).distinctBy { it.taskId }
    
    val nowItem = overdue.firstOrNull() ?: now.firstOrNull()
    val nextItem = if (nowItem != null) {
        (overdue + now + next).filter { it.taskId != nowItem.taskId }.firstOrNull()
    } else {
        next.firstOrNull() ?: anytime.firstOrNull()
    }

    return TodayWidgetState(
        unresolvedCount = unresolved.size,
        nowTask = nowItem?.let { WidgetTask(it.taskId, it.task.title, "NOW") },
        nextTask = nextItem?.let { WidgetTask(it.taskId, it.task.title, "NEXT") },
        dateLabel = date.format(WidgetDateFormatter)
    )
}
