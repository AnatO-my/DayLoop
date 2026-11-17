package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddDraftTest {
    private val fixedDate = LocalDate.of(2026, 9, 22)
    private val fixedTime = LocalTime.of(10, 0)

    @Test
    fun blankTitleReturnsValidationError() {
        val result = QuickAddDraft(title = "   ").submit(createTaskId = { TaskId("unused") })

        assertTrue(result is QuickAddSubmitResult.Invalid)
        val invalid = result as QuickAddSubmitResult.Invalid
        assertEquals("Enter a task title", invalid.draft.errorMessage)
        assertFalse(invalid.draft.canSubmit)
    }

    @Test
    fun titleChangeClearsValidationError() {
        val draft = QuickAddDraft(
            title = "",
            errorMessage = "Enter a task title",
        )

        val changed = draft.onTitleChanged("Plan today")

        assertEquals("Plan today", changed.title)
        assertNull(changed.errorMessage)
        assertTrue(changed.canSubmit)
    }

    @Test
    fun validAllDayModeCreatesAllDayTask() {
        val result = QuickAddDraft(title = "Plan today", scheduleMode = QuickAddScheduleMode.ALL_DAY).submit(
            createTaskId = { TaskId("quick-1") },
            todayDate = fixedDate,
            nowTime = fixedTime,
        )

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        assertEquals(TaskId("quick-1"), submitted.task.id)
        assertEquals("Plan today", submitted.task.title)
        assertEquals(TaskSchedule.AllDay(fixedDate), submitted.task.schedule)
        assertEquals(ReminderPolicy.normal(), submitted.task.reminderPolicy)
        assertFalse(submitted.startFocus)
    }

    @Test
    fun validDeadlineModeCreatesDeadlineTask() {
        val result = QuickAddDraft(title = "Submit report", scheduleMode = QuickAddScheduleMode.DEADLINE).submit(
            createTaskId = { TaskId("quick-2") },
            todayDate = fixedDate,
            nowTime = fixedTime,
        )

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        val schedule = submitted.task.schedule as TaskSchedule.Deadline
        assertEquals(fixedDate.atTime(11, 0), schedule.dueAt)
        assertFalse(submitted.startFocus)
    }

    @Test
    fun validTimeBlockModeCreatesTimeBlockTask() {
        val result = QuickAddDraft(title = "Deep work", scheduleMode = QuickAddScheduleMode.TIME_BLOCK).submit(
            createTaskId = { TaskId("quick-3") },
            todayDate = fixedDate,
            nowTime = fixedTime,
        )

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        val schedule = submitted.task.schedule as TaskSchedule.TimeBlock
        assertEquals(fixedDate.atTime(10, 0), schedule.startsAt)
        assertEquals(fixedDate.atTime(11, 0), schedule.endsAt)
        assertEquals(ReminderPolicy.focus(), submitted.task.reminderPolicy)
        assertFalse(submitted.startFocus)
    }

    @Test
    fun validIntervalModeCreatesIntervalTask() {
        val result = QuickAddDraft(title = "Drink water", scheduleMode = QuickAddScheduleMode.INTERVAL).submit(
            createTaskId = { TaskId("quick-4") },
            todayDate = fixedDate,
            nowTime = fixedTime,
        )

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        val schedule = submitted.task.schedule as TaskSchedule.Interval
        assertEquals(45L, schedule.rule.every.toMinutes())
        assertEquals(LocalTime.of(10, 0), schedule.rule.activeWindow.startsAt)
        assertFalse(submitted.startFocus)
    }

    @Test
    fun validFocusModeCreatesFocusTaskAndRequestsStartFocus() {
        val result = QuickAddDraft(title = "Pomodoro task", scheduleMode = QuickAddScheduleMode.FOCUS).submit(
            createTaskId = { TaskId("quick-5") },
            todayDate = fixedDate,
            nowTime = fixedTime,
        )

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        val schedule = submitted.task.schedule as TaskSchedule.TimeBlock
        assertEquals(fixedDate.atTime(10, 0), schedule.startsAt)
        assertEquals(fixedDate.atTime(10, 25), schedule.endsAt)
        assertTrue(submitted.startFocus)
    }
}
