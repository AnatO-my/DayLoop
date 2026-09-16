package com.dayloop.app.feature.taskeditor

import com.dayloop.app.core.model.*
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskEditorConnectionTest {
    @Test fun removingDeadlinePreservesProjectAndRemovesSchedule() {
        val task = Task(TaskId("task"), "Read", schedule = TaskSchedule.Deadline(LocalDateTime.of(2026, 9, 8, 12, 0)),
            projectId = ProjectId("project"))
        val result = TaskEditorDraft.fromTask(task).onDeadlineEnabledChanged(false).applyTo(task) as TaskEditorSubmitResult.Saved
        assertNull(result.task.schedule)
        assertEquals(task.projectId, result.task.projectId)
    }
}
