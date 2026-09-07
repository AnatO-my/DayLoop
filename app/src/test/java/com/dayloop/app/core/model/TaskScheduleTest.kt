package com.dayloop.app.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskScheduleTest {
    @Test
    fun allDayKeepsDateOnlySemantics() {
        val date = LocalDate.of(2026, 8, 29)

        val schedule = TaskSchedule.AllDay(date = date)

        assertEquals(TaskScheduleKind.ALL_DAY, schedule.kind)
        assertEquals(date, schedule.date)
    }

    @Test
    fun deadlineKeepsLatestCompletionDateTime() {
        val dueAt = LocalDateTime.of(2026, 8, 29, 17, 30)

        val schedule = TaskSchedule.Deadline(dueAt = dueAt)

        assertEquals(TaskScheduleKind.DEADLINE, schedule.kind)
        assertEquals(dueAt, schedule.dueAt)
    }

    @Test
    fun timeBlockAcceptsStartBeforeEnd() {
        val startsAt = LocalDateTime.of(2026, 8, 29, 9, 0)
        val endsAt = LocalDateTime.of(2026, 8, 29, 10, 0)

        val schedule = TaskSchedule.TimeBlock(
            startsAt = startsAt,
            endsAt = endsAt,
        )

        assertEquals(TaskScheduleKind.TIME_BLOCK, schedule.kind)
        assertEquals(startsAt, schedule.startsAt)
        assertEquals(endsAt, schedule.endsAt)
    }

    @Test
    fun timeBlockRejectsEqualStartAndEnd() {
        val startsAt = LocalDateTime.of(2026, 8, 29, 9, 0)

        assertThrows(IllegalArgumentException::class.java) {
            TaskSchedule.TimeBlock(
                startsAt = startsAt,
                endsAt = startsAt,
            )
        }
    }

    @Test
    fun timeBlockRejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 29, 10, 0),
                endsAt = LocalDateTime.of(2026, 8, 29, 9, 0),
            )
        }
    }

    @Test
    fun taskCanCarryScheduleWithoutMutatingOriginal() {
        val task = Task(
            id = TaskId("task-1"),
            title = "Plan the day",
        )
        val schedule = TaskSchedule.AllDay(
            date = LocalDate.of(2026, 8, 29),
        )

        val scheduled = task.withSchedule(schedule)

        assertNull(task.schedule)
        assertEquals(schedule, scheduled.schedule)
        assertEquals(task.id, scheduled.id)
        assertEquals(task.title, scheduled.title)
    }
}
