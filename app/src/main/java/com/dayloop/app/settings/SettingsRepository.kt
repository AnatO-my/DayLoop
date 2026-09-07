package com.dayloop.app.settings

import com.dayloop.app.core.model.ReminderMode
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setDefaultReminderMode(reminderMode: ReminderMode)

    suspend fun setLockScreenPrivacyEnabled(enabled: Boolean)

    suspend fun setWeekStart(dayOfWeek: DayOfWeek)
}
