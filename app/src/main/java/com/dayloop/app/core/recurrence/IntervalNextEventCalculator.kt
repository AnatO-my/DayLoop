package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.IntervalRule
import java.time.LocalDateTime

class IntervalNextEventCalculator(
    private val activeWindowCalculator: IntervalActiveWindowCalculator = IntervalActiveWindowCalculator(),
) {
    fun nextEventAtOrAfter(
        rule: IntervalRule,
        now: LocalDateTime,
    ): LocalDateTime? {
        val window = activeWindowCalculator.nextWindowOnOrAfter(rule, now) ?: return null
        if (!now.isAfter(window.startsAt)) return window.startsAt

        var slot = window.startsAt
        while (slot.isBefore(now)) {
            slot = slot.plus(rule.every)
        }
        return slot.takeIf { candidate -> candidate.isBefore(window.endsAt) }
            ?: activeWindowCalculator.nextWindowOnOrAfter(rule, window.endsAt)?.startsAt
    }
}
