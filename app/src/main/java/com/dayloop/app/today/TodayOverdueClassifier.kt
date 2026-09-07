package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDateTime

class TodayOverdueClassifier {
    fun classify(
        tasks: List<Task>,
        now: LocalDateTime,
    ): List<TodayItem> = tasks
        .filter { task -> task.status == TaskStatus.ACTIVE }
        .filter { task -> task.isOverdueAt(now) }
        .map { task -> TodayItem(task) }

    private fun Task.isOverdueAt(now: LocalDateTime): Boolean {
        val deadline = schedule as? TaskSchedule.Deadline ?: return false
        return deadline.dueAt.isBefore(now)
    }
}
