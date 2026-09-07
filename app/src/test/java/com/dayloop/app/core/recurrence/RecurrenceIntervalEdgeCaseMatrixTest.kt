package com.dayloop.app.core.recurrence

import com.dayloop.app.core.alarms.NextOccurrenceCalculator
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
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

class RecurrenceIntervalEdgeCaseMatrixTest {
    private val recurrencePreviewCalculator = RecurrencePreviewCalculator()
    private val intervalActiveWindowCalculator = IntervalActiveWindowCalculator()
    private val intervalNextEventCalculator = IntervalNextEventCalculator()

    @Test
    fun recurrencePreviewMatrixCoversCadenceEndDateAndCalendarEdges() {
        data class Row(
            val name: String,
            val rule: RecurrenceRule,
            val fromDate: LocalDate,
            val count: Int,
            val expected: List<LocalDate>,
        )

        val rows = listOf(
            Row(
                name = "daily includes exact end date and stops",
                rule = RecurrenceRule.daily(
                    startsOn = LocalDate.of(2026, 9, 1),
                    endsOn = LocalDate.of(2026, 9, 3),
                ),
                fromDate = LocalDate.of(2026, 9, 2),
                count = 5,
                expected = listOf(
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 9, 3),
                ),
            ),
            Row(
                name = "weekly every other week keeps selected days in eligible weeks",
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 7),
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    every = 2,
                ),
                fromDate = LocalDate.of(2026, 9, 12),
                count = 4,
                expected = listOf(
                    LocalDate.of(2026, 9, 21),
                    LocalDate.of(2026, 9, 25),
                    LocalDate.of(2026, 10, 5),
                    LocalDate.of(2026, 10, 9),
                ),
            ),
            Row(
                name = "monthly thirty first clamps then restores anchor",
                rule = RecurrenceRule.monthly(
                    startsOn = LocalDate.of(2026, 1, 31),
                ),
                fromDate = LocalDate.of(2026, 1, 31),
                count = 4,
                expected = listOf(
                    LocalDate.of(2026, 1, 31),
                    LocalDate.of(2026, 2, 28),
                    LocalDate.of(2026, 3, 31),
                    LocalDate.of(2026, 4, 30),
                ),
            ),
            Row(
                name = "yearly leap day clamps until leap year returns",
                rule = RecurrenceRule.yearly(
                    startsOn = LocalDate.of(2028, 2, 29),
                ),
                fromDate = LocalDate.of(2028, 2, 29),
                count = 5,
                expected = listOf(
                    LocalDate.of(2028, 2, 29),
                    LocalDate.of(2029, 2, 28),
                    LocalDate.of(2030, 2, 28),
                    LocalDate.of(2031, 2, 28),
                    LocalDate.of(2032, 2, 29),
                ),
            ),
        )

        rows.forEach { row ->
            assertEquals(
                row.name,
                row.expected,
                recurrencePreviewCalculator.previewDates(
                    rule = row.rule,
                    fromDate = row.fromDate,
                    count = row.count,
                ),
            )
        }
    }

    @Test
    fun intervalActiveWindowMatrixCoversBoundaryPositions() {
        data class Row(
            val name: String,
            val now: LocalDateTime,
            val expectedPosition: IntervalActiveWindowPosition,
            val expectedNextWindowStart: LocalDateTime?,
        )

        val rule = weekdayRule()
        val rows = listOf(
            Row(
                name = "before start date finds first eligible window",
                now = LocalDateTime.of(2026, 9, 6, 10, 0),
                expectedPosition = IntervalActiveWindowPosition.OUTSIDE_DATE_RANGE,
                expectedNextWindowStart = LocalDateTime.of(2026, 9, 7, 9, 0),
            ),
            Row(
                name = "before window keeps current date as next window",
                now = LocalDateTime.of(2026, 9, 7, 8, 59),
                expectedPosition = IntervalActiveWindowPosition.BEFORE_WINDOW,
                expectedNextWindowStart = LocalDateTime.of(2026, 9, 7, 9, 0),
            ),
            Row(
                name = "window start is inside",
                now = LocalDateTime.of(2026, 9, 7, 9, 0),
                expectedPosition = IntervalActiveWindowPosition.INSIDE_WINDOW,
                expectedNextWindowStart = LocalDateTime.of(2026, 9, 7, 9, 0),
            ),
            Row(
                name = "window end is after and rolls forward",
                now = LocalDateTime.of(2026, 9, 7, 17, 0),
                expectedPosition = IntervalActiveWindowPosition.AFTER_WINDOW,
                expectedNextWindowStart = LocalDateTime.of(2026, 9, 9, 9, 0),
            ),
            Row(
                name = "ineligible day rolls to next eligible day",
                now = LocalDateTime.of(2026, 9, 8, 10, 0),
                expectedPosition = IntervalActiveWindowPosition.INELIGIBLE_DAY,
                expectedNextWindowStart = LocalDateTime.of(2026, 9, 9, 9, 0),
            ),
        )

        rows.forEach { row ->
            val status = intervalActiveWindowCalculator.statusAt(rule, row.now)

            assertEquals(row.name, row.expectedPosition, status.position)
            assertEquals(row.name, row.expectedNextWindowStart, status.nextWindow?.startsAt)
        }
    }

    @Test
    fun intervalNextEventMatrixCoversSlotAndExhaustionEdges() {
        data class Row(
            val name: String,
            val now: LocalDateTime,
            val expected: LocalDateTime?,
        )

        val rule = weekdayRule(endsOn = LocalDate.of(2026, 9, 9))
        val rows = listOf(
            Row(
                name = "before window returns window start",
                now = LocalDateTime.of(2026, 9, 7, 8, 0),
                expected = LocalDateTime.of(2026, 9, 7, 9, 0),
            ),
            Row(
                name = "exact clock slot is returned",
                now = LocalDateTime.of(2026, 9, 7, 10, 30),
                expected = LocalDateTime.of(2026, 9, 7, 10, 30),
            ),
            Row(
                name = "inside window advances to next clock slot",
                now = LocalDateTime.of(2026, 9, 7, 10, 31),
                expected = LocalDateTime.of(2026, 9, 7, 12, 0),
            ),
            Row(
                name = "no remaining same-day slot rolls to next eligible window",
                now = LocalDateTime.of(2026, 9, 7, 16, 46),
                expected = LocalDateTime.of(2026, 9, 9, 9, 0),
            ),
            Row(
                name = "no remaining slot on final eligible date returns null",
                now = LocalDateTime.of(2026, 9, 9, 16, 46),
                expected = null,
            ),
        )

        rows.forEach { row ->
            assertEquals(
                row.name,
                row.expected,
                intervalNextEventCalculator.nextEventAtOrAfter(rule, row.now),
            )
        }
    }

    @Test
    fun schedulerFacingIntervalMatrixCoversDstSpringForwardConversion() {
        val zoneId = ZoneId.of("America/New_York")
        val task = Task(
            id = TaskId("task-dst-interval"),
            title = "Hydrate",
            schedule = TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(3, 30),
                        endsAt = LocalTime.of(6, 30),
                    ),
                    eligibleDays = setOf(DayOfWeek.SUNDAY),
                    startsOn = LocalDate.of(2026, 3, 8),
                ),
            ),
        )

        val occurrence = NextOccurrenceCalculator().calculate(
            task = task,
            now = Instant.parse("2026-03-08T06:00:00Z"),
            zoneId = zoneId,
        )

        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), occurrence?.fireAt)
    }

    @Test
    fun schedulerFacingRecurrenceStillDoesNotCreateAlarmOccurrence() {
        val task = Task(
            id = TaskId("task-recurrence"),
            title = "Review",
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.daily(startsOn = LocalDate.of(2026, 9, 7)),
            ),
        )

        val occurrence = NextOccurrenceCalculator().calculate(
            task = task,
            now = Instant.parse("2026-09-07T08:00:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )

        assertNull(occurrence)
    }

    private fun weekdayRule(
        endsOn: LocalDate? = null,
    ): IntervalRule = IntervalRule(
        every = Duration.ofMinutes(90),
        activeWindow = LocalTimeRange(
            startsAt = LocalTime.of(9, 0),
            endsAt = LocalTime.of(17, 0),
        ),
        eligibleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        startsOn = LocalDate.of(2026, 9, 7),
        endsOn = endsOn,
    )
}
