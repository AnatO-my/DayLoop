package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecurrencePreviewCalculatorTest {
    private val calculator = RecurrencePreviewCalculator()

    @Test
    fun previewIncludesSearchDateWhenItIsOnCadence() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
            every = 2,
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2026, 9, 5),
            count = 3,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 9),
            ),
            preview,
        )
    }

    @Test
    fun previewStartsAtRuleStartWhenSearchDateIsEarlier() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 10),
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2026, 9, 7),
            count = 2,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 11),
            ),
            preview,
        )
    }

    @Test
    fun previewStopsAtEndDate() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
            endsOn = LocalDate.of(2026, 9, 3),
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2026, 9, 1),
            count = 5,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3),
            ),
            preview,
        )
    }

    @Test
    fun previewHandlesWeeklySelectedDaysInOrder() {
        val rule = RecurrenceRule.weekly(
            startsOn = LocalDate.of(2026, 9, 7),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2026, 9, 8),
            count = 4,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 21),
            ),
            preview,
        )
    }

    @Test
    fun previewUsesMonthlyShortMonthClamping() {
        val rule = RecurrenceRule.monthly(
            startsOn = LocalDate.of(2026, 1, 31),
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2026, 2, 1),
            count = 3,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            preview,
        )
    }

    @Test
    fun previewUsesYearlyLeapDateClamping() {
        val rule = RecurrenceRule.yearly(
            startsOn = LocalDate.of(2028, 2, 29),
        )

        val preview = calculator.previewDates(
            rule = rule,
            fromDate = LocalDate.of(2029, 1, 1),
            count = 4,
        )

        assertEquals(
            listOf(
                LocalDate.of(2029, 2, 28),
                LocalDate.of(2030, 2, 28),
                LocalDate.of(2031, 2, 28),
                LocalDate.of(2032, 2, 29),
            ),
            preview,
        )
    }

    @Test
    fun previewRejectsNonPositiveCount() {
        val rule = RecurrenceRule.daily(
            startsOn = LocalDate.of(2026, 9, 1),
        )

        assertThrows(IllegalArgumentException::class.java) {
            calculator.previewDates(
                rule = rule,
                fromDate = LocalDate.of(2026, 9, 1),
                count = 0,
            )
        }
    }
}
