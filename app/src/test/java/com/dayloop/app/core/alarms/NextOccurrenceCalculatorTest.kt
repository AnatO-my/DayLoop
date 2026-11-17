package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextOccurrenceCalculatorTest {
    private val zoneId = ZoneId.of("Africa/Lagos")
    private val now = Instant.parse("2026-09-06T08:00:00Z")

    @Test
    fun futureDeadlineBuildsScheduledOccurrenceAtDueInstant() {
        val task = Task(
            id = TaskId("task-deadline"),
            title = "Submit report",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 9, 6, 10, 30),
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = now,
            zoneId = zoneId,
            generation = 2L,
        )

        assertEquals(ScheduledOccurrenceId("occurrence-1"), occurrence?.id)
        assertEquals(TaskId("task-deadline"), occurrence?.taskId)
        assertEquals(Instant.parse("2026-09-06T09:30:00Z"), occurrence?.fireAt)
        assertEquals(2L, occurrence?.generation)
        assertEquals(ScheduledOccurrenceStatus.SCHEDULED, occurrence?.status)
        assertEquals(now, occurrence?.createdAt)
        assertEquals(now, occurrence?.updatedAt)
    }

    @Test
    fun futureTimeBlockBuildsScheduledOccurrenceAtStartInstant() {
        val task = Task(
            id = TaskId("task-time-block"),
            title = "Deep work",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 9, 30),
                endsAt = LocalDateTime.of(2026, 9, 6, 11, 0),
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = now,
            zoneId = zoneId,
        )

        assertEquals(Instant.parse("2026-09-06T08:30:00Z"), occurrence?.fireAt)
    }

    @Test
    fun occurrenceCanBeBuiltAtCurrentInstant() {
        val task = Task(
            id = TaskId("task-now"),
            title = "Start now",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 9, 6, 9, 0),
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = now,
            zoneId = zoneId,
        )

        assertEquals(now, occurrence?.fireAt)
    }

    @Test
    fun pastDeadlineReturnsNoOccurrence() {
        val task = Task(
            id = TaskId("task-past"),
            title = "Past due",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 9, 6, 8, 59),
            ),
        )

        assertNull(calculator().calculate(task = task, now = now, zoneId = zoneId))
    }

    @Test
    fun activeTimeBlockWithPastStartReturnsNoOccurrence() {
        val task = Task(
            id = TaskId("task-active"),
            title = "Already started",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 8, 30),
                endsAt = LocalDateTime.of(2026, 9, 6, 10, 0),
            ),
        )

        assertNull(calculator().calculate(task = task, now = now, zoneId = zoneId))
    }

    @Test
    fun completedTaskReturnsNoOccurrence() {
        val task = Task(
            id = TaskId("task-complete"),
            title = "Done",
            status = TaskStatus.COMPLETED,
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 9, 6, 10, 0),
            ),
        )

        assertNull(calculator().calculate(task = task, now = now, zoneId = zoneId))
    }

    @Test
    fun intervalBuildsScheduledOccurrenceAtNextClockAnchoredSlot() {
        val task = Task(
            id = TaskId("task-interval"),
            title = "Hydrate",
            schedule = TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(9, 0),
                        endsAt = LocalTime.of(17, 0),
                    ),
                    eligibleDays = setOf(DayOfWeek.SUNDAY),
                    startsOn = LocalDate.of(2026, 9, 6),
                ),
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = now,
            zoneId = zoneId,
        )

        assertEquals(ScheduledOccurrenceId("occurrence-1"), occurrence?.id)
        assertEquals(TaskId("task-interval"), occurrence?.taskId)
        assertEquals(Instant.parse("2026-09-06T08:00:00Z"), occurrence?.fireAt)
        assertEquals(now, occurrence?.createdAt)
        assertEquals(now, occurrence?.updatedAt)
    }

    @Test
    fun intervalAfterWindowCloseBuildsOccurrenceOnNextEligibleDay() {
        val task = Task(
            id = TaskId("task-interval"),
            title = "Hydrate",
            schedule = TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(9, 0),
                        endsAt = LocalTime.of(17, 0),
                    ),
                    eligibleDays = setOf(DayOfWeek.MONDAY),
                    startsOn = LocalDate.of(2026, 9, 6),
                ),
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = Instant.parse("2026-09-06T16:30:00Z"),
            zoneId = zoneId,
        )

        assertEquals(Instant.parse("2026-09-07T08:00:00Z"), occurrence?.fireAt)
    }

    @Test
    fun completionAnchoredIntervalReturnsNoOccurrenceUntilAcknowledgementStateExists() {
        val task = Task(
            id = TaskId("task-completion-anchored"),
            title = "Hydrate",
            schedule = TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(9, 0),
                        endsAt = LocalTime.of(17, 0),
                    ),
                    eligibleDays = setOf(DayOfWeek.SUNDAY),
                    startsOn = LocalDate.of(2026, 9, 6),
                    anchor = com.dayloop.app.core.model.IntervalAnchor.COMPLETION,
                ),
            ),
        )

        assertNull(calculator().calculate(task = task, now = now, zoneId = zoneId))
    }

    @Test
    fun recurrenceWithTimeReturnsOccurrence() {
        val task = Task(
            id = TaskId("task-recurrence"),
            title = "Morning Gym",
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.daily(LocalDate.of(2026, 9, 6)),
                localTime = LocalTime.of(7, 0)
            ),
        )

        val occurrence = calculator().calculate(
            task = task,
            now = now,
            zoneId = zoneId,
        )

        assertEquals(Instant.parse("2026-09-07T06:00:00Z"), occurrence?.fireAt)
    }

    @Test
    fun unscheduledAllDayAndRecurrenceWithoutTimeTasksReturnNoOccurrence() {
        val unscheduled = Task(
            id = TaskId("task-unscheduled"),
            title = "Inbox",
        )
        val allDay = Task(
            id = TaskId("task-all-day"),
            title = "Plan today",
            schedule = TaskSchedule.AllDay(LocalDate.of(2026, 9, 6)),
        )
        val recurrence = Task(
            id = TaskId("task-recurrence"),
            title = "Weekly review",
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 6),
                    daysOfWeek = setOf(DayOfWeek.SUNDAY),
                ),
            ),
        )

        listOf(unscheduled, allDay, recurrence).forEach { task ->
            assertNull(calculator().calculate(task = task, now = now, zoneId = zoneId))
        }
    }

    private fun calculator(): NextOccurrenceCalculator = NextOccurrenceCalculator(
        createOccurrenceId = { ScheduledOccurrenceId("occurrence-1") },
    )
}
