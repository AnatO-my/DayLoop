package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.TaskSchedule
import org.junit.Assert.*
import org.junit.Test

class TaskScheduleRepairTest {
    @Test fun conflictingScheduleFlagsAreRejected() {
        val result = TaskEditorDraft(title = "Read", deadlineEnabled = true, intervalEnabled = true).createTask()
        assertTrue(result is TaskEditorSubmitResult.Invalid)
        assertEquals("Choose one schedule type", (result as TaskEditorSubmitResult.Invalid).draft.errorMessage)
    }

    @Test fun deadlineNeedsTimeAndSavesWhenProvided() {
        val draft = TaskEditorDraft(title = "Read", deadlineEnabled = true, deadlineDate = "2026-09-17")
        assertTrue(draft.createTask() is TaskEditorSubmitResult.Invalid)
        val result = draft.onDeadlineTimeChanged("16:30").createTask() as TaskEditorSubmitResult.Saved
        assertTrue(result.task.schedule is TaskSchedule.Deadline)
    }

    @Test fun selectingBlockDisablesOtherScheduleFlags() {
        val draft = TaskEditorDraft(title = "Read", recurrenceEnabled = true).onTimeBlockEnabledChanged(true)
        assertTrue(draft.timeBlockEnabled)
        assertFalse(draft.recurrenceEnabled)
        assertFalse(draft.deadlineEnabled)
        assertFalse(draft.intervalEnabled)
    }
}
