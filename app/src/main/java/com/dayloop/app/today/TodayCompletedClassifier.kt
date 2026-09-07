package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate

class TodayCompletedClassifier {
    fun classify(
        tasks: List<Task>,
        date: LocalDate,
    ): List<TodayItem> = tasks
        .filter { task -> task.status == TaskStatus.COMPLETED }
        .filter { task -> task.belongsToCompletedToday(date) }
        .map { task -> TodayItem(task) }

    private fun Task.belongsToCompletedToday(date: LocalDate): Boolean = when (val currentSchedule = schedule) {
        is TaskSchedule.AllDay -> currentSchedule.date == date
        is TaskSchedule.Deadline -> currentSchedule.dueAt.toLocalDate() == date
        is TaskSchedule.TimeBlock -> currentSchedule.startsAt.toLocalDate() == date ||
            currentSchedule.endsAt.toLocalDate() == date
        else -> false
    }
}
