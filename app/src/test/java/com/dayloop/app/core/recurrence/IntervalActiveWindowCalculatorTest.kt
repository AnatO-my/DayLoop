package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalActiveWindowCalculatorTest {
    private val calculator = IntervalActiveWindowCalculator()

    @Test
    fun beforeStartDateFindsFirstEligibleWindow() {
        val rule = weekdayRule(startsOn = LocalDate.of(2026, 9, 9))

        val status = calculator.statusAt(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 10, 0),
        )

        assertEquals(IntervalActiveWindowPosition.OUTSIDE_DATE_RANGE, status.position)
        assertNull(status.currentWindow)
        assertEquals(
            IntervalActiveWindow(
                startsAt = LocalDateTime.of(2026, 9, 9, 8, 0),
                endsAt = LocalDateTime.of(2026, 9, 9, 20, 0),
            ),
            status.nextWindow,
        )
    }

    @Test
    fun beforeWindowReportsCurrentWindowAsNextWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(),
            now = LocalDateTime.of(2026, 9, 7, 7, 30),
        )

        assertEquals(IntervalActiveWindowPosition.BEFORE_WINDOW, status.position)
        assertEquals(status.currentWindow, status.nextWindow)
        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), status.nextWindow?.startsAt)
    }

    @Test
    fun windowStartIsInsideWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(),
            now = LocalDateTime.of(2026, 9, 7, 8, 0),
        )

        assertEquals(IntervalActiveWindowPosition.INSIDE_WINDOW, status.position)
        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), status.currentWindow?.startsAt)
        assertEquals(LocalDateTime.of(2026, 9, 7, 20, 0), status.currentWindow?.endsAt)
    }

    @Test
    fun windowEndIsAfterWindowAndRollsToNextEligibleWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(),
            now = LocalDateTime.of(2026, 9, 7, 20, 0),
        )

        assertEquals(IntervalActiveWindowPosition.AFTER_WINDOW, status.position)
        assertEquals(LocalDateTime.of(2026, 9, 7, 20, 0), status.currentWindow?.endsAt)
        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), status.nextWindow?.startsAt)
    }

    @Test
    fun ineligibleDayReportsNextEligibleWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(),
            now = LocalDateTime.of(2026, 9, 12, 10, 0),
        )

        assertEquals(IntervalActiveWindowPosition.INELIGIBLE_DAY, status.position)
        assertNull(status.currentWindow)
        assertEquals(LocalDateTime.of(2026, 9, 14, 8, 0), status.nextWindow?.startsAt)
    }

    @Test
    fun afterEndDateHasNoNextWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(endsOn = LocalDate.of(2026, 9, 7)),
            now = LocalDateTime.of(2026, 9, 8, 10, 0),
        )

        assertEquals(IntervalActiveWindowPosition.OUTSIDE_DATE_RANGE, status.position)
        assertNull(status.currentWindow)
        assertNull(status.nextWindow)
    }

    @Test
    fun unsupportedAnchorHasNoWindow() {
        val status = calculator.statusAt(
            rule = weekdayRule(anchor = IntervalAnchor.COMPLETION),
            now = LocalDateTime.of(2026, 9, 7, 10, 0),
        )

        assertEquals(IntervalActiveWindowPosition.UNSUPPORTED_ANCHOR, status.position)
        assertNull(status.currentWindow)
        assertNull(status.nextWindow)
    }

    private fun weekdayRule(
        startsOn: LocalDate = LocalDate.of(2026, 9, 7),
        endsOn: LocalDate? = null,
        anchor: IntervalAnchor = IntervalAnchor.SCHEDULED_START,
    ): IntervalRule = IntervalRule(
        every = Duration.ofMinutes(50),
        activeWindow = LocalTimeRange(
            startsAt = LocalTime.of(8, 0),
            endsAt = LocalTime.of(20, 0),
        ),
        eligibleDays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ),
        startsOn = startsOn,
        endsOn = endsOn,
        anchor = anchor,
    )
}
