package com.dayloop.app.core.model

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TaskCompletionTimeTest {
    private val task = Task(TaskId("task"), "Read")
    private val at = Instant.parse("2026-09-20T10:00:00Z")

    @Test fun duplicateCompletionPreservesOriginalTime() {
        val completed = task.markCompleted(at)
        assertEquals(at, completed.completedAt)
        assertEquals(completed, completed.markCompleted(at.plusSeconds(86400)))
    }

    @Test fun reopenClearsTimeAndRecompletionRecordsNewTime() {
        val reopened = task.markCompleted(at).reopen()
        assertEquals(TaskStatus.ACTIVE, reopened.status)
        assertNull(reopened.completedAt)
        assertEquals(at.plusSeconds(60), reopened.markCompleted(at.plusSeconds(60)).completedAt)
    }

    @Test fun legacyCompletionDoesNotAcquireInventedTimestamp() {
        val legacy = task.copy(status = TaskStatus.COMPLETED)
        assertEquals(legacy, legacy.markCompleted(at))
    }
}
