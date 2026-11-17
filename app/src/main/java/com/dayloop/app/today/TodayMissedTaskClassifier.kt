package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

class TodayMissedTaskClassifier {
    fun classify(
        tasks: List<Task>,
        now: LocalDateTime,
        date: LocalDate,
    ): List<TodayItem> = tasks
        .filter { it.status == TaskStatus.ACTIVE }
        .filter { it.isMissedAt(now, date) }
        .map { TodayItem(it) }

    private fun Task.isMissedAt(now: LocalDateTime, date: LocalDate): Boolean {
        return when (val s = schedule) {
            is TaskSchedule.AllDay -> s.date.isBefore(date)
            is TaskSchedule.Deadline -> s.dueAt.isBefore(now)
            is TaskSchedule.TimeBlock -> !s.endsAt.isAfter(now)
            is TaskSchedule.Interval,
            is TaskSchedule.CalendarRecurrence,
            null -> false
        }
    }
}
