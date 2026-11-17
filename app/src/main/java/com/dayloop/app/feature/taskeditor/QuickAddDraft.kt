package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val BlankTitleError = "Enter a task title"

enum class QuickAddScheduleMode(val label: String) {
    ALL_DAY("Today"),
    DEADLINE("Due +1h"),
    TIME_BLOCK("1h Block"),
    INTERVAL("Every 45m"),
    FOCUS("⚡ Focus"),
}

data class QuickAddDraft(
    val title: String = "",
    val scheduleMode: QuickAddScheduleMode = QuickAddScheduleMode.ALL_DAY,
    val errorMessage: String? = null,
) {
    val normalizedTitle: String = title.trim()
    val canSubmit: Boolean = normalizedTitle.isNotEmpty()

    fun onTitleChanged(title: String): QuickAddDraft = copy(
        title = title,
        errorMessage = null,
    )

    fun onScheduleModeChanged(mode: QuickAddScheduleMode): QuickAddDraft = copy(
        scheduleMode = mode,
    )

    fun submit(
        createTaskId: () -> TaskId = { TaskId.generate() },
        todayDate: LocalDate = LocalDate.now(),
        nowTime: LocalTime = LocalTime.now(),
    ): QuickAddSubmitResult {
        if (!canSubmit) {
            return QuickAddSubmitResult.Invalid(
                draft = copy(errorMessage = BlankTitleError),
            )
        }

        val currentDateTime = LocalDateTime.of(todayDate, nowTime)

        val (schedule, reminderPolicy, startFocus) = when (scheduleMode) {
            QuickAddScheduleMode.ALL_DAY -> Triple(
                TaskSchedule.AllDay(date = todayDate),
                ReminderPolicy.normal(),
                false,
            )
            QuickAddScheduleMode.DEADLINE -> {
                val dueAt = currentDateTime.plusHours(1)
                Triple(
                    TaskSchedule.Deadline(dueAt = dueAt),
                    ReminderPolicy.normal(),
                    false,
                )
            }
            QuickAddScheduleMode.TIME_BLOCK -> {
                val startsAt = currentDateTime
                val endsAt = currentDateTime.plusHours(1)
                Triple(
                    TaskSchedule.TimeBlock(startsAt = startsAt, endsAt = endsAt),
                    ReminderPolicy.focus(),
                    false,
                )
            }
            QuickAddScheduleMode.INTERVAL -> {
                val windowStart = nowTime.withMinute(0).withSecond(0).withNano(0)
                val windowEnd = LocalTime.of(23, 59)
                Triple(
                    TaskSchedule.Interval(
                        rule = IntervalRule(
                            every = Duration.ofMinutes(45),
                            activeWindow = LocalTimeRange(startsAt = windowStart, endsAt = windowEnd),
                            eligibleDays = DayOfWeek.entries.toSet(),
                            startsOn = todayDate,
                            anchor = IntervalAnchor.SCHEDULED_START,
                        ),
                    ),
                    ReminderPolicy.normal(),
                    false,
                )
            }
            QuickAddScheduleMode.FOCUS -> {
                val startsAt = currentDateTime
                val endsAt = currentDateTime.plusMinutes(25)
                Triple(
                    TaskSchedule.TimeBlock(startsAt = startsAt, endsAt = endsAt),
                    ReminderPolicy.focus(),
                    true,
                )
            }
        }

        return QuickAddSubmitResult.Submitted(
            task = Task(
                id = createTaskId(),
                title = normalizedTitle,
                schedule = schedule,
                reminderPolicy = reminderPolicy,
            ),
            startFocus = startFocus,
            nextDraft = QuickAddDraft(),
        )
    }
}

sealed interface QuickAddSubmitResult {
    data class Submitted(
        val task: Task,
        val startFocus: Boolean = false,
        val nextDraft: QuickAddDraft,
    ) : QuickAddSubmitResult

    data class Invalid(
        val draft: QuickAddDraft,
    ) : QuickAddSubmitResult
}
