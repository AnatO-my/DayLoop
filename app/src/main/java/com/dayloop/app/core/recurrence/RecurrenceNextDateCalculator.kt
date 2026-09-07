package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class RecurrenceNextDateCalculator {
    fun nextDateOnOrAfter(
        rule: RecurrenceRule,
        date: LocalDate,
    ): LocalDate? {
        val searchDate = maxOf(date, rule.startsOn)
        val next = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> nextDailyDate(rule, searchDate)
            RecurrenceFrequency.WEEKLY -> nextWeeklyDate(rule, searchDate)
            RecurrenceFrequency.MONTHLY -> nextMonthlyDate(rule, searchDate)
            RecurrenceFrequency.YEARLY -> nextYearlyDate(rule, searchDate)
        } ?: return null

        return next.takeIf { candidate -> rule.endsOn == null || !candidate.isAfter(rule.endsOn) }
    }

    private fun nextDailyDate(
        rule: RecurrenceRule,
        searchDate: LocalDate,
    ): LocalDate {
        val daysSinceStart = ChronoUnit.DAYS.between(rule.startsOn, searchDate)
        val remainder = daysSinceStart.floorMod(rule.every.toLong())
        return if (remainder == 0L) {
            searchDate
        } else {
            searchDate.plusDays(rule.every.toLong() - remainder)
        }
    }

    private fun nextWeeklyDate(
        rule: RecurrenceRule,
        searchDate: LocalDate,
    ): LocalDate? {
        var candidate = searchDate
        while (rule.endsOn == null || !candidate.isAfter(rule.endsOn)) {
            if (candidate.dayOfWeek in rule.daysOfWeek && rule.isEligibleWeek(candidate)) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun nextMonthlyDate(
        rule: RecurrenceRule,
        searchDate: LocalDate,
    ): LocalDate {
        val anchorMonth = YearMonth.from(rule.startsOn)
        val searchMonth = YearMonth.from(searchDate)
        val monthsSinceStart = ChronoUnit.MONTHS.between(anchorMonth, searchMonth)
        val remainder = monthsSinceStart.floorMod(rule.every.toLong())
        var candidateMonth = if (remainder == 0L) {
            searchMonth
        } else {
            searchMonth.plusMonths(rule.every.toLong() - remainder)
        }
        var candidate = candidateMonth.atClampedDay(rule.startsOn.dayOfMonth)

        if (candidate.isBefore(searchDate)) {
            candidateMonth = candidateMonth.plusMonths(rule.every.toLong())
            candidate = candidateMonth.atClampedDay(rule.startsOn.dayOfMonth)
        }

        return candidate
    }

    private fun nextYearlyDate(
        rule: RecurrenceRule,
        searchDate: LocalDate,
    ): LocalDate {
        val yearsSinceStart = searchDate.year - rule.startsOn.year
        val remainder = yearsSinceStart.toLong().floorMod(rule.every.toLong())
        var candidateYear = if (remainder == 0L) {
            searchDate.year
        } else {
            searchDate.year + (rule.every - remainder).toInt()
        }
        var candidate = clampedDate(
            year = candidateYear,
            month = rule.startsOn.month,
            dayOfMonth = rule.startsOn.dayOfMonth,
        )

        if (candidate.isBefore(searchDate)) {
            candidateYear += rule.every
            candidate = clampedDate(
                year = candidateYear,
                month = rule.startsOn.month,
                dayOfMonth = rule.startsOn.dayOfMonth,
            )
        }

        return candidate
    }

    private fun RecurrenceRule.isEligibleWeek(date: LocalDate): Boolean {
        val startOfAnchorWeek = startsOn.startOfWeek()
        val startOfCandidateWeek = date.startOfWeek()
        val weeksSinceStart = ChronoUnit.WEEKS.between(startOfAnchorWeek, startOfCandidateWeek)
        return weeksSinceStart.floorMod(every.toLong()) == 0L
    }
}

private fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

private fun Long.floorMod(divisor: Long): Long = Math.floorMod(this, divisor)

private fun YearMonth.atClampedDay(dayOfMonth: Int): LocalDate = atDay(minOf(dayOfMonth, lengthOfMonth()))

private fun clampedDate(
    year: Int,
    month: Month,
    dayOfMonth: Int,
): LocalDate = YearMonth.of(year, month).atClampedDay(dayOfMonth)

private fun maxOf(
    first: LocalDate,
    second: LocalDate,
): LocalDate = if (first.isBefore(second)) second else first
