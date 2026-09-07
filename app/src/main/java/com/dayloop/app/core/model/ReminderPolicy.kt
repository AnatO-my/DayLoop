package com.dayloop.app.core.model

import java.time.Duration

data class ReminderPolicy(
    val mode: ReminderMode,
    val realertInterval: Duration? = null,
    val maximumRealerts: Int = 0,
    val snoozeAllowed: Boolean = true,
    val rescheduleAllowed: Boolean = true,
) {
    val usesRealerts: Boolean
        get() = maximumRealerts > 0

    init {
        require(maximumRealerts >= 0) { "Maximum re-alerts must not be negative." }
        require(realertInterval == null || realertInterval.isPositive()) {
            "Re-alert interval must be positive."
        }
        require((realertInterval == null) == (maximumRealerts == 0)) {
            "Re-alert interval and maximum re-alerts must be configured together."
        }
        require(mode.supportsRealerts || !usesRealerts) {
            "Only Persistent and Strict reminder policies can configure re-alerts."
        }
        require(!mode.supportsRealerts || usesRealerts) {
            "Persistent and Strict reminder policies require re-alerts."
        }
    }

    companion object {
        fun normal(): ReminderPolicy = ReminderPolicy(mode = ReminderMode.NORMAL)

        fun persistent(
            realertInterval: Duration,
            maximumRealerts: Int,
        ): ReminderPolicy = ReminderPolicy(
            mode = ReminderMode.PERSISTENT,
            realertInterval = realertInterval,
            maximumRealerts = maximumRealerts,
        )

        fun strict(
            realertInterval: Duration,
            maximumRealerts: Int,
            snoozeAllowed: Boolean = false,
            rescheduleAllowed: Boolean = false,
        ): ReminderPolicy = ReminderPolicy(
            mode = ReminderMode.STRICT,
            realertInterval = realertInterval,
            maximumRealerts = maximumRealerts,
            snoozeAllowed = snoozeAllowed,
            rescheduleAllowed = rescheduleAllowed,
        )

        fun focus(): ReminderPolicy = ReminderPolicy(mode = ReminderMode.FOCUS)
    }
}

enum class ReminderMode {
    NORMAL,
    PERSISTENT,
    STRICT,
    FOCUS;

    val supportsRealerts: Boolean
        get() = this == PERSISTENT || this == STRICT
}

private fun Duration.isPositive(): Boolean = !isZero && !isNegative
