package com.dayloop.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {
    @Test
    fun taskDefaultsToActiveNormalPriorityAndNormalReminderPolicy() {
        val task = Task(
            id = TaskId("task-1"),
            title = "Plan the day",
        )

        assertEquals(TaskStatus.ACTIVE, task.status)
        assertEquals(TaskPriority.NORMAL, task.priority)
        assertEquals(ReminderPolicy.normal(), task.reminderPolicy)
    }

    @Test
    fun blankTaskIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskId(" ")
        }
    }

    @Test
    fun blankTitleIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Task(
                id = TaskId("task-1"),
                title = " ",
            )
        }
    }

    @Test
    fun generatedTaskIdsAreNonBlankAndDistinct() {
        val first = TaskId.generate()
        val second = TaskId.generate()

        assertTrue(first.value.isNotBlank())
        assertTrue(second.value.isNotBlank())
        assertNotEquals(first, second)
    }

    @Test
    fun completionReturnsCompletedCopy() {
        val task = Task(
            id = TaskId("task-1"),
            title = "Plan the day",
        )

        val completed = task.markCompleted()

        assertEquals(TaskStatus.ACTIVE, task.status)
        assertEquals(TaskStatus.COMPLETED, completed.status)
        assertEquals(task.id, completed.id)
        assertEquals(task.title, completed.title)
        assertEquals(task.priority, completed.priority)
        assertEquals(task.reminderPolicy, completed.reminderPolicy)
    }

    @Test
    fun completionIsIdempotent() {
        val task = Task(
            id = TaskId("task-1"),
            title = "Plan the day",
            status = TaskStatus.COMPLETED,
        )

        assertEquals(task, task.markCompleted())
    }

    @Test
    fun priorityOrderingKeepsUrgencySeparateFromStatus() {
        assertTrue(TaskPriority.HIGH.isHigherThan(TaskPriority.NORMAL))
        assertTrue(TaskPriority.NORMAL.isHigherThan(TaskPriority.LOW))
    }
}
