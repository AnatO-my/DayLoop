package com.dayloop.app.feature.taskeditor

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
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val TaskTitleError = "Enter a task title"
private const val DeadlineRequiredError = "Enter deadline date and time"
private const val DeadlineFormatError = "Use deadline date YYYY-MM-DD and time HH:mm"
private const val TimeBlockRequiredError = "Enter time block start and end"
private const val TimeBlockFormatError = "Use time-block dates YYYY-MM-DD and times HH:mm"
private const val TimeBlockRangeError = "Time block start must be before end"
private const val IntervalRequiredError = "Enter interval date, duration, and window"
private const val IntervalFormatError = "Use interval date YYYY-MM-DD, duration minutes, and times HH:mm"
private const val IntervalDurationError = "Interval duration must be greater than 0"
private const val IntervalWindowRangeError = "Interval window start must be before end"
private const val RecurrenceRequiredError = "Enter recurrence start date and repeat interval"
private const val RecurrenceFormatError = "Use recurrence dates YYYY-MM-DD and a positive whole number"
private const val RecurrenceEveryError = "Recurrence interval must be greater than 0"
private const val RecurrenceRangeError = "Recurrence end date must not be before start date"
private const val WeeklyRecurrenceDaysError = "Choose at least one weekday"
private const val ReminderRequiredError = "Enter re-alert interval and count"
private const val ReminderFormatError = "Use whole numbers for reminder re-alert settings"
private const val ReminderIntervalError = "Reminder re-alert interval must be greater than 0"
private const val ReminderCountError = "Reminder re-alert count must be greater than 0"
private const val FocusReminderScheduleError = "Focus reminders require a time block"
private val LocalTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class TaskEditorDraft(
    val title: String = "",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val deadlineEnabled: Boolean = false,
    val deadlineDate: String = "",
    val deadlineTime: String = "",
    val timeBlockEnabled: Boolean = false,
    val timeBlockStartDate: String = "",
    val timeBlockStartTime: String = "",
    val timeBlockEndDate: String = "",
    val timeBlockEndTime: String = "",
    val intervalEnabled: Boolean = false,
    val intervalStartDate: String = "",
    val intervalEveryMinutes: String = "",
    val intervalWindowStartTime: String = "",
    val intervalWindowEndTime: String = "",
    val recurrenceEnabled: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val recurrenceEvery: String = "1",
    val recurrenceStartDate: String = "",
    val recurrenceEndDate: String = "",
    val recurrenceDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val reminderMode: ReminderMode = ReminderMode.NORMAL,
    val reminderRealertMinutes: String = "15",
    val reminderMaximumRealerts: String = "3",
    val reminderSnoozeAllowed: Boolean = true,
    val reminderRescheduleAllowed: Boolean = true,
    val errorMessage: String? = null,
    val deadlineErrorMessage: String? = null,
    val timeBlockErrorMessage: String? = null,
    val intervalErrorMessage: String? = null,
    val recurrenceErrorMessage: String? = null,
    val reminderPolicyErrorMessage: String? = null,
) {
    val normalizedTitle: String = title.trim()
    val canSubmit: Boolean = normalizedTitle.isNotEmpty()

    fun onTitleChanged(title: String): TaskEditorDraft = copy(
        title = title,
        errorMessage = null,
    )

    fun onPriorityChanged(priority: TaskPriority): TaskEditorDraft = copy(
        priority = priority,
    )

    fun onDeadlineEnabledChanged(enabled: Boolean): TaskEditorDraft = copy(
        deadlineEnabled = enabled,
        timeBlockEnabled = if (enabled) false else timeBlockEnabled,
        intervalEnabled = if (enabled) false else intervalEnabled,
        recurrenceEnabled = if (enabled) false else recurrenceEnabled,
        deadlineErrorMessage = null,
        timeBlockErrorMessage = if (enabled) null else timeBlockErrorMessage,
        intervalErrorMessage = if (enabled) null else intervalErrorMessage,
        recurrenceErrorMessage = if (enabled) null else recurrenceErrorMessage,
        reminderPolicyErrorMessage = null,
    )

    fun onDeadlineDateChanged(date: String): TaskEditorDraft = copy(
        deadlineDate = date,
        deadlineErrorMessage = null,
    )

    fun onDeadlineTimeChanged(time: String): TaskEditorDraft = copy(
        deadlineTime = time,
        deadlineErrorMessage = null,
    )

    fun onTimeBlockEnabledChanged(enabled: Boolean): TaskEditorDraft = copy(
        timeBlockEnabled = enabled,
        deadlineEnabled = if (enabled) false else deadlineEnabled,
        intervalEnabled = if (enabled) false else intervalEnabled,
        recurrenceEnabled = if (enabled) false else recurrenceEnabled,
        deadlineErrorMessage = if (enabled) null else deadlineErrorMessage,
        timeBlockErrorMessage = null,
        intervalErrorMessage = if (enabled) null else intervalErrorMessage,
        recurrenceErrorMessage = if (enabled) null else recurrenceErrorMessage,
        reminderPolicyErrorMessage = null,
    )

    fun onTimeBlockStartDateChanged(date: String): TaskEditorDraft = copy(
        timeBlockStartDate = date,
        timeBlockErrorMessage = null,
    )

    fun onTimeBlockStartTimeChanged(time: String): TaskEditorDraft = copy(
        timeBlockStartTime = time,
        timeBlockErrorMessage = null,
    )

    fun onTimeBlockEndDateChanged(date: String): TaskEditorDraft = copy(
        timeBlockEndDate = date,
        timeBlockErrorMessage = null,
    )

    fun onTimeBlockEndTimeChanged(time: String): TaskEditorDraft = copy(
        timeBlockEndTime = time,
        timeBlockErrorMessage = null,
    )

    fun onIntervalEnabledChanged(enabled: Boolean): TaskEditorDraft = copy(
        intervalEnabled = enabled,
        deadlineEnabled = if (enabled) false else deadlineEnabled,
        timeBlockEnabled = if (enabled) false else timeBlockEnabled,
        recurrenceEnabled = if (enabled) false else recurrenceEnabled,
        deadlineErrorMessage = if (enabled) null else deadlineErrorMessage,
        timeBlockErrorMessage = if (enabled) null else timeBlockErrorMessage,
        intervalErrorMessage = null,
        recurrenceErrorMessage = if (enabled) null else recurrenceErrorMessage,
        reminderPolicyErrorMessage = null,
    )

    fun onIntervalStartDateChanged(date: String): TaskEditorDraft = copy(
        intervalStartDate = date,
        intervalErrorMessage = null,
    )

    fun onIntervalEveryMinutesChanged(minutes: String): TaskEditorDraft = copy(
        intervalEveryMinutes = minutes,
        intervalErrorMessage = null,
    )

    fun onIntervalWindowStartTimeChanged(time: String): TaskEditorDraft = copy(
        intervalWindowStartTime = time,
        intervalErrorMessage = null,
    )

    fun onIntervalWindowEndTimeChanged(time: String): TaskEditorDraft = copy(
        intervalWindowEndTime = time,
        intervalErrorMessage = null,
    )

    fun onRecurrenceEnabledChanged(enabled: Boolean): TaskEditorDraft = copy(
        recurrenceEnabled = enabled,
        deadlineEnabled = if (enabled) false else deadlineEnabled,
        timeBlockEnabled = if (enabled) false else timeBlockEnabled,
        intervalEnabled = if (enabled) false else intervalEnabled,
        deadlineErrorMessage = if (enabled) null else deadlineErrorMessage,
        timeBlockErrorMessage = if (enabled) null else timeBlockErrorMessage,
        intervalErrorMessage = if (enabled) null else intervalErrorMessage,
        recurrenceErrorMessage = null,
        reminderPolicyErrorMessage = null,
    )

    fun onRecurrenceFrequencyChanged(frequency: RecurrenceFrequency): TaskEditorDraft = copy(
        recurrenceFrequency = frequency,
        recurrenceDaysOfWeek = if (frequency == RecurrenceFrequency.WEEKLY) recurrenceDaysOfWeek else emptySet(),
        recurrenceErrorMessage = null,
    )

    fun onRecurrenceEveryChanged(every: String): TaskEditorDraft = copy(
        recurrenceEvery = every,
        recurrenceErrorMessage = null,
    )

    fun onRecurrenceStartDateChanged(date: String): TaskEditorDraft = copy(
        recurrenceStartDate = date,
        recurrenceErrorMessage = null,
    )

    fun onRecurrenceEndDateChanged(date: String): TaskEditorDraft = copy(
        recurrenceEndDate = date,
        recurrenceErrorMessage = null,
    )

    fun onRecurrenceDayToggled(day: DayOfWeek): TaskEditorDraft = copy(
        recurrenceDaysOfWeek = if (day in recurrenceDaysOfWeek) {
            recurrenceDaysOfWeek - day
        } else {
            recurrenceDaysOfWeek + day
        },
        recurrenceErrorMessage = null,
    )

    fun onReminderModeChanged(mode: ReminderMode): TaskEditorDraft = copy(
        reminderMode = mode,
        reminderPolicyErrorMessage = null,
    )

    fun onReminderRealertMinutesChanged(minutes: String): TaskEditorDraft = copy(
        reminderRealertMinutes = minutes,
        reminderPolicyErrorMessage = null,
    )

    fun onReminderMaximumRealertsChanged(count: String): TaskEditorDraft = copy(
        reminderMaximumRealerts = count,
        reminderPolicyErrorMessage = null,
    )

    fun onReminderSnoozeAllowedChanged(allowed: Boolean): TaskEditorDraft = copy(
        reminderSnoozeAllowed = allowed,
        reminderPolicyErrorMessage = null,
    )

    fun onReminderRescheduleAllowedChanged(allowed: Boolean): TaskEditorDraft = copy(
        reminderRescheduleAllowed = allowed,
        reminderPolicyErrorMessage = null,
    )

    fun createTask(createTaskId: () -> TaskId = { TaskId.generate() }): TaskEditorSubmitResult {
        if (!canSubmit) {
            return invalidTitle()
        }

        val schedule = when (val result = editorSchedule()) {
            is EditorScheduleResult.Invalid -> return invalidSchedule(result)
            is EditorScheduleResult.Valid -> result.schedule
        }
        val reminderPolicy = when (val result = editorReminderPolicy(schedule)) {
            is EditorReminderPolicyResult.Invalid -> return invalidReminderPolicy(result.message)
            is EditorReminderPolicyResult.Valid -> result.reminderPolicy
        }

        return TaskEditorSubmitResult.Saved(
            task = Task(
                id = createTaskId(),
                title = normalizedTitle,
                priority = priority,
                schedule = schedule,
                reminderPolicy = reminderPolicy,
            ),
        )
    }

    fun applyTo(task: Task): TaskEditorSubmitResult {
        if (!canSubmit) {
            return invalidTitle()
        }

        val existingIntervalRule = (task.schedule as? TaskSchedule.Interval)?.rule
        val schedule = when (val result = editorSchedule(existingIntervalRule)) {
            is EditorScheduleResult.Invalid -> return invalidSchedule(result)
            is EditorScheduleResult.Valid -> result.schedule ?: task.schedule
        }
        val reminderPolicy = when (val result = editorReminderPolicy(schedule)) {
            is EditorReminderPolicyResult.Invalid -> return invalidReminderPolicy(result.message)
            is EditorReminderPolicyResult.Valid -> result.reminderPolicy
        }

        return TaskEditorSubmitResult.Saved(
            task = task.copy(
                title = normalizedTitle,
                priority = priority,
                schedule = schedule,
                reminderPolicy = reminderPolicy,
            ),
        )
    }

    private fun editorSchedule(existingIntervalRule: IntervalRule? = null): EditorScheduleResult = when {
        recurrenceEnabled -> recurrenceSchedule()
        intervalEnabled -> intervalSchedule(existingIntervalRule)
        timeBlockEnabled -> timeBlockSchedule()
        deadlineEnabled -> deadlineSchedule()
        else -> EditorScheduleResult.Valid(schedule = null)
    }

    private fun deadlineSchedule(): EditorScheduleResult {
        val dateInput = deadlineDate.trim()
        val timeInput = deadlineTime.trim()
        if (dateInput.isEmpty() || timeInput.isEmpty()) {
            return EditorScheduleResult.Invalid(
                message = DeadlineRequiredError,
                target = ScheduleErrorTarget.DEADLINE,
            )
        }

        return try {
            EditorScheduleResult.Valid(TaskSchedule.Deadline(parseLocalDateTime(dateInput, timeInput)))
        } catch (_: DateTimeParseException) {
            EditorScheduleResult.Invalid(
                message = DeadlineFormatError,
                target = ScheduleErrorTarget.DEADLINE,
            )
        }
    }

    private fun timeBlockSchedule(): EditorScheduleResult {
        val startDateInput = timeBlockStartDate.trim()
        val startTimeInput = timeBlockStartTime.trim()
        val endDateInput = timeBlockEndDate.trim()
        val endTimeInput = timeBlockEndTime.trim()
        if (
            startDateInput.isEmpty() ||
            startTimeInput.isEmpty() ||
            endDateInput.isEmpty() ||
            endTimeInput.isEmpty()
        ) {
            return EditorScheduleResult.Invalid(
                message = TimeBlockRequiredError,
                target = ScheduleErrorTarget.TIME_BLOCK,
            )
        }

        return try {
            val startsAt = parseLocalDateTime(startDateInput, startTimeInput)
            val endsAt = parseLocalDateTime(endDateInput, endTimeInput)
            if (!startsAt.isBefore(endsAt)) {
                return EditorScheduleResult.Invalid(
                    message = TimeBlockRangeError,
                    target = ScheduleErrorTarget.TIME_BLOCK,
                )
            }
            EditorScheduleResult.Valid(TaskSchedule.TimeBlock(startsAt = startsAt, endsAt = endsAt))
        } catch (_: DateTimeParseException) {
            EditorScheduleResult.Invalid(
                message = TimeBlockFormatError,
                target = ScheduleErrorTarget.TIME_BLOCK,
            )
        }
    }

    private fun intervalSchedule(existingIntervalRule: IntervalRule?): EditorScheduleResult {
        val startDateInput = intervalStartDate.trim()
        val everyMinutesInput = intervalEveryMinutes.trim()
        val windowStartInput = intervalWindowStartTime.trim()
        val windowEndInput = intervalWindowEndTime.trim()
        if (
            startDateInput.isEmpty() ||
            everyMinutesInput.isEmpty() ||
            windowStartInput.isEmpty() ||
            windowEndInput.isEmpty()
        ) {
            return EditorScheduleResult.Invalid(
                message = IntervalRequiredError,
                target = ScheduleErrorTarget.INTERVAL,
            )
        }

        val everyMinutes = everyMinutesInput.toLongOrNull()
            ?: return EditorScheduleResult.Invalid(
                message = IntervalFormatError,
                target = ScheduleErrorTarget.INTERVAL,
            )
        if (everyMinutes <= 0) {
            return EditorScheduleResult.Invalid(
                message = IntervalDurationError,
                target = ScheduleErrorTarget.INTERVAL,
            )
        }

        return try {
            val startsOn = LocalDate.parse(startDateInput)
            val windowStart = LocalTime.parse(windowStartInput, LocalTimeFormatter)
            val windowEnd = LocalTime.parse(windowEndInput, LocalTimeFormatter)
            if (!windowStart.isBefore(windowEnd)) {
                return EditorScheduleResult.Invalid(
                    message = IntervalWindowRangeError,
                    target = ScheduleErrorTarget.INTERVAL,
                )
            }
            EditorScheduleResult.Valid(
                TaskSchedule.Interval(
                    rule = IntervalRule(
                        every = Duration.ofMinutes(everyMinutes),
                        activeWindow = LocalTimeRange(
                            startsAt = windowStart,
                            endsAt = windowEnd,
                        ),
                        eligibleDays = existingIntervalRule?.eligibleDays ?: DayOfWeek.values().toSet(),
                        startsOn = startsOn,
                        endsOn = existingIntervalRule?.endsOn,
                        anchor = existingIntervalRule?.anchor ?: IntervalAnchor.SCHEDULED_START,
                    ),
                ),
            )
        } catch (_: DateTimeParseException) {
            EditorScheduleResult.Invalid(
                message = IntervalFormatError,
                target = ScheduleErrorTarget.INTERVAL,
            )
        }
    }

    private fun recurrenceSchedule(): EditorScheduleResult {
        val startDateInput = recurrenceStartDate.trim()
        val everyInput = recurrenceEvery.trim()
        val endDateInput = recurrenceEndDate.trim()
        if (startDateInput.isEmpty() || everyInput.isEmpty()) {
            return EditorScheduleResult.Invalid(
                message = RecurrenceRequiredError,
                target = ScheduleErrorTarget.RECURRENCE,
            )
        }

        val every = everyInput.toIntOrNull()
            ?: return EditorScheduleResult.Invalid(
                message = RecurrenceFormatError,
                target = ScheduleErrorTarget.RECURRENCE,
            )
        if (every <= 0) {
            return EditorScheduleResult.Invalid(
                message = RecurrenceEveryError,
                target = ScheduleErrorTarget.RECURRENCE,
            )
        }
        if (recurrenceFrequency == RecurrenceFrequency.WEEKLY && recurrenceDaysOfWeek.isEmpty()) {
            return EditorScheduleResult.Invalid(
                message = WeeklyRecurrenceDaysError,
                target = ScheduleErrorTarget.RECURRENCE,
            )
        }

        return try {
            val startsOn = LocalDate.parse(startDateInput)
            val endsOn = if (endDateInput.isEmpty()) null else LocalDate.parse(endDateInput)
            if (endsOn != null && endsOn.isBefore(startsOn)) {
                return EditorScheduleResult.Invalid(
                    message = RecurrenceRangeError,
                    target = ScheduleErrorTarget.RECURRENCE,
                )
            }
            EditorScheduleResult.Valid(
                TaskSchedule.CalendarRecurrence(
                    rule = RecurrenceRule(
                        frequency = recurrenceFrequency,
                        every = every,
                        startsOn = startsOn,
                        endsOn = endsOn,
                        daysOfWeek = if (recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                            recurrenceDaysOfWeek
                        } else {
                            emptySet()
                        },
                    ),
                ),
            )
        } catch (_: DateTimeParseException) {
            EditorScheduleResult.Invalid(
                message = RecurrenceFormatError,
                target = ScheduleErrorTarget.RECURRENCE,
            )
        }
    }

    private fun editorReminderPolicy(schedule: TaskSchedule?): EditorReminderPolicyResult {
        if (reminderMode == ReminderMode.FOCUS && schedule !is TaskSchedule.TimeBlock) {
            return EditorReminderPolicyResult.Invalid(FocusReminderScheduleError)
        }
        if (!reminderMode.supportsRealerts) {
            return EditorReminderPolicyResult.Valid(
                ReminderPolicy(
                    mode = reminderMode,
                    snoozeAllowed = reminderSnoozeAllowed,
                    rescheduleAllowed = reminderRescheduleAllowed,
                ),
            )
        }

        val realertMinutesInput = reminderRealertMinutes.trim()
        val maximumRealertsInput = reminderMaximumRealerts.trim()
        if (realertMinutesInput.isEmpty() || maximumRealertsInput.isEmpty()) {
            return EditorReminderPolicyResult.Invalid(ReminderRequiredError)
        }
        val realertMinutes = realertMinutesInput.toLongOrNull()
            ?: return EditorReminderPolicyResult.Invalid(ReminderFormatError)
        val maximumRealerts = maximumRealertsInput.toIntOrNull()
            ?: return EditorReminderPolicyResult.Invalid(ReminderFormatError)
        if (realertMinutes <= 0) {
            return EditorReminderPolicyResult.Invalid(ReminderIntervalError)
        }
        if (maximumRealerts <= 0) {
            return EditorReminderPolicyResult.Invalid(ReminderCountError)
        }

        return EditorReminderPolicyResult.Valid(
            ReminderPolicy(
                mode = reminderMode,
                realertInterval = Duration.ofMinutes(realertMinutes),
                maximumRealerts = maximumRealerts,
                snoozeAllowed = reminderSnoozeAllowed,
                rescheduleAllowed = reminderRescheduleAllowed,
            ),
        )
    }

    private fun invalidTitle(): TaskEditorSubmitResult.Invalid = TaskEditorSubmitResult.Invalid(
        draft = copy(errorMessage = TaskTitleError),
    )

    private fun invalidSchedule(result: EditorScheduleResult.Invalid): TaskEditorSubmitResult.Invalid {
        val draft = when (result.target) {
            ScheduleErrorTarget.DEADLINE -> copy(deadlineErrorMessage = result.message)
            ScheduleErrorTarget.TIME_BLOCK -> copy(timeBlockErrorMessage = result.message)
            ScheduleErrorTarget.INTERVAL -> copy(intervalErrorMessage = result.message)
            ScheduleErrorTarget.RECURRENCE -> copy(recurrenceErrorMessage = result.message)
        }
        return TaskEditorSubmitResult.Invalid(draft = draft)
    }

    private fun invalidReminderPolicy(message: String): TaskEditorSubmitResult.Invalid = TaskEditorSubmitResult.Invalid(
        draft = copy(reminderPolicyErrorMessage = message),
    )

    companion object {
        fun fromTask(task: Task): TaskEditorDraft {
            val reminderPolicy = task.reminderPolicy
            val baseDraft = when (val schedule = task.schedule) {
                is TaskSchedule.Deadline -> TaskEditorDraft(
                    title = task.title,
                    priority = task.priority,
                    deadlineEnabled = true,
                    deadlineDate = schedule.dueAt.toLocalDate().toString(),
                    deadlineTime = schedule.dueAt.toLocalTime().format(LocalTimeFormatter),
                )

                is TaskSchedule.TimeBlock -> TaskEditorDraft(
                    title = task.title,
                    priority = task.priority,
                    timeBlockEnabled = true,
                    timeBlockStartDate = schedule.startsAt.toLocalDate().toString(),
                    timeBlockStartTime = schedule.startsAt.toLocalTime().format(LocalTimeFormatter),
                    timeBlockEndDate = schedule.endsAt.toLocalDate().toString(),
                    timeBlockEndTime = schedule.endsAt.toLocalTime().format(LocalTimeFormatter),
                )

                is TaskSchedule.Interval -> TaskEditorDraft(
                    title = task.title,
                    priority = task.priority,
                    intervalEnabled = true,
                    intervalStartDate = schedule.rule.startsOn.toString(),
                    intervalEveryMinutes = schedule.rule.every.toMinutes().toString(),
                    intervalWindowStartTime = schedule.rule.activeWindow.startsAt.format(LocalTimeFormatter),
                    intervalWindowEndTime = schedule.rule.activeWindow.endsAt.format(LocalTimeFormatter),
                )

                is TaskSchedule.CalendarRecurrence -> TaskEditorDraft(
                    title = task.title,
                    priority = task.priority,
                    recurrenceEnabled = true,
                    recurrenceFrequency = schedule.rule.frequency,
                    recurrenceEvery = schedule.rule.every.toString(),
                    recurrenceStartDate = schedule.rule.startsOn.toString(),
                    recurrenceEndDate = schedule.rule.endsOn?.toString().orEmpty(),
                    recurrenceDaysOfWeek = schedule.rule.daysOfWeek,
                )

                else -> TaskEditorDraft(
                    title = task.title,
                    priority = task.priority,
                )
            }
            return baseDraft.copy(
                reminderMode = reminderPolicy.mode,
                reminderRealertMinutes = reminderPolicy.realertInterval?.toMinutes()?.toString() ?: "15",
                reminderMaximumRealerts = if (reminderPolicy.maximumRealerts > 0) {
                    reminderPolicy.maximumRealerts.toString()
                } else {
                    "3"
                },
                reminderSnoozeAllowed = reminderPolicy.snoozeAllowed,
                reminderRescheduleAllowed = reminderPolicy.rescheduleAllowed,
            )
        }
    }
}

private fun parseLocalDateTime(dateInput: String, timeInput: String): LocalDateTime = LocalDateTime.of(
    LocalDate.parse(dateInput),
    LocalTime.parse(timeInput, LocalTimeFormatter),
)

private sealed interface EditorScheduleResult {
    data class Valid(
        val schedule: TaskSchedule?,
    ) : EditorScheduleResult

    data class Invalid(
        val message: String,
        val target: ScheduleErrorTarget,
    ) : EditorScheduleResult
}

private enum class ScheduleErrorTarget {
    DEADLINE,
    TIME_BLOCK,
    INTERVAL,
    RECURRENCE,
}

private sealed interface EditorReminderPolicyResult {
    data class Valid(
        val reminderPolicy: ReminderPolicy,
    ) : EditorReminderPolicyResult

    data class Invalid(
        val message: String,
    ) : EditorReminderPolicyResult
}

sealed interface TaskEditorSubmitResult {
    data class Saved(
        val task: Task,
    ) : TaskEditorSubmitResult

    data class Invalid(
        val draft: TaskEditorDraft,
    ) : TaskEditorSubmitResult
}
