package com.dayloop.app.core.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCapabilityPolicyTest {
    @Test fun notificationPermissionIsRequiredFromAndroid13() {
        assertTrue(ReminderCapabilityPolicy.notificationsAllowed(32, runtimeGranted = false, systemEnabled = true))
        assertFalse(ReminderCapabilityPolicy.notificationsAllowed(33, runtimeGranted = false, systemEnabled = true))
        assertTrue(ReminderCapabilityPolicy.notificationsAllowed(33, runtimeGranted = true, systemEnabled = true))
        assertFalse(ReminderCapabilityPolicy.notificationsAllowed(33, runtimeGranted = true, systemEnabled = false))
    }

    @Test fun exactAlarmCapabilityIsRequiredFromAndroid12() {
        assertTrue(ReminderCapabilityPolicy.exactAlarmsAllowed(30, canScheduleExactAlarms = false))
        assertFalse(ReminderCapabilityPolicy.exactAlarmsAllowed(31, canScheduleExactAlarms = false))
        assertTrue(ReminderCapabilityPolicy.exactAlarmsAllowed(31, canScheduleExactAlarms = true))
    }
}
