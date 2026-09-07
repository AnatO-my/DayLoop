package com.dayloop.app.core.model

import java.time.LocalDate
import java.time.LocalDateTime

sealed interface TaskSchedule {
    val kind: TaskScheduleKind

    data class AllDay(
        val date: LocalDate,
    ) : TaskSchedule {
        override val kind: TaskScheduleKind = TaskScheduleKind.ALL_DAY
    }

    data class Deadline(
        val dueAt: LocalDateTime,
    ) : TaskSchedule {
        override val kind: TaskScheduleKind = TaskScheduleKind.DEADLINE
    }

    data class TimeBlock(
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
    ) : TaskSchedule {
        init {
            require(startsAt.isBefore(endsAt)) {
                "TimeBlock start must be before end."
            }
        }

        override val kind: TaskScheduleKind = TaskScheduleKind.TIME_BLOCK
    }

    data class Interval(
        val rule: IntervalRule,
    ) : TaskSchedule {
        override val kind: TaskScheduleKind = TaskScheduleKind.INTERVAL
    }

    data class CalendarRecurrence(
        val rule: RecurrenceRule,
    ) : TaskSchedule {
        override val kind: TaskScheduleKind = TaskScheduleKind.CALENDAR_RECURRENCE
    }
}

enum class TaskScheduleKind {
    ALL_DAY,
    DEADLINE,
    TIME_BLOCK,
    INTERVAL,
    CALENDAR_RECURRENCE,
}
