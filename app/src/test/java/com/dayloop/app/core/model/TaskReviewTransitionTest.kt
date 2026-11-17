package com.dayloop.app.core.model

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskReviewTransitionTest {
    private val date = LocalDate.of(2026, 9, 20)
    private val task = Task(TaskId("focus"), "Study",
        schedule = TaskSchedule.TimeBlock(date.minusDays(1).atTime(9, 0), date.minusDays(1).atTime(10, 0)),
        reminderPolicy = ReminderPolicy.focus(), projectId = ProjectId("project"),
        provenance = TaskProvenance("RECURRENCE_RULE", "series", 1))

    @Test fun somedayIsRecoverableAndValidForFocusTasks() {
        val saved = task.moveToSomeday()
        assertEquals(TaskStatus.SOMEDAY, saved.status)
        assertNull(saved.schedule)
        assertEquals(ReminderMode.NORMAL, saved.reminderPolicy.mode)
        assertNull(saved.completedAt)
        assertEquals(task.id, saved.id)
        assertEquals(task.projectId, saved.projectId)
        assertEquals(task.provenance, saved.provenance)
        assertEquals(TaskSchedule.AllDay(date), saved.keepForToday(date).schedule)
        assertEquals(TaskStatus.ACTIVE, saved.keepForToday(date).status)
    }

    @Test fun keepTodayNormalizesFocusAndRepeatedDecisionIsStable() {
        val kept = task.keepForToday(date)
        assertEquals(ReminderMode.NORMAL, kept.reminderPolicy.mode)
        assertEquals(kept, kept.keepForToday(date))
    }

    @Test fun dismissIsNotCompletionAndCanBeRestored() {
        val dismissed = task.markCompleted(Instant.EPOCH).skip()
        assertEquals(TaskStatus.SKIPPED, dismissed.status)
        assertNull(dismissed.completedAt)
        assertEquals(task.schedule, dismissed.schedule)
        assertEquals(TaskStatus.ACTIVE, dismissed.keepForToday(date).status)
    }
}
