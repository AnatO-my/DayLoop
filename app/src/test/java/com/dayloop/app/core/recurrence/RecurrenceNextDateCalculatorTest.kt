package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceNextDateCalculatorTest {
    private val calculator = RecurrenceNextDateCalculator()

    @Test
    fun dailyRecurrenceReturnsStartDateBeforeSeriesBegins() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 10),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 7),
        )

        assertEquals(LocalDate.of(2026, 9, 10), next)
    }

    @Test
    fun dailyRecurrenceHonorsEveryStep() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
            every = 3,
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 6),
        )

        assertEquals(LocalDate.of(2026, 9, 7), next)
    }

    @Test
    fun dailyRecurrenceReturnsSearchDateWhenItIsOnCadence() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
            every = 3,
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 10),
        )

        assertEquals(LocalDate.of(2026, 9, 10), next)
    }

    @Test
    fun weeklyRecurrenceReturnsNextSelectedDayInEligibleWeek() {
        val rule = RecurrenceRule.weekly(
            startsOn = LocalDate.of(2026, 9, 7),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 8),
        )

        assertEquals(LocalDate.of(2026, 9, 11), next)
    }

    @Test
    fun weeklyRecurrenceHonorsEveryOtherWeekFromStartWeek() {
        val rule = RecurrenceRule.weekly(
            startsOn = LocalDate.of(2026, 9, 7),
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            every = 2,
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 14),
        )

        assertEquals(LocalDate.of(2026, 9, 21), next)
    }

    @Test
    fun recurrenceStopsAtEndDate() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
            every = 3,
            endsOn = LocalDate.of(2026, 9, 5),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 9, 6),
        )

        assertNull(next)
    }

    @Test
    fun monthlyRecurrenceClampsThirtyFirstToShortMonth() {
        val rule = RecurrenceRule.monthly(
            startsOn = LocalDate.of(2026, 1, 31),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 2, 1),
        )

        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun monthlyRecurrenceReturnsToAnchorDayWhenMonthSupportsIt() {
        val rule = RecurrenceRule.monthly(
            startsOn = LocalDate.of(2026, 1, 31),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 3, 1),
        )

        assertEquals(LocalDate.of(2026, 3, 31), next)
    }

    @Test
    fun monthlyRecurrenceHonorsEveryStepFromStartMonth() {
        val rule = RecurrenceRule.monthly(
            startsOn = LocalDate.of(2026, 1, 31),
            every = 2,
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 2, 1),
        )

        assertEquals(LocalDate.of(2026, 3, 31), next)
    }

    @Test
    fun monthlyRecurrenceAppliesEndDateAfterClamping() {
        val rule = RecurrenceRule.monthly(
            startsOn = LocalDate.of(2026, 1, 31),
            endsOn = LocalDate.of(2026, 2, 27),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2026, 2, 1),
        )

        assertNull(next)
    }

    @Test
    fun yearlyRecurrenceClampsLeapDayToFebruaryTwentyEighth() {
        val rule = RecurrenceRule.yearly(
            startsOn = LocalDate.of(2028, 2, 29),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2029, 1, 1),
        )

        assertEquals(LocalDate.of(2029, 2, 28), next)
    }

    @Test
    fun yearlyRecurrenceReturnsToLeapDayWhenYearSupportsIt() {
        val rule = RecurrenceRule.yearly(
            startsOn = LocalDate.of(2028, 2, 29),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2032, 1, 1),
        )

        assertEquals(LocalDate.of(2032, 2, 29), next)
    }

    @Test
    fun yearlyRecurrenceHonorsEveryStepFromStartYear() {
        val rule = RecurrenceRule.yearly(
            startsOn = LocalDate.of(2028, 2, 29),
            every = 4,
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2029, 1, 1),
        )

        assertEquals(LocalDate.of(2032, 2, 29), next)
    }

    @Test
    fun yearlyRecurrenceAdvancesAfterClampedDatePasses() {
        val rule = RecurrenceRule.yearly(
            startsOn = LocalDate.of(2028, 2, 29),
        )

        val next = calculator.nextDateOnOrAfter(
            rule = rule,
            date = LocalDate.of(2029, 3, 1),
        )

        assertEquals(LocalDate.of(2030, 2, 28), next)
    }
}
