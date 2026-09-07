package com.dayloop.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dayloop.app.core.model.ReminderMode
import java.io.File
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:org.junit.Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun settingsDefaultsMatchEarlySettingsContract() = runTest {
        val repository = createRepository()

        assertEquals(AppSettings(), repository.settings.first())
    }

    @Test
    fun themeModePersistsIndependentlyFromTaskDatabase() = runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)
    }

    @Test
    fun dynamicColorPreferencePersistsIndependentlyFromTaskDatabase() = runTest {
        val repository = createRepository()

        repository.setDynamicColorEnabled(false)

        assertEquals(false, repository.settings.first().dynamicColorEnabled)
    }

    @Test
    fun defaultReminderModePersistsIndependentlyFromTaskDatabase() = runTest {
        val repository = createRepository()

        repository.setDefaultReminderMode(ReminderMode.STRICT)

        assertEquals(ReminderMode.STRICT, repository.settings.first().defaultReminderMode)
    }

    @Test
    fun lockScreenPrivacyPersistsIndependentlyFromTaskDatabase() = runTest {
        val repository = createRepository()

        repository.setLockScreenPrivacyEnabled(false)

        assertEquals(false, repository.settings.first().lockScreenPrivacyEnabled)
    }

    @Test
    fun weekStartPersistsIndependentlyFromTaskDatabase() = runTest {
        val repository = createRepository()

        repository.setWeekStart(DayOfWeek.SUNDAY)

        assertEquals(DayOfWeek.SUNDAY, repository.settings.first().weekStart)
    }

    @Test
    fun invalidStoredEnumValuesFallBackToSafeDefaults() = runTest {
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = backgroundScope,
            produceFile = { newSettingsPath() },
        )
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "NEON"
            preferences[stringPreferencesKey("default_reminder_mode")] = "LOUD"
            preferences[stringPreferencesKey("week_start")] = "FUNDAY"
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertEquals(AppSettings(), repository.settings.first())
    }

    private fun TestScope.createRepository(): DataStoreSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = backgroundScope,
            produceFile = { newSettingsPath() },
        )
        return DataStoreSettingsRepository(dataStore)
    }

    private fun newSettingsPath(): okio.Path {
        val file = File(temporaryFolder.root, "settings-${System.nanoTime()}.preferences_pb")
        return file.absolutePath.toPath()
    }
}
