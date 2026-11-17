package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.recurrence.IntervalNextEventCalculator
import com.dayloop.app.core.recurrence.RecurrenceNextDateCalculator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NextOccurrenceCalculator(
    private val createOccurrenceId: () -> ScheduledOccurrenceId = ScheduledOccurrenceId::generate,
    private val intervalNextEventCalculator: IntervalNextEventCalculator = IntervalNextEventCalculator(),
    private val recurrenceNextDateCalculator: RecurrenceNextDateCalculator = RecurrenceNextDateCalculator(),
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
            is TaskSchedule.CalendarRecurrence -> {
                val today = now.atZone(zoneId).toLocalDate()
                val nextDate = recurrenceNextDateCalculator.nextDateOnOrAfter(schedule.rule, today)
                val time = schedule.localTime ?: return null
                
                val candidate = nextDate?.atTime(time)?.toInstant(zoneId)
                if (candidate != null && !candidate.isBefore(now)) {
                    candidate
                } else {
                    val futureDate = recurrenceNextDateCalculator.nextDateOnOrAfter(schedule.rule, today.plusDays(1))
                    futureDate?.atTime(time)?.toInstant(zoneId)
                }
            }
            null,
            is TaskSchedule.AllDay,
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
