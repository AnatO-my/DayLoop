package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayMoment
import java.time.LocalDate

class RecurrenceOccurrenceGenerator(
    private val nextDateCalculator: RecurrenceNextDateCalculator = RecurrenceNextDateCalculator()
) {
    fun generateFor(tasks: List<Task>, moment: TodayMoment): List<Task> {
        return tasks
            .filter { it.status == TaskStatus.ACTIVE }
            .mapNotNull { task ->
                val recurrence = task.schedule as? TaskSchedule.CalendarRecurrence ?: return@mapNotNull null
                val nextDate = nextDateCalculator.nextDateOnOrAfter(recurrence.rule, moment.date)
                
                if (nextDate == moment.date) {
                    Task(
                        id = generateTaskId(task.id, moment.date),
                        title = task.title,
                        priority = task.priority,
                        status = TaskStatus.ACTIVE,
                        schedule = recurrence.localTime?.let { 
                            TaskSchedule.Deadline(moment.date.atTime(it))
                        } ?: TaskSchedule.AllDay(moment.date),
                        reminderPolicy = task.reminderPolicy,
                        projectId = task.projectId,
                        provenance = TaskProvenance(
                            originType = "RECURRENCE_RULE",
                            ruleId = task.id.value,
                            version = 1 // Basic versioning for now
                        )
                    )
                } else {
                    null
                }
            }
    }

    private fun generateTaskId(masterId: TaskId, date: LocalDate): TaskId {
        return TaskId("generated:recurrence:${masterId.value}:${date}")
    }
}
