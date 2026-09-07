package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.recurrence.IntervalNextEventCalculator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NextOccurrenceCalculator(
    private val createOccurrenceId: () -> ScheduledOccurrenceId = ScheduledOccurrenceId::generate,
    private val intervalNextEventCalculator: IntervalNextEventCalculator = IntervalNextEventCalculator(),
) {
    fun calculate(
        task: Task,
        now: Instant,
        zoneId: ZoneId,
        generation: Long = 1L,
    ): ScheduledOccurrence? {
        if (task.status != TaskStatus.ACTIVE) return null

        val fireAt = when (val schedule = task.schedule) {
            is TaskSchedule.Deadline -> schedule.dueAt.toInstant(zoneId)
            is TaskSchedule.TimeBlock -> schedule.startsAt.toInstant(zoneId)
            is TaskSchedule.Interval -> intervalNextEventCalculator
                .nextEventAtOrAfter(
                    rule = schedule.rule,
                    now = now.atZone(zoneId).toLocalDateTime(),
                )
                ?.toInstant(zoneId)
            null,
            is TaskSchedule.AllDay,
            is TaskSchedule.CalendarRecurrence,
            -> return null
        } ?: return null

        if (fireAt.isBefore(now)) return null

        return ScheduledOccurrence(
            id = createOccurrenceId(),
            taskId = task.id,
            fireAt = fireAt,
            generation = generation,
            createdAt = now,
            updatedAt = now,
        )
    }
}

private fun LocalDateTime.toInstant(zoneId: ZoneId): Instant = atZone(zoneId).toInstant()
