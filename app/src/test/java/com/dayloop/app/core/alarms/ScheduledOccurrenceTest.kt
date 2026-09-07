package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledOccurrenceTest {
    @Test
    fun generatedOccurrenceIdIsNonBlank() {
        assertTrue(ScheduledOccurrenceId.generate().value.isNotBlank())
    }

    @Test
    fun blankOccurrenceIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ScheduledOccurrenceId(" ")
        }
    }

    @Test
    fun nonPositiveGenerationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            baseOccurrence(generation = 0)
        }
    }

    @Test
    fun updatedAtBeforeCreatedAtIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            baseOccurrence(
                createdAt = Instant.parse("2026-09-06T09:00:00Z"),
                updatedAt = Instant.parse("2026-09-06T08:59:59Z"),
            )
        }
    }

    @Test
    fun statusTransitionsUpdateTimestampWithoutChangingIdentity() {
        val occurrence = baseOccurrence()
        val firedAt = Instant.parse("2026-09-06T09:05:00Z")
        val cancelledAt = Instant.parse("2026-09-06T09:10:00Z")

        val fired = occurrence.markFired(firedAt)
        val cancelled = occurrence.markCancelled(cancelledAt)

        assertEquals(occurrence.id, fired.id)
        assertEquals(occurrence.taskId, fired.taskId)
        assertEquals(ScheduledOccurrenceStatus.FIRED, fired.status)
        assertEquals(firedAt, fired.updatedAt)
        assertEquals(ScheduledOccurrenceStatus.CANCELLED, cancelled.status)
        assertEquals(cancelledAt, cancelled.updatedAt)
    }

    private fun baseOccurrence(
        generation: Long = 1L,
        createdAt: Instant = Instant.parse("2026-09-06T08:00:00Z"),
        updatedAt: Instant = createdAt,
    ): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId("occurrence-1"),
        taskId = TaskId("task-1"),
        fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        generation = generation,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}