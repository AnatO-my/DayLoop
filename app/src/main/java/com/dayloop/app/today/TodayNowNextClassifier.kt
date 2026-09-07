package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDateTime

data class TodayNowNextClassification(
    val now: List<TodayItem> = emptyList(),
    val next: List<TodayItem> = emptyList(),
)

class TodayNowNextClassifier {
    fun classify(
        tasks: List<Task>,
        now: LocalDateTime,
    ): TodayNowNextClassification {
        val activeTasks = tasks.filter { task -> task.status == TaskStatus.ACTIVE }

        return TodayNowNextClassification(
            now = activeTasks
                .filter { task -> task.isActiveAt(now) }
                .map { task -> TodayItem(task) },
            next = activeTasks
                .mapNotNull { task -> task.nextScheduledAtOrNull(now)?.let { scheduledAt -> scheduledAt to task } }
                .sortedWith(compareBy<Pair<LocalDateTime, Task>> { it.first }.thenBy { it.second.id.value })
                .map { (_, task) -> TodayItem(task) },
        )
    }

    private fun Task.isActiveAt(now: LocalDateTime): Boolean {
        val timeBlock = schedule as? TaskSchedule.TimeBlock ?: return false
        return !timeBlock.startsAt.isAfter(now) && now.isBefore(timeBlock.endsAt)
    }

    private fun Task.nextScheduledAtOrNull(now: LocalDateTime): LocalDateTime? = when (val currentSchedule = schedule) {
        is TaskSchedule.Deadline -> currentSchedule.dueAt
            .takeIf { dueAt -> !dueAt.isBefore(now) && dueAt.toLocalDate() == now.toLocalDate() }
        is TaskSchedule.TimeBlock -> currentSchedule.startsAt
            .takeIf { startsAt -> startsAt.isAfter(now) && startsAt.toLocalDate() == now.toLocalDate() }
        else -> null
    }
}
