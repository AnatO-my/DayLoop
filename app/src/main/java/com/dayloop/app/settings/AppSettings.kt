package com.dayloop.app.settings

import com.dayloop.app.core.model.ReminderMode
import java.time.DayOfWeek

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val defaultReminderMode: ReminderMode = ReminderMode.NORMAL,
    val lockScreenPrivacyEnabled: Boolean = true,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
