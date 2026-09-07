package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import java.time.LocalDate
import java.time.LocalDateTime

data class IntervalActiveWindow(
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
) {
    init {
        require(startsAt.isBefore(endsAt)) { "Interval active window start must be before end." }
    }
}

data class IntervalActiveWindowStatus(
    val position: IntervalActiveWindowPosition,
    val currentWindow: IntervalActiveWindow? = null,
    val nextWindow: IntervalActiveWindow? = null,
)

enum class IntervalActiveWindowPosition {
    BEFORE_WINDOW,
    INSIDE_WINDOW,
    AFTER_WINDOW,
    INELIGIBLE_DAY,
    OUTSIDE_DATE_RANGE,
    UNSUPPORTED_ANCHOR,
}

class IntervalActiveWindowCalculator {
    fun statusAt(
        rule: IntervalRule,
        now: LocalDateTime,
    ): IntervalActiveWindowStatus {
        if (rule.anchor != IntervalAnchor.SCHEDULED_START) {
            return IntervalActiveWindowStatus(IntervalActiveWindowPosition.UNSUPPORTED_ANCHOR)
        }

        val date = now.toLocalDate()
        if (rule.endsOn != null && date.isAfter(rule.endsOn)) {
            return IntervalActiveWindowStatus(IntervalActiveWindowPosition.OUTSIDE_DATE_RANGE)
        }

        val searchDate = maxOf(date, rule.startsOn)
        if (searchDate != date) {
            return IntervalActiveWindowStatus(
                position = IntervalActiveWindowPosition.OUTSIDE_DATE_RANGE,
                nextWindow = nextWindowStartingOnOrAfter(rule, searchDate),
            )
        }

        if (date.dayOfWeek !in rule.eligibleDays) {
            return IntervalActiveWindowStatus(
                position = IntervalActiveWindowPosition.INELIGIBLE_DAY,
                nextWindow = nextWindowStartingOnOrAfter(rule, date.plusDays(1)),
            )
        }

        val currentWindow = rule.windowOn(date)
        return when {
            now.isBefore(currentWindow.startsAt) -> IntervalActiveWindowStatus(
                position = IntervalActiveWindowPosition.BEFORE_WINDOW,
                currentWindow = currentWindow,
                nextWindow = currentWindow,
            )
            now.isBefore(currentWindow.endsAt) -> IntervalActiveWindowStatus(
                position = IntervalActiveWindowPosition.INSIDE_WINDOW,
                currentWindow = currentWindow,
                nextWindow = currentWindow,
            )
            else -> IntervalActiveWindowStatus(
                position = IntervalActiveWindowPosition.AFTER_WINDOW,
                currentWindow = currentWindow,
                nextWindow = nextWindowStartingOnOrAfter(rule, date.plusDays(1)),
            )
        }
    }

    fun nextWindowOnOrAfter(
        rule: IntervalRule,
        now: LocalDateTime,
    ): IntervalActiveWindow? = statusAt(rule, now).nextWindow

    private fun nextWindowStartingOnOrAfter(
        rule: IntervalRule,
        startDate: LocalDate,
    ): IntervalActiveWindow? {
        var date = maxOf(startDate, rule.startsOn)
        while (rule.endsOn == null || !date.isAfter(rule.endsOn)) {
            if (date.dayOfWeek in rule.eligibleDays) {
                return rule.windowOn(date)
            }
            date = date.plusDays(1)
        }
        return null
    }
}

private fun IntervalRule.windowOn(date: LocalDate): IntervalActiveWindow = IntervalActiveWindow(
    startsAt = LocalDateTime.of(date, activeWindow.startsAt),
    endsAt = LocalDateTime.of(date, activeWindow.endsAt),
)

private fun maxOf(
    first: LocalDate,
    second: LocalDate,
): LocalDate = if (first.isBefore(second)) second else first
