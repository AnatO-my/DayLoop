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

class IntervalNextEventCalculatorTest {
    private val calculator = IntervalNextEventCalculator()

    @Test
    fun returnsWindowStartWhenNowIsBeforeFirstEligibleSlot() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 7, 30),
        )

        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), next)
    }

    @Test
    fun returnsCurrentSlotWhenNowMatchesClockAnchoredSlot() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 8, 50),
        )

        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 50), next)
    }

    @Test
    fun advancesToNextClockAnchoredSlotInsideWindow() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 9, 5),
        )

        assertEquals(LocalDateTime.of(2026, 9, 7, 9, 40), next)
    }

    @Test
    fun skipsToNextEligibleDateWhenWindowIsClosed() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 20, 0),
        )

        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), next)
    }

    @Test
    fun rollsToNextWindowWhenNoClockAnchoredSlotRemainsBeforeClose() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 19, 55),
        )

        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), next)
    }
    @Test
    fun skipsIneligibleDays() {
        val rule = weekdayRule()

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 12, 9, 0),
        )

        assertEquals(LocalDateTime.of(2026, 9, 14, 8, 0), next)
    }

    @Test
    fun returnsNullWhenEndDateHasPassed() {
        val rule = weekdayRule(endsOn = LocalDate.of(2026, 9, 7))

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 20, 0),
        )

        assertNull(next)
    }

    @Test
    fun completionAnchoredRuleReturnsNullUntilAcknowledgementStateExists() {
        val rule = weekdayRule(anchor = IntervalAnchor.COMPLETION)

        val next = calculator.nextEventAtOrAfter(
            rule = rule,
            now = LocalDateTime.of(2026, 9, 7, 9, 0),
        )

        assertNull(next)
    }

    private fun weekdayRule(
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
        startsOn = LocalDate.of(2026, 9, 7),
        endsOn = endsOn,
        anchor = anchor,
    )
}

