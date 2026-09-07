package com.dayloop.app.core.model

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    @Test
    fun normalPolicyHasNoRealertsAndAllowsUserAdjustment() {
        val policy = ReminderPolicy.normal()

        assertEquals(ReminderMode.NORMAL, policy.mode)
        assertFalse(policy.usesRealerts)
        assertEquals(null, policy.realertInterval)
        assertEquals(0, policy.maximumRealerts)
        assertTrue(policy.snoozeAllowed)
        assertTrue(policy.rescheduleAllowed)
    }

    @Test
    fun persistentPolicyRequiresPositiveRealertConfiguration() {
        val policy = ReminderPolicy.persistent(
            realertInterval = Duration.ofMinutes(15),
            maximumRealerts = 3,
        )

        assertEquals(ReminderMode.PERSISTENT, policy.mode)
        assertTrue(policy.usesRealerts)
        assertEquals(Duration.ofMinutes(15), policy.realertInterval)
        assertEquals(3, policy.maximumRealerts)
    }

    @Test
    fun strictPolicyCanConstrainSnoozeAndRescheduleSeparatelyFromPriority() {
        val policy = ReminderPolicy.strict(
            realertInterval = Duration.ofMinutes(10),
            maximumRealerts = 2,
            snoozeAllowed = false,
            rescheduleAllowed = false,
        )
        val task = Task(
            id = TaskId("task-1"),
            title = "Submit form",
            priority = TaskPriority.LOW,
            reminderPolicy = policy,
        )

        assertEquals(TaskPriority.LOW, task.priority)
        assertEquals(ReminderMode.STRICT, task.reminderPolicy.mode)
        assertFalse(task.reminderPolicy.snoozeAllowed)
        assertFalse(task.reminderPolicy.rescheduleAllowed)
    }

    @Test
    fun focusPolicyRequiresTimeBlockScheduleOnTask() {
        val task = Task(
            id = TaskId("task-1"),
            title = "Deep work",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 29, 9, 0),
                endsAt = LocalDateTime.of(2026, 8, 29, 10, 0),
            ),
            reminderPolicy = ReminderPolicy.focus(),
        )

        assertEquals(ReminderMode.FOCUS, task.reminderPolicy.mode)
    }

    @Test
    fun focusPolicyRejectsTaskWithoutTimeBlockSchedule() {
        assertThrows(IllegalArgumentException::class.java) {
            Task(
                id = TaskId("task-1"),
                title = "Deep work",
                reminderPolicy = ReminderPolicy.focus(),
            )
        }
    }

    @Test
    fun realertIntervalAndMaximumRealertsMustBeConfiguredTogether() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(
                mode = ReminderMode.PERSISTENT,
                realertInterval = Duration.ofMinutes(15),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(
                mode = ReminderMode.PERSISTENT,
                maximumRealerts = 3,
            )
        }
    }

    @Test
    fun realertIntervalMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy.persistent(
                realertInterval = Duration.ZERO,
                maximumRealerts = 3,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy.persistent(
                realertInterval = Duration.ofMinutes(-1),
                maximumRealerts = 3,
            )
        }
    }

    @Test
    fun normalAndFocusPoliciesCannotConfigureRealerts() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(
                mode = ReminderMode.NORMAL,
                realertInterval = Duration.ofMinutes(15),
                maximumRealerts = 1,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(
                mode = ReminderMode.FOCUS,
                realertInterval = Duration.ofMinutes(15),
                maximumRealerts = 1,
            )
        }
    }

    @Test
    fun persistentAndStrictPoliciesRequireRealerts() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(mode = ReminderMode.PERSISTENT)
        }

        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(mode = ReminderMode.STRICT)
        }
    }

    @Test
    fun maximumRealertsMustNotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderPolicy(
                mode = ReminderMode.PERSISTENT,
                realertInterval = Duration.ofMinutes(15),
                maximumRealerts = -1,
            )
        }
    }
}
