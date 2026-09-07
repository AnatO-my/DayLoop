package com.dayloop.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dayloop.app.core.model.ReminderMode
import java.io.IOException
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE].toEnumOrDefault(ThemeMode.SYSTEM),
                dynamicColorEnabled = preferences[DYNAMIC_COLOR_ENABLED] ?: true,
                defaultReminderMode = preferences[DEFAULT_REMINDER_MODE].toEnumOrDefault(ReminderMode.NORMAL),
                lockScreenPrivacyEnabled = preferences[LOCK_SCREEN_PRIVACY_ENABLED] ?: true,
                weekStart = preferences[WEEK_START].toEnumOrDefault(DayOfWeek.MONDAY),
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    override suspend fun setDefaultReminderMode(reminderMode: ReminderMode) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_REMINDER_MODE] = reminderMode.name
        }
    }

    override suspend fun setLockScreenPrivacyEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOCK_SCREEN_PRIVACY_ENABLED] = enabled
        }
    }

    override suspend fun setWeekStart(dayOfWeek: DayOfWeek) {
        dataStore.edit { preferences ->
            preferences[WEEK_START] = dayOfWeek.name
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val DEFAULT_REMINDER_MODE = stringPreferencesKey("default_reminder_mode")
        val LOCK_SCREEN_PRIVACY_ENABLED = booleanPreferencesKey("lock_screen_privacy_enabled")
        val WEEK_START = stringPreferencesKey("week_start")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T = enumValues<T>()
    .firstOrNull { value -> value.name == this }
    ?: default
