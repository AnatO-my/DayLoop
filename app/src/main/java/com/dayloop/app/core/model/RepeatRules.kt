package com.dayloop.app.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class LocalTimeRange(
    val startsAt: LocalTime,
    val endsAt: LocalTime,
) {
    init {
        require(startsAt.isBefore(endsAt)) {
            "Local time range start must be before end."
        }
    }
}

data class IntervalRule(
    val every: Duration,
    val activeWindow: LocalTimeRange,
    val eligibleDays: Set<DayOfWeek>,
    val startsOn: LocalDate,
    val endsOn: LocalDate? = null,
    val anchor: IntervalAnchor = IntervalAnchor.SCHEDULED_START,
) {
    init {
        require(every.isPositive()) { "Interval duration must be positive." }
        require(eligibleDays.isNotEmpty()) { "Interval eligible days must not be empty." }
        require(endsOn == null || !endsOn.isBefore(startsOn)) {
            "Interval end date must not be before start date."
        }
    }
}

enum class IntervalAnchor {
    SCHEDULED_START,
    COMPLETION,
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val every: Int = 1,
    val startsOn: LocalDate,
    val endsOn: LocalDate? = null,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
) {
    init {
        require(every > 0) { "Recurrence interval must be positive." }
        require(endsOn == null || !endsOn.isBefore(startsOn)) {
            "Recurrence end date must not be before start date."
        }
        require(frequency != RecurrenceFrequency.WEEKLY || daysOfWeek.isNotEmpty()) {
            "Weekly recurrence requires at least one day of week."
        }
        require(frequency == RecurrenceFrequency.WEEKLY || daysOfWeek.isEmpty()) {
            "Only weekly recurrence can configure days of week in this model slice."
        }
    }

    companion object {
        fun daily(
            startsOn: LocalDate,
            every: Int = 1,
            endsOn: LocalDate? = null,
        ): RecurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.DAILY,
            every = every,
            startsOn = startsOn,
            endsOn = endsOn,
        )

        fun weekly(
            startsOn: LocalDate,
            daysOfWeek: Set<DayOfWeek>,
            every: Int = 1,
            endsOn: LocalDate? = null,
        ): RecurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.WEEKLY,
            every = every,
            startsOn = startsOn,
            endsOn = endsOn,
            daysOfWeek = daysOfWeek,
        )

        fun monthly(
            startsOn: LocalDate,
            every: Int = 1,
            endsOn: LocalDate? = null,
        ): RecurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.MONTHLY,
            every = every,
            startsOn = startsOn,
            endsOn = endsOn,
        )

        fun yearly(
            startsOn: LocalDate,
            every: Int = 1,
            endsOn: LocalDate? = null,
        ): RecurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.YEARLY,
            every = every,
            startsOn = startsOn,
            endsOn = endsOn,
        )
    }
}

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

private fun Duration.isPositive(): Boolean = !isZero && !isNegative
