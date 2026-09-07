package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentReminderEscalationTest {
    @Test
    fun startCreatesActiveChainFromPersistentReminderPolicy() {
        val escalation = PersistentReminderEscalation.start(
            policy = ReminderPolicy.persistent(
                realertInterval = Duration.ofMinutes(15),
                maximumRealerts = 2,
            ),
            taskId = TaskId("task-1"),
            sourceOccurrenceId = ScheduledOccurrenceId("occurrence-1"),
            deliveredAt = STARTED_AT,
            id = PersistentReminderEscalationId("escalation-1"),
        )

        assertTrue(escalation.isActive)
        assertEquals(STARTED_AT.plus(Duration.ofMinutes(15)), escalation.nextRealertAt)
        assertEquals(0, escalation.realertsDelivered)
        assertEquals(2, escalation.maximumRealerts)
    }

    @Test
    fun deliveredRealertAdvancesToNextRealertUntilCap() {
        val escalation = baseEscalation()

        val advanced = escalation.recordRealertDelivered(STARTED_AT.plus(Duration.ofMinutes(15)))

        assertTrue(advanced.isActive)
        assertEquals(1, advanced.realertsDelivered)
        assertEquals(STARTED_AT.plus(Duration.ofMinutes(30)), advanced.nextRealertAt)
    }

    @Test
    fun finalDeliveredRealertCapsChainAndClearsNextTime() {
        val escalation = baseEscalation(
            maximumRealerts = 2,
            realertsDelivered = 1,
            nextRealertAt = STARTED_AT.plus(Duration.ofMinutes(30)),
        )

        val capped = escalation.recordRealertDelivered(STARTED_AT.plus(Duration.ofMinutes(30)))

        assertFalse(capped.isActive)
        assertEquals(PersistentReminderEscalationStatus.CAPPED, capped.status)
        assertEquals(2, capped.realertsDelivered)
        assertNull(capped.nextRealertAt)
    }

    @Test
    fun resolveStopsChainImmediatelyAndIsIdempotent() {
        val escalation = baseEscalation()

        val resolved = escalation.resolve(STARTED_AT.plus(Duration.ofMinutes(5)))

        assertEquals(PersistentReminderEscalationStatus.RESOLVED, resolved.status)
        assertNull(resolved.nextRealertAt)
        assertEquals(resolved, resolved.resolve(STARTED_AT.plus(Duration.ofMinutes(10))))
    }

    @Test
    fun inactiveChainIgnoresDuplicateDeliveryAdvance() {
        val resolved = baseEscalation().resolve(STARTED_AT.plus(Duration.ofMinutes(5)))

        assertEquals(resolved, resolved.recordRealertDelivered(STARTED_AT.plus(Duration.ofMinutes(15))))
    }

    @Test
    fun normalPolicyCannotStartPersistentEscalation() {
        assertThrows(IllegalArgumentException::class.java) {
            PersistentReminderEscalation.start(
                policy = ReminderPolicy.normal(),
                taskId = TaskId("task-1"),
                sourceOccurrenceId = ScheduledOccurrenceId("occurrence-1"),
                deliveredAt = STARTED_AT,
                id = PersistentReminderEscalationId("escalation-1"),
            )
        }
    }

    @Test
    fun activeEscalationRejectsExhaustedRealerts() {
        assertThrows(IllegalArgumentException::class.java) {
            baseEscalation(maximumRealerts = 1, realertsDelivered = 1)
        }
    }

    private fun baseEscalation(
        maximumRealerts: Int = 2,
        realertsDelivered: Int = 0,
        nextRealertAt: Instant? = STARTED_AT.plus(Duration.ofMinutes(15)),
    ): PersistentReminderEscalation = PersistentReminderEscalation(
        id = PersistentReminderEscalationId("escalation-1"),
        taskId = TaskId("task-1"),
        sourceOccurrenceId = ScheduledOccurrenceId("occurrence-1"),
        nextRealertAt = nextRealertAt,
        realertInterval = Duration.ofMinutes(15),
        maximumRealerts = maximumRealerts,
        realertsDelivered = realertsDelivered,
        createdAt = STARTED_AT,
    )

    private companion object {
        val STARTED_AT: Instant = Instant.parse("2026-09-06T09:00:00Z")
    }
}
