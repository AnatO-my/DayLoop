package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddDraftTest {
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
    fun validTitleCreatesPlainActiveTaskAndClearsDraft() {
        val result = QuickAddDraft(title = "  Plan today  ").submit(createTaskId = { TaskId("quick-1") })

        assertTrue(result is QuickAddSubmitResult.Submitted)
        val submitted = result as QuickAddSubmitResult.Submitted
        assertEquals(TaskId("quick-1"), submitted.task.id)
        assertEquals("Plan today", submitted.task.title)
        assertEquals(TaskStatus.ACTIVE, submitted.task.status)
        assertEquals(TaskPriority.NORMAL, submitted.task.priority)
        assertNull(submitted.task.schedule)
        assertEquals(ReminderPolicy.normal(), submitted.task.reminderPolicy)
        assertEquals(QuickAddDraft(), submitted.nextDraft)
    }
}
