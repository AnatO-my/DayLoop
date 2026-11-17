package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId

class TodayCompletedClassifier {
    fun classify(
        tasks: List<Task>,
        date: LocalDate,
        zoneId: ZoneId,
    ): List<TodayItem> = tasks
        .filter { task -> task.status == TaskStatus.COMPLETED }
        .filter { task -> task.completedAt?.atZone(zoneId)?.toLocalDate() == date }
        .map { task -> TodayItem(task) }

}
