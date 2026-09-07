package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate

class TodayAnytimeClassifier {
    fun classify(
        tasks: List<Task>,
        date: LocalDate,
    ): List<TodayItem> = tasks
        .filter { task -> task.status == TaskStatus.ACTIVE }
        .filter { task -> task.belongsToAnytimeToday(date) }
        .map { task -> TodayItem(task) }

    private fun Task.belongsToAnytimeToday(date: LocalDate): Boolean {
        val allDay = schedule as? TaskSchedule.AllDay ?: return false
        return allDay.date == date
    }
}
