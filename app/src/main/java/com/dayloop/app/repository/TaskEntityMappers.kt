package com.dayloop.app.repository

import com.dayloop.app.core.database.TaskEntity
import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskScheduleKind
import com.dayloop.app.core.model.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun Task.toEntity(): TaskEntity {
    val base = TaskEntity(
        id = id.value,
        title = title,
        status = status.name,
        priority = priority.name,
        reminderMode = reminderPolicy.mode.name,
        realertIntervalMillis = reminderPolicy.realertInterval?.toMillis(),
        maximumRealerts = reminderPolicy.maximumRealerts,
        snoozeAllowed = reminderPolicy.snoozeAllowed,
        rescheduleAllowed = reminderPolicy.rescheduleAllowed,
    )

    return when (val taskSchedule = schedule) {
        null -> base
        is TaskSchedule.AllDay -> base.copy(
            scheduleKind = taskSchedule.kind.name,
            allDayDate = taskSchedule.date.toString(),
        )
        is TaskSchedule.Deadline -> base.copy(
            scheduleKind = taskSchedule.kind.name,
            deadlineAt = taskSchedule.dueAt.toString(),
        )
        is TaskSchedule.TimeBlock -> base.copy(
            scheduleKind = taskSchedule.kind.name,
            timeBlockStartsAt = taskSchedule.startsAt.toString(),
            timeBlockEndsAt = taskSchedule.endsAt.toString(),
        )
        is TaskSchedule.Interval -> base.copy(
            scheduleKind = taskSchedule.kind.name,
            intervalEveryMillis = taskSchedule.rule.every.toMillis(),
            intervalWindowStartsAt = taskSchedule.rule.activeWindow.startsAt.toString(),
            intervalWindowEndsAt = taskSchedule.rule.activeWindow.endsAt.toString(),
            intervalEligibleDays = taskSchedule.rule.eligibleDays.toStorageString(),
            intervalStartsOn = taskSchedule.rule.startsOn.toString(),
            intervalEndsOn = taskSchedule.rule.endsOn?.toString(),
            intervalAnchor = taskSchedule.rule.anchor.name,
        )
        is TaskSchedule.CalendarRecurrence -> base.copy(
            scheduleKind = taskSchedule.kind.name,
            recurrenceFrequency = taskSchedule.rule.frequency.name,
            recurrenceEvery = taskSchedule.rule.every,
            recurrenceStartsOn = taskSchedule.rule.startsOn.toString(),
            recurrenceEndsOn = taskSchedule.rule.endsOn?.toString(),
            recurrenceDaysOfWeek = taskSchedule.rule.daysOfWeek.toStorageString(),
        )
    }
}

internal fun TaskEntity.toDomain(): Task = Task(
    id = TaskId(id),
    title = title,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    schedule = toSchedule(),
    reminderPolicy = toReminderPolicy(),
)

private fun TaskEntity.toSchedule(): TaskSchedule? {
    val kind = scheduleKind?.let(TaskScheduleKind::valueOf) ?: return null

    return when (kind) {
        TaskScheduleKind.ALL_DAY -> TaskSchedule.AllDay(
            date = LocalDate.parse(allDayDate.requireColumn("all_day_date", kind)),
        )
        TaskScheduleKind.DEADLINE -> TaskSchedule.Deadline(
            dueAt = LocalDateTime.parse(deadlineAt.requireColumn("deadline_at", kind)),
        )
        TaskScheduleKind.TIME_BLOCK -> TaskSchedule.TimeBlock(
            startsAt = LocalDateTime.parse(timeBlockStartsAt.requireColumn("time_block_starts_at", kind)),
            endsAt = LocalDateTime.parse(timeBlockEndsAt.requireColumn("time_block_ends_at", kind)),
        )
        TaskScheduleKind.INTERVAL -> TaskSchedule.Interval(
            rule = IntervalRule(
                every = Duration.ofMillis(intervalEveryMillis.requireColumn("interval_every_millis", kind)),
                activeWindow = LocalTimeRange(
                    startsAt = LocalTime.parse(intervalWindowStartsAt.requireColumn("interval_window_starts_at", kind)),
                    endsAt = LocalTime.parse(intervalWindowEndsAt.requireColumn("interval_window_ends_at", kind)),
                ),
                eligibleDays = intervalEligibleDays.requireColumn("interval_eligible_days", kind).toDayOfWeekSet(),
                startsOn = LocalDate.parse(intervalStartsOn.requireColumn("interval_starts_on", kind)),
                endsOn = intervalEndsOn?.let(LocalDate::parse),
                anchor = IntervalAnchor.valueOf(intervalAnchor.requireColumn("interval_anchor", kind)),
            ),
        )
        TaskScheduleKind.CALENDAR_RECURRENCE -> TaskSchedule.CalendarRecurrence(
            rule = RecurrenceRule(
                frequency = RecurrenceFrequency.valueOf(
                    recurrenceFrequency.requireColumn("recurrence_frequency", kind),
                ),
                every = recurrenceEvery.requireColumn("recurrence_every", kind),
                startsOn = LocalDate.parse(recurrenceStartsOn.requireColumn("recurrence_starts_on", kind)),
                endsOn = recurrenceEndsOn?.let(LocalDate::parse),
                daysOfWeek = recurrenceDaysOfWeek?.toDayOfWeekSet() ?: emptySet(),
            ),
        )
    }
}

private fun TaskEntity.toReminderPolicy(): ReminderPolicy = ReminderPolicy(
    mode = ReminderMode.valueOf(reminderMode),
    realertInterval = realertIntervalMillis?.let(Duration::ofMillis),
    maximumRealerts = maximumRealerts,
    snoozeAllowed = snoozeAllowed,
    rescheduleAllowed = rescheduleAllowed,
)

private fun Set<DayOfWeek>.toStorageString(): String? = takeIf { it.isNotEmpty() }
    ?.sortedBy { day -> day.value }
    ?.joinToString(separator = ",") { day -> day.name }

private fun String.toDayOfWeekSet(): Set<DayOfWeek> = split(",")
    .filter { value -> value.isNotBlank() }
    .map { value -> DayOfWeek.valueOf(value) }
    .toSet()

private fun <T : Any> T?.requireColumn(columnName: String, kind: TaskScheduleKind): T = requireNotNull(this) {
    "${kind.name} schedule requires $columnName."
}
