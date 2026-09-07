package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.RecurrenceRule
import java.time.LocalDate

class RecurrencePreviewCalculator(
    private val nextDateCalculator: RecurrenceNextDateCalculator = RecurrenceNextDateCalculator(),
) {
    fun previewDates(
        rule: RecurrenceRule,
        fromDate: LocalDate,
        count: Int,
    ): List<LocalDate> {
        require(count > 0) { "Preview count must be positive." }

        val dates = mutableListOf<LocalDate>()
        var searchDate = fromDate

        while (dates.size < count) {
            val nextDate = nextDateCalculator.nextDateOnOrAfter(
                rule = rule,
                date = searchDate,
            ) ?: break

            dates += nextDate
            searchDate = nextDate.plusDays(1)
        }

        return dates
    }
}
