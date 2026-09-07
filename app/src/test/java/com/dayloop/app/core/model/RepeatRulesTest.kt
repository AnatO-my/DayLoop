package com.dayloop.app.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RepeatRulesTest {
    @Test
    fun intervalScheduleKeepsDurationWindowEligibleDaysAndBounds() {
        val rule = IntervalRule(
            every = Duration.ofMinutes(90),
            activeWindow = LocalTimeRange(
                startsAt = LocalTime.of(9, 0),
                endsAt = LocalTime.of(17, 0),
            ),
            eligibleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            startsOn = LocalDate.of(2026, 8, 31),
            endsOn = LocalDate.of(2026, 9, 30),
            anchor = IntervalAnchor.COMPLETION,
        )

        val schedule = TaskSchedule.Interval(rule = rule)

        assertEquals(TaskScheduleKind.INTERVAL, schedule.kind)
        assertEquals(Duration.ofMinutes(90), schedule.rule.every)
        assertEquals(LocalTime.of(9, 0), schedule.rule.activeWindow.startsAt)
        assertEquals(LocalTime.of(17, 0), schedule.rule.activeWindow.endsAt)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), schedule.rule.eligibleDays)
        assertEquals(LocalDate.of(2026, 8, 31), schedule.rule.startsOn)
        assertEquals(LocalDate.of(2026, 9, 30), schedule.rule.endsOn)
        assertEquals(IntervalAnchor.COMPLETION, schedule.rule.anchor)
    }

    @Test
    fun intervalRuleRejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            baseIntervalRule(every = Duration.ZERO)
        }

        assertThrows(IllegalArgumentException::class.java) {
            baseIntervalRule(every = Duration.ofMinutes(-15))
        }
    }

    @Test
    fun intervalRuleRejectsEmptyEligibleDays() {
        assertThrows(IllegalArgumentException::class.java) {
            baseIntervalRule(eligibleDays = emptySet())
        }
    }

    @Test
    fun intervalRuleRejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException::class.java) {
            baseIntervalRule(
                startsOn = LocalDate.of(2026, 9, 2),
                endsOn = LocalDate.of(2026, 9, 1),
            )
        }
    }

    @Test
    fun localTimeRangeRejectsEqualOrReversedTimes() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalTimeRange(
                startsAt = LocalTime.of(9, 0),
                endsAt = LocalTime.of(9, 0),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            LocalTimeRange(
                startsAt = LocalTime.of(17, 0),
                endsAt = LocalTime.of(9, 0),
            )
        }
    }

    @Test
    fun calendarRecurrenceScheduleKeepsRuleSeparateFromIntervalSchedule() {
        val rule = RecurrenceRule.weekly(
            startsOn = LocalDate.of(2026, 8, 31),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            every = 2,
            endsOn = LocalDate.of(2026, 12, 31),
        )

        val schedule = TaskSchedule.CalendarRecurrence(rule = rule)

        assertEquals(TaskScheduleKind.CALENDAR_RECURRENCE, schedule.kind)
        assertEquals(RecurrenceFrequency.WEEKLY, schedule.rule.frequency)
        assertEquals(2, schedule.rule.every)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), schedule.rule.daysOfWeek)
    }

    @Test
    fun dailyMonthlyAndYearlyRecurrencesUseStartDateAsCalendarAnchor() {
        val startsOn = LocalDate.of(2026, 1, 31)

        assertEquals(RecurrenceFrequency.DAILY, RecurrenceRule.daily(startsOn).frequency)
        assertEquals(RecurrenceFrequency.MONTHLY, RecurrenceRule.monthly(startsOn).frequency)
        assertEquals(RecurrenceFrequency.YEARLY, RecurrenceRule.yearly(startsOn).frequency)
        assertEquals(startsOn, RecurrenceRule.monthly(startsOn).startsOn)
    }

    @Test
    fun recurrenceRuleRejectsNonPositiveEveryValue() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRule.daily(
                startsOn = LocalDate.of(2026, 8, 31),
                every = 0,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRule.weekly(
                startsOn = LocalDate.of(2026, 8, 31),
                daysOfWeek = setOf(DayOfWeek.MONDAY),
                every = -1,
            )
        }
    }

    @Test
    fun recurrenceRuleRejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRule.monthly(
                startsOn = LocalDate.of(2026, 9, 1),
                endsOn = LocalDate.of(2026, 8, 31),
            )
        }
    }

    @Test
    fun weeklyRecurrenceRequiresEligibleDays() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRule.weekly(
                startsOn = LocalDate.of(2026, 8, 31),
                daysOfWeek = emptySet(),
            )
        }
    }

    @Test
    fun onlyWeeklyRecurrenceCanConfigureDaysOfWeekInThisSlice() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRule(
                frequency = RecurrenceFrequency.MONTHLY,
                startsOn = LocalDate.of(2026, 8, 31),
                daysOfWeek = setOf(DayOfWeek.MONDAY),
            )
        }
    }

    private fun baseIntervalRule(
        every: Duration = Duration.ofHours(2),
        eligibleDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
        startsOn: LocalDate = LocalDate.of(2026, 8, 31),
        endsOn: LocalDate? = null,
    ): IntervalRule = IntervalRule(
        every = every,
        activeWindow = LocalTimeRange(
            startsAt = LocalTime.of(9, 0),
            endsAt = LocalTime.of(17, 0),
        ),
        eligibleDays = eligibleDays,
        startsOn = startsOn,
        endsOn = endsOn,
    )
}
