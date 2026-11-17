package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.recurrence.RecurrenceOccurrenceGenerator

object TodaySnapshotBuilder {
    fun build(
        tasks: List<Task>,
        moment: TodayMoment,
        generatedTasks: List<Task> = emptyList(),
    ): TodaySnapshot {
        val recurrenceOccurrences = RecurrenceOccurrenceGenerator().generateFor(tasks, moment)
        val allGenerated = generatedTasks + recurrenceOccurrences
        val generatedIds = allGenerated.map { it.id }.toSet()

        val storedById = tasks.associateBy { it.id }

        // Filter out master recurrence tasks and anything that was generated but is already stored
        val baseTasks = tasks.filter {
            it.id !in generatedIds && it.schedule !is TaskSchedule.CalendarRecurrence
        }

        val mergedGenerated = allGenerated.map { generated ->
            storedById[generated.id]?.takeIf { it.provenance == generated.provenance } ?: generated
        }

        val allTasks = baseTasks + mergedGenerated

        val missedItems = TodayMissedTaskClassifier().classify(allTasks, moment.localDateTime, moment.date)
        val missedIds = missedItems.map { it.taskId }.toSet()
        val remainingTasks = allTasks.filter { it.id !in missedIds }

        val overdue = TodayOverdueClassifier().classify(allTasks, moment.localDateTime)
        val nowNext = TodayNowNextClassifier().classify(remainingTasks, moment.localDateTime)
        val anytime = TodayAnytimeClassifier().classify(remainingTasks, moment.date)
        val completed = TodayCompletedClassifier().classify(allTasks, moment.date, moment.zoneId)

        return TodaySnapshot(
            date = moment.date,
            overdue = overdue,
            now = nowNext.now,
            next = nowNext.next,
            anytime = anytime,
            completed = completed,
            missed = missedItems
        )
    }
}
