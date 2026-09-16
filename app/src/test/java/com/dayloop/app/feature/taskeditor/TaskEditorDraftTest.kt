package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEditorDraftTest {
    @Test
    fun planningDateCreatesDatedAnytimeTask() {
        val result = TaskEditorDraft(title = "Plan today", allDayDate = "2026-09-08")
            .createTask(createTaskId = { TaskId("planned") }) as TaskEditorSubmitResult.Saved
        assertEquals(TaskSchedule.AllDay(LocalDate.of(2026, 9, 8)), result.task.schedule)
        assertEquals("2026-09-08", TaskEditorDraft.fromTask(result.task).allDayDate)
    }

    @Test
    fun invalidPlanningDateCannotBeSaved() {
        val result = TaskEditorDraft(title = "Plan today", allDayDate = "2026-02-30")
            .createTask(createTaskId = { TaskId("invalid-date") })
        assertTrue(result is TaskEditorSubmitResult.Invalid)
    }

    @Test
    fun blankTitleReturnsValidationError() {
        val result = TaskEditorDraft(title = " ").createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter a task title", invalid.draft.errorMessage)
    }

    @Test
    fun titleChangeClearsValidationError() {
        val draft = TaskEditorDraft(
            title = "",
            errorMessage = "Enter a task title",
        )

        val changed = draft.onTitleChanged("Plan today")

        assertEquals("Plan today", changed.title)
        assertNull(changed.errorMessage)
    }

    @Test
    fun createTaskUsesTrimmedTitleAndSelectedPriority() {
        val result = TaskEditorDraft(
            title = "  Plan today  ",
            priority = TaskPriority.HIGH,
        ).createTask(createTaskId = { TaskId("new-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(TaskId("new-task"), saved.task.id)
        assertEquals("Plan today", saved.task.title)
        assertEquals(TaskPriority.HIGH, saved.task.priority)
        assertEquals(TaskStatus.ACTIVE, saved.task.status)
        assertNull(saved.task.schedule)
        assertEquals(ReminderPolicy.normal(), saved.task.reminderPolicy)
    }

    @Test
    fun createTaskWithDeadlineBuildsDeadlineSchedule() {
        val result = TaskEditorDraft(
            title = "Submit report",
            deadlineEnabled = true,
            deadlineDate = "2026-09-06",
            deadlineTime = "17:30",
        ).createTask(createTaskId = { TaskId("deadline-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            TaskSchedule.Deadline(dueAt = LocalDateTime.of(2026, 9, 6, 17, 30)),
            saved.task.schedule,
        )
    }

    @Test
    fun missingDeadlineInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Submit report",
            deadlineEnabled = true,
            deadlineDate = "2026-09-06",
            deadlineTime = " ",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter deadline date and time", invalid.draft.deadlineErrorMessage)
    }

    @Test
    fun malformedDeadlineInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Submit report",
            deadlineEnabled = true,
            deadlineDate = "09/06/2026",
            deadlineTime = "5:30 PM",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Use deadline date YYYY-MM-DD and time HH:mm", invalid.draft.deadlineErrorMessage)
    }

    @Test
    fun deadlineChangeClearsDeadlineValidationError() {
        val draft = TaskEditorDraft(
            deadlineEnabled = true,
            deadlineErrorMessage = "Enter deadline date and time",
        )

        val changed = draft.onDeadlineTimeChanged("09:45")

        assertEquals("09:45", changed.deadlineTime)
        assertNull(changed.deadlineErrorMessage)
    }

    @Test
    fun createTaskWithTimeBlockBuildsTimeBlockSchedule() {
        val result = TaskEditorDraft(
            title = "Deep work",
            timeBlockEnabled = true,
            timeBlockStartDate = "2026-09-06",
            timeBlockStartTime = "09:00",
            timeBlockEndDate = "2026-09-06",
            timeBlockEndTime = "10:30",
        ).createTask(createTaskId = { TaskId("time-block-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 9, 0),
                endsAt = LocalDateTime.of(2026, 9, 6, 10, 30),
            ),
            saved.task.schedule,
        )
    }

    @Test
    fun missingTimeBlockInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Deep work",
            timeBlockEnabled = true,
            timeBlockStartDate = "2026-09-06",
            timeBlockStartTime = "09:00",
            timeBlockEndDate = "2026-09-06",
            timeBlockEndTime = " ",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter time block start and end", invalid.draft.timeBlockErrorMessage)
    }

    @Test
    fun malformedTimeBlockInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Deep work",
            timeBlockEnabled = true,
            timeBlockStartDate = "2026-09-06",
            timeBlockStartTime = "9 AM",
            timeBlockEndDate = "2026-09-06",
            timeBlockEndTime = "10:30",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Use time-block dates YYYY-MM-DD and times HH:mm", invalid.draft.timeBlockErrorMessage)
    }

    @Test
    fun reversedTimeBlockInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Deep work",
            timeBlockEnabled = true,
            timeBlockStartDate = "2026-09-06",
            timeBlockStartTime = "10:30",
            timeBlockEndDate = "2026-09-06",
            timeBlockEndTime = "09:00",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Time block start must be before end", invalid.draft.timeBlockErrorMessage)
    }

    @Test
    fun timeBlockChangeClearsTimeBlockValidationError() {
        val draft = TaskEditorDraft(
            timeBlockEnabled = true,
            timeBlockErrorMessage = "Enter time block start and end",
        )

        val changed = draft.onTimeBlockEndTimeChanged("10:30")

        assertEquals("10:30", changed.timeBlockEndTime)
        assertNull(changed.timeBlockErrorMessage)
    }

    @Test
    fun createTaskWithIntervalBuildsIntervalSchedule() {
        val result = TaskEditorDraft(
            title = "Drink water",
            intervalEnabled = true,
            intervalStartDate = "2026-09-06",
            intervalEveryMinutes = "90",
            intervalWindowStartTime = "08:00",
            intervalWindowEndTime = "18:00",
        ).createTask(createTaskId = { TaskId("interval-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(8, 0),
                        endsAt = LocalTime.of(18, 0),
                    ),
                    eligibleDays = DayOfWeek.values().toSet(),
                    startsOn = LocalDate.of(2026, 9, 6),
                    anchor = IntervalAnchor.SCHEDULED_START,
                ),
            ),
            saved.task.schedule,
        )
    }

    @Test
    fun missingIntervalInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            intervalEnabled = true,
            intervalStartDate = "2026-09-06",
            intervalEveryMinutes = "90",
            intervalWindowStartTime = " ",
            intervalWindowEndTime = "18:00",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter interval date, duration, and window", invalid.draft.intervalErrorMessage)
    }

    @Test
    fun malformedIntervalInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            intervalEnabled = true,
            intervalStartDate = "09/06/2026",
            intervalEveryMinutes = "ninety",
            intervalWindowStartTime = "8 AM",
            intervalWindowEndTime = "18:00",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Use interval date YYYY-MM-DD, duration minutes, and times HH:mm", invalid.draft.intervalErrorMessage)
    }

    @Test
    fun nonPositiveIntervalDurationReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            intervalEnabled = true,
            intervalStartDate = "2026-09-06",
            intervalEveryMinutes = "0",
            intervalWindowStartTime = "08:00",
            intervalWindowEndTime = "18:00",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Interval duration must be greater than 0", invalid.draft.intervalErrorMessage)
    }

    @Test
    fun reversedIntervalWindowReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            intervalEnabled = true,
            intervalStartDate = "2026-09-06",
            intervalEveryMinutes = "90",
            intervalWindowStartTime = "18:00",
            intervalWindowEndTime = "08:00",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Interval window start must be before end", invalid.draft.intervalErrorMessage)
    }

    @Test
    fun intervalChangeClearsIntervalValidationError() {
        val draft = TaskEditorDraft(
            intervalEnabled = true,
            intervalErrorMessage = "Enter interval date, duration, and window",
        )

        val changed = draft.onIntervalWindowEndTimeChanged("18:00")

        assertEquals("18:00", changed.intervalWindowEndTime)
        assertNull(changed.intervalErrorMessage)
    }

    @Test
    fun enablingScheduleInputsKeepsOnlyOneScheduleInputActive() {
        val deadlineDraft = TaskEditorDraft(timeBlockEnabled = true, intervalEnabled = true, recurrenceEnabled = true)
            .onDeadlineEnabledChanged(true)

        assertTrue(deadlineDraft.deadlineEnabled)
        assertFalse(deadlineDraft.timeBlockEnabled)
        assertFalse(deadlineDraft.intervalEnabled)
        assertFalse(deadlineDraft.recurrenceEnabled)

        val timeBlockDraft = TaskEditorDraft(deadlineEnabled = true, intervalEnabled = true, recurrenceEnabled = true)
            .onTimeBlockEnabledChanged(true)

        assertFalse(timeBlockDraft.deadlineEnabled)
        assertTrue(timeBlockDraft.timeBlockEnabled)
        assertFalse(timeBlockDraft.intervalEnabled)
        assertFalse(timeBlockDraft.recurrenceEnabled)

        val intervalDraft = TaskEditorDraft(deadlineEnabled = true, timeBlockEnabled = true, recurrenceEnabled = true)
            .onIntervalEnabledChanged(true)

        assertFalse(intervalDraft.deadlineEnabled)
        assertFalse(intervalDraft.timeBlockEnabled)
        assertTrue(intervalDraft.intervalEnabled)
        assertFalse(intervalDraft.recurrenceEnabled)

        val recurrenceDraft = TaskEditorDraft(deadlineEnabled = true, timeBlockEnabled = true, intervalEnabled = true)
            .onRecurrenceEnabledChanged(true)

        assertFalse(recurrenceDraft.deadlineEnabled)
        assertFalse(recurrenceDraft.timeBlockEnabled)
        assertFalse(recurrenceDraft.intervalEnabled)
        assertTrue(recurrenceDraft.recurrenceEnabled)
    }


    @Test
    fun createTaskWithDailyRecurrenceBuildsCalendarRecurrenceSchedule() {
        val result = TaskEditorDraft(
            title = "Review plan",
            recurrenceEnabled = true,
            recurrenceFrequency = RecurrenceFrequency.DAILY,
            recurrenceEvery = "2",
            recurrenceStartDate = "2026-09-06",
            recurrenceEndDate = "2026-10-06",
        ).createTask(createTaskId = { TaskId("recurrence-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.daily(
                    startsOn = LocalDate.of(2026, 9, 6),
                    every = 2,
                    endsOn = LocalDate.of(2026, 10, 6),
                ),
            ),
            saved.task.schedule,
        )
    }

    @Test
    fun createTaskWithWeeklyRecurrenceBuildsCalendarRecurrenceSchedule() {
        val result = TaskEditorDraft(
            title = "Team review",
            recurrenceEnabled = true,
            recurrenceFrequency = RecurrenceFrequency.WEEKLY,
            recurrenceEvery = "1",
            recurrenceStartDate = "2026-09-06",
            recurrenceDaysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        ).createTask(createTaskId = { TaskId("weekly-recurrence-task") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 6),
                    every = 1,
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                ),
            ),
            saved.task.schedule,
        )
    }

    @Test
    fun missingRecurrenceInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Review plan",
            recurrenceEnabled = true,
            recurrenceEvery = " ",
            recurrenceStartDate = "2026-09-06",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter recurrence start date and repeat interval", invalid.draft.recurrenceErrorMessage)
    }

    @Test
    fun malformedRecurrenceInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Review plan",
            recurrenceEnabled = true,
            recurrenceEvery = "twice",
            recurrenceStartDate = "09/06/2026",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Use recurrence dates YYYY-MM-DD and a positive whole number", invalid.draft.recurrenceErrorMessage)
    }

    @Test
    fun nonPositiveRecurrenceIntervalReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Review plan",
            recurrenceEnabled = true,
            recurrenceEvery = "0",
            recurrenceStartDate = "2026-09-06",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Recurrence interval must be greater than 0", invalid.draft.recurrenceErrorMessage)
    }

    @Test
    fun weeklyRecurrenceWithoutDaysReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Team review",
            recurrenceEnabled = true,
            recurrenceFrequency = RecurrenceFrequency.WEEKLY,
            recurrenceEvery = "1",
            recurrenceStartDate = "2026-09-06",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Choose at least one weekday", invalid.draft.recurrenceErrorMessage)
    }

    @Test
    fun reversedRecurrenceDateRangeReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Review plan",
            recurrenceEnabled = true,
            recurrenceEvery = "1",
            recurrenceStartDate = "2026-09-06",
            recurrenceEndDate = "2026-09-05",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Recurrence end date must not be before start date", invalid.draft.recurrenceErrorMessage)
    }

    @Test
    fun recurrenceChangeClearsRecurrenceValidationError() {
        val draft = TaskEditorDraft(
            recurrenceEnabled = true,
            recurrenceErrorMessage = "Enter recurrence start date and repeat interval",
        )

        val changed = draft.onRecurrenceStartDateChanged("2026-09-06")

        assertEquals("2026-09-06", changed.recurrenceStartDate)
        assertNull(changed.recurrenceErrorMessage)
    }

    @Test
    fun changingRecurrenceFrequencyClearsNonWeeklyDays() {
        val draft = TaskEditorDraft(
            recurrenceEnabled = true,
            recurrenceFrequency = RecurrenceFrequency.WEEKLY,
            recurrenceDaysOfWeek = setOf(DayOfWeek.MONDAY),
        )

        val changed = draft.onRecurrenceFrequencyChanged(RecurrenceFrequency.MONTHLY)

        assertEquals(RecurrenceFrequency.MONTHLY, changed.recurrenceFrequency)
        assertEquals(emptySet<DayOfWeek>(), changed.recurrenceDaysOfWeek)
    }
    @Test
    fun existingTaskCanRoundTripBaseDetailsWithoutChangingScheduleOrReminder() {
        val existing = Task(
            id = TaskId("existing"),
            title = "Old title",
            status = TaskStatus.COMPLETED,
            priority = TaskPriority.LOW,
            schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 9, 5)),
            reminderPolicy = ReminderPolicy.normal(),
        )
        val draft = TaskEditorDraft.fromTask(existing)
            .onTitleChanged("  New title  ")
            .onPriorityChanged(TaskPriority.HIGH)

        val result = draft.applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(existing.id, saved.task.id)
        assertEquals("New title", saved.task.title)
        assertEquals(TaskPriority.HIGH, saved.task.priority)
        assertEquals(existing.status, saved.task.status)
        assertEquals(existing.schedule, saved.task.schedule)
        assertEquals(existing.reminderPolicy, saved.task.reminderPolicy)
    }

    @Test
    fun existingDeadlineTaskCanRoundTripDeadlineDetails() {
        val existing = Task(
            id = TaskId("existing-deadline"),
            title = "Old title",
            status = TaskStatus.COMPLETED,
            priority = TaskPriority.LOW,
            schedule = TaskSchedule.Deadline(dueAt = LocalDateTime.of(2026, 9, 6, 8, 15)),
            reminderPolicy = ReminderPolicy.normal(),
        )

        val draft = TaskEditorDraft.fromTask(existing)
        assertTrue(draft.deadlineEnabled)
        assertEquals("2026-09-06", draft.deadlineDate)
        assertEquals("08:15", draft.deadlineTime)

        val result = draft
            .onDeadlineDateChanged("2026-09-07")
            .onDeadlineTimeChanged("09:45")
            .applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(existing.id, saved.task.id)
        assertEquals(existing.status, saved.task.status)
        assertEquals(existing.reminderPolicy, saved.task.reminderPolicy)
        assertEquals(
            TaskSchedule.Deadline(dueAt = LocalDateTime.of(2026, 9, 7, 9, 45)),
            saved.task.schedule,
        )
    }

    @Test
    fun existingTimeBlockTaskCanRoundTripTimeBlockDetails() {
        val existing = Task(
            id = TaskId("existing-time-block"),
            title = "Old title",
            status = TaskStatus.COMPLETED,
            priority = TaskPriority.LOW,
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 8, 15),
                endsAt = LocalDateTime.of(2026, 9, 6, 9, 0),
            ),
            reminderPolicy = ReminderPolicy.normal(),
        )

        val draft = TaskEditorDraft.fromTask(existing)
        assertTrue(draft.timeBlockEnabled)
        assertEquals("2026-09-06", draft.timeBlockStartDate)
        assertEquals("08:15", draft.timeBlockStartTime)
        assertEquals("2026-09-06", draft.timeBlockEndDate)
        assertEquals("09:00", draft.timeBlockEndTime)

        val result = draft
            .onTimeBlockStartTimeChanged("10:00")
            .onTimeBlockEndTimeChanged("11:30")
            .applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(existing.id, saved.task.id)
        assertEquals(existing.status, saved.task.status)
        assertEquals(existing.reminderPolicy, saved.task.reminderPolicy)
        assertEquals(
            TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 10, 0),
                endsAt = LocalDateTime.of(2026, 9, 6, 11, 30),
            ),
            saved.task.schedule,
        )
    }

    @Test
    fun existingIntervalTaskCanRoundTripIntervalDetailsAndPreserveHiddenRuleFields() {
        val existingRule = IntervalRule(
            every = Duration.ofMinutes(45),
            activeWindow = LocalTimeRange(
                startsAt = LocalTime.of(9, 0),
                endsAt = LocalTime.of(15, 0),
            ),
            eligibleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            startsOn = LocalDate.of(2026, 9, 6),
            endsOn = LocalDate.of(2026, 12, 31),
            anchor = IntervalAnchor.COMPLETION,
        )
        val existing = Task(
            id = TaskId("existing-interval"),
            title = "Old title",
            status = TaskStatus.COMPLETED,
            priority = TaskPriority.LOW,
            schedule = TaskSchedule.Interval(rule = existingRule),
            reminderPolicy = ReminderPolicy.normal(),
        )

        val draft = TaskEditorDraft.fromTask(existing)
        assertTrue(draft.intervalEnabled)
        assertEquals("2026-09-06", draft.intervalStartDate)
        assertEquals("45", draft.intervalEveryMinutes)
        assertEquals("09:00", draft.intervalWindowStartTime)
        assertEquals("15:00", draft.intervalWindowEndTime)

        val result = draft
            .onIntervalEveryMinutesChanged("60")
            .onIntervalWindowStartTimeChanged("10:00")
            .onIntervalWindowEndTimeChanged("16:30")
            .applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(existing.id, saved.task.id)
        assertEquals(existing.status, saved.task.status)
        assertEquals(existing.reminderPolicy, saved.task.reminderPolicy)
        assertEquals(
            TaskSchedule.Interval(
                rule = existingRule.copy(
                    every = Duration.ofMinutes(60),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(10, 0),
                        endsAt = LocalTime.of(16, 30),
                    ),
                ),
            ),
            saved.task.schedule,
        )
    }


    @Test
    fun existingRecurrenceTaskCanRoundTripRecurrenceDetails() {
        val existing = Task(
            id = TaskId("existing-recurrence"),
            title = "Old title",
            status = TaskStatus.COMPLETED,
            priority = TaskPriority.LOW,
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 6),
                    every = 2,
                    endsOn = LocalDate.of(2026, 12, 31),
                    daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                ),
            ),
            reminderPolicy = ReminderPolicy.normal(),
        )

        val draft = TaskEditorDraft.fromTask(existing)
        assertTrue(draft.recurrenceEnabled)
        assertEquals(RecurrenceFrequency.WEEKLY, draft.recurrenceFrequency)
        assertEquals("2", draft.recurrenceEvery)
        assertEquals("2026-09-06", draft.recurrenceStartDate)
        assertEquals("2026-12-31", draft.recurrenceEndDate)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), draft.recurrenceDaysOfWeek)

        val result = draft
            .onRecurrenceEveryChanged("3")
            .onRecurrenceDayToggled(DayOfWeek.MONDAY)
            .applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(existing.id, saved.task.id)
        assertEquals(existing.status, saved.task.status)
        assertEquals(existing.reminderPolicy, saved.task.reminderPolicy)
        assertEquals(
            TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 6),
                    every = 3,
                    endsOn = LocalDate.of(2026, 12, 31),
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                ),
            ),
            saved.task.schedule,
        )
    }
    @Test
    fun allDayScheduleStartsWithScheduleInputsDisabled() {
        val task = Task(
            id = TaskId("all-day"),
            title = "Plan today",
            schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 9, 6)),
        )

        val draft = TaskEditorDraft.fromTask(task)

        assertFalse(draft.deadlineEnabled)
        assertFalse(draft.timeBlockEnabled)
        assertFalse(draft.intervalEnabled)
        assertFalse(draft.recurrenceEnabled)
        assertEquals("", draft.deadlineDate)
        assertEquals("", draft.deadlineTime)
        assertEquals("", draft.timeBlockStartDate)
        assertEquals("", draft.timeBlockStartTime)
        assertEquals("", draft.timeBlockEndDate)
        assertEquals("", draft.timeBlockEndTime)
        assertEquals("", draft.intervalStartDate)
        assertEquals("", draft.intervalEveryMinutes)
        assertEquals("", draft.intervalWindowStartTime)
        assertEquals("", draft.intervalWindowEndTime)
        assertEquals("", draft.recurrenceStartDate)
        assertEquals("1", draft.recurrenceEvery)
        assertEquals("", draft.recurrenceEndDate)
        assertEquals(emptySet<DayOfWeek>(), draft.recurrenceDaysOfWeek)
    }
    @Test
    fun createTaskWithPersistentReminderBuildsReminderPolicy() {
        val result = TaskEditorDraft(
            title = "Drink water",
            reminderMode = ReminderMode.PERSISTENT,
            reminderRealertMinutes = "15",
            reminderMaximumRealerts = "3",
        ).createTask(createTaskId = { TaskId("persistent-reminder") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(ReminderPolicy.persistent(Duration.ofMinutes(15), 3), saved.task.reminderPolicy)
    }

    @Test
    fun createTaskWithStrictReminderBuildsReminderPolicy() {
        val result = TaskEditorDraft(
            title = "Submit report",
            reminderMode = ReminderMode.STRICT,
            reminderRealertMinutes = "10",
            reminderMaximumRealerts = "2",
            reminderSnoozeAllowed = false,
            reminderRescheduleAllowed = false,
        ).createTask(createTaskId = { TaskId("strict-reminder") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            ReminderPolicy.strict(
                realertInterval = Duration.ofMinutes(10),
                maximumRealerts = 2,
                snoozeAllowed = false,
                rescheduleAllowed = false,
            ),
            saved.task.reminderPolicy,
        )
    }

    @Test
    fun missingReminderRealertInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            reminderMode = ReminderMode.PERSISTENT,
            reminderRealertMinutes = "15",
            reminderMaximumRealerts = " ",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Enter re-alert interval and count", invalid.draft.reminderPolicyErrorMessage)
    }

    @Test
    fun malformedReminderRealertInputReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            reminderMode = ReminderMode.PERSISTENT,
            reminderRealertMinutes = "soon",
            reminderMaximumRealerts = "3",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Use whole numbers for reminder re-alert settings", invalid.draft.reminderPolicyErrorMessage)
    }

    @Test
    fun nonPositiveReminderRealertIntervalReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            reminderMode = ReminderMode.PERSISTENT,
            reminderRealertMinutes = "0",
            reminderMaximumRealerts = "3",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Reminder re-alert interval must be greater than 0", invalid.draft.reminderPolicyErrorMessage)
    }

    @Test
    fun nonPositiveReminderRealertCountReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Drink water",
            reminderMode = ReminderMode.PERSISTENT,
            reminderRealertMinutes = "15",
            reminderMaximumRealerts = "0",
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Reminder re-alert count must be greater than 0", invalid.draft.reminderPolicyErrorMessage)
    }

    @Test
    fun focusReminderWithoutTimeBlockReturnsValidationError() {
        val result = TaskEditorDraft(
            title = "Deep work",
            reminderMode = ReminderMode.FOCUS,
        ).createTask(createTaskId = { TaskId("unused") })

        assertTrue(result is TaskEditorSubmitResult.Invalid)
        val invalid = result as TaskEditorSubmitResult.Invalid
        assertEquals("Focus reminders require a time block", invalid.draft.reminderPolicyErrorMessage)
    }

    @Test
    fun focusReminderWithTimeBlockBuildsReminderPolicy() {
        val result = TaskEditorDraft(
            title = "Deep work",
            timeBlockEnabled = true,
            timeBlockStartDate = "2026-09-06",
            timeBlockStartTime = "09:00",
            timeBlockEndDate = "2026-09-06",
            timeBlockEndTime = "10:00",
            reminderMode = ReminderMode.FOCUS,
        ).createTask(createTaskId = { TaskId("focus-reminder") })

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(ReminderPolicy.focus(), saved.task.reminderPolicy)
    }

    @Test
    fun reminderPolicyChangeClearsReminderValidationError() {
        val changed = TaskEditorDraft(
            reminderMode = ReminderMode.PERSISTENT,
            reminderPolicyErrorMessage = "Enter re-alert interval and count",
        ).onReminderMaximumRealertsChanged("3")

        assertEquals("3", changed.reminderMaximumRealerts)
        assertNull(changed.reminderPolicyErrorMessage)
    }

    @Test
    fun existingTaskCanRoundTripReminderPolicyDetails() {
        val existing = Task(
            id = TaskId("existing-reminder"),
            title = "Old title",
            status = TaskStatus.ACTIVE,
            priority = TaskPriority.NORMAL,
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 9, 6, 9, 0),
                endsAt = LocalDateTime.of(2026, 9, 6, 10, 0),
            ),
            reminderPolicy = ReminderPolicy.strict(
                realertInterval = Duration.ofMinutes(10),
                maximumRealerts = 2,
                snoozeAllowed = false,
                rescheduleAllowed = false,
            ),
        )

        val draft = TaskEditorDraft.fromTask(existing)
        assertEquals(ReminderMode.STRICT, draft.reminderMode)
        assertEquals("10", draft.reminderRealertMinutes)
        assertEquals("2", draft.reminderMaximumRealerts)
        assertFalse(draft.reminderSnoozeAllowed)
        assertFalse(draft.reminderRescheduleAllowed)

        val result = draft
            .onReminderMaximumRealertsChanged("4")
            .applyTo(existing)

        assertTrue(result is TaskEditorSubmitResult.Saved)
        val saved = result as TaskEditorSubmitResult.Saved
        assertEquals(
            ReminderPolicy.strict(
                realertInterval = Duration.ofMinutes(10),
                maximumRealerts = 4,
                snoozeAllowed = false,
                rescheduleAllowed = false,
            ),
            saved.task.reminderPolicy,
        )
    }
}


