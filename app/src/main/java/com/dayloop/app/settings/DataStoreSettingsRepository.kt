package com.dayloop.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dayloop.app.core.model.ReminderMode
import java.io.IOException
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
                alarmGradeEnabled = preferences[ALARM_GRADE_ENABLED] ?: false,
                widgetPrivacyEnabled = preferences[WIDGET_PRIVACY_ENABLED] ?: true,
                weekStart = preferences[WEEK_START].toEnumOrDefault(DayOfWeek.MONDAY),
                onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
                palette = preferences[PALETTE].toEnumOrDefault(AppPalette.MATCHA),
                textScale = preferences[TEXT_SCALE]?.takeIf { it.isFinite() }?.coerceIn(1f, 1.3f) ?: 1f,
                reducedMotion = preferences[REDUCED_MOTION] ?: false,
                fontWeightScale = preferences[FONT_WEIGHT_SCALE]?.takeIf { it.isFinite() }?.coerceIn(0.5f, 1.5f) ?: 1f,
                customColorHex = normalizeHexColor(preferences[CUSTOM_COLOR_HEX]),
                font = preferences[FONT].toEnumOrDefault(AppFont.JETBRAINS_MONO),
                aiAssistantEnabled = preferences[AI_ASSISTANT_ENABLED] ?: true,
                hapticHeartbeatEnabled = preferences[HAPTIC_HEARTBEAT_ENABLED] ?: true,
                planningIcons = preferences.asMap().entries.filter { it.key.name.startsWith("planning_icon:") }
                    .mapNotNull { (key, value) -> (value as? String)?.let { key.name.removePrefix("planning_icon:") to it } }.toMap(),
                notes = preferences[NOTES]?.let { json ->
                    try {
                        val array = JSONArray(json)
                        (0 until array.length()).map { i ->
                            val obj = array.getJSONObject(i)
                            Note(obj.getString("id"), obj.getString("content"), obj.getLong("createdAt"))
                        }
                    } catch (_: Exception) { emptyList() }
                } ?: emptyList(),
                savedPalettes = preferences[SAVED_PALETTES]?.let { json ->
                    try {
                        val array = JSONArray(json)
                        (0 until array.length()).map { i ->
                            val obj = array.getJSONObject(i)
                            SavedPalette(obj.getString("name"), obj.getString("colorHex"))
                        }
                    } catch (_: Exception) { emptyList() }
                } ?: emptyList(),
                gifts = preferences[GIFTS]?.let { json ->
                    try {
                        val array = JSONArray(json)
                        (0 until array.length()).map { i ->
                            val obj = array.getJSONObject(i)
                            MascotGift(obj.getString("id"), obj.getString("title"), obj.getString("message"), obj.getLong("timestamp"), obj.getBoolean("read"))
                        }
                    } catch (_: Exception) { emptyList() }
                } ?: emptyList(),
                usageStats = UsageStats(
                    totalFocusMinutes = preferences[TOTAL_FOCUS_MINUTES] ?: 0,
                    lastAppOpenTimestamp = preferences[LAST_APP_OPEN_TIMESTAMP] ?: 0,
                    backgroundServiceSeconds = preferences[BACKGROUND_SERVICE_SECONDS] ?: 0,
                ),
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setFont(font: AppFont) { dataStore.edit { it[FONT] = font.name } }

    override suspend fun setPlanningIcon(key: String, icon: String) {
        require(key.startsWith("project:") || key.startsWith("goal:"))
        dataStore.edit { it[stringPreferencesKey("planning_icon:$key")] = icon }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    override suspend fun setAppearance(
        palette: AppPalette,
        textScale: Float,
        reducedMotion: Boolean,
        fontWeightScale: Float,
        customColorHex: String?
    ) {
        require(textScale.isFinite() && textScale in 1f..1.3f)
        require(fontWeightScale.isFinite() && fontWeightScale in 0.5f..1.5f)
        val normalized = normalizeHexColor(customColorHex)
        require(customColorHex == null || normalized != null) { "Use a six-digit HEX colour" }
        dataStore.edit {
            it[PALETTE] = palette.name
            it[TEXT_SCALE] = textScale
            it[REDUCED_MOTION] = reducedMotion
            it[FONT_WEIGHT_SCALE] = fontWeightScale
            if (normalized != null) it[CUSTOM_COLOR_HEX] = normalized else it.remove(CUSTOM_COLOR_HEX)
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

    override suspend fun setAlarmGradeEnabled(enabled: Boolean) {
        dataStore.edit { it[ALARM_GRADE_ENABLED] = enabled }
    }

    override suspend fun setWidgetPrivacyEnabled(enabled: Boolean) {
        dataStore.edit { it[WIDGET_PRIVACY_ENABLED] = enabled }
    }

    override suspend fun setAiAssistantEnabled(enabled: Boolean) {
        dataStore.edit { it[AI_ASSISTANT_ENABLED] = enabled }
    }

    override suspend fun setHapticHeartbeatEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_HEARTBEAT_ENABLED] = enabled }
    }

    override suspend fun addNote(content: String) {
        dataStore.edit { preferences ->
            val currentNotes = preferences[NOTES]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        Note(obj.getString("id"), obj.getString("content"), obj.getLong("createdAt"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()
            
            val newNote = Note(UUID.randomUUID().toString(), content, System.currentTimeMillis())
            val updatedNotes = currentNotes + newNote
            
            val array = JSONArray()
            updatedNotes.forEach { note ->
                array.put(JSONObject().apply {
                    put("id", note.id)
                    put("content", note.content)
                    put("createdAt", note.createdAt)
                })
            }
            preferences[NOTES] = array.toString()
        }
    }

    override suspend fun deleteNote(id: String) {
        dataStore.edit { preferences ->
            val currentNotes = preferences[NOTES]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        Note(obj.getString("id"), obj.getString("content"), obj.getLong("createdAt"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()
            
            val updatedNotes = currentNotes.filterNot { it.id == id }
            
            val array = JSONArray()
            updatedNotes.forEach { note ->
                array.put(JSONObject().apply {
                    put("id", note.id)
                    put("content", note.content)
                    put("createdAt", note.createdAt)
                })
            }
            preferences[NOTES] = array.toString()
        }
    }

    override suspend fun savePalette(name: String, hex: String) {
        dataStore.edit { preferences ->
            val current = preferences[SAVED_PALETTES]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        SavedPalette(obj.getString("name"), obj.getString("colorHex"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()

            val updated = current.filterNot { it.name == name } + SavedPalette(name, hex)
            val array = JSONArray()
            updated.forEach { palette ->
                array.put(JSONObject().apply {
                    put("name", palette.name)
                    put("colorHex", palette.colorHex)
                })
            }
            preferences[SAVED_PALETTES] = array.toString()
        }
    }

    override suspend fun deletePalette(name: String) {
        dataStore.edit { preferences ->
            val current = preferences[SAVED_PALETTES]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        SavedPalette(obj.getString("name"), obj.getString("colorHex"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()

            val updated = current.filterNot { it.name == name }
            val array = JSONArray()
            updated.forEach { palette ->
                array.put(JSONObject().apply {
                    put("name", palette.name)
                    put("colorHex", palette.colorHex)
                })
            }
            preferences[SAVED_PALETTES] = array.toString()
        }
    }

    override suspend fun recordAppOpen(timestamp: Long) {
        dataStore.edit { it[LAST_APP_OPEN_TIMESTAMP] = timestamp }
    }

    override suspend fun addFocusMinutes(minutes: Long) {
        dataStore.edit { it[TOTAL_FOCUS_MINUTES] = (it[TOTAL_FOCUS_MINUTES] ?: 0) + minutes }
    }

    override suspend fun addBackgroundServiceSeconds(seconds: Long) {
        dataStore.edit { it[BACKGROUND_SERVICE_SECONDS] = (it[BACKGROUND_SERVICE_SECONDS] ?: 0) + seconds }
    }

    override suspend fun addGift(title: String, message: String) {
        dataStore.edit { preferences ->
            val current = preferences[GIFTS]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        MascotGift(obj.getString("id"), obj.getString("title"), obj.getString("message"), obj.getLong("timestamp"), obj.getBoolean("read"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()
            
            val updated = current + MascotGift(UUID.randomUUID().toString(), title, message, System.currentTimeMillis())
            val array = JSONArray()
            updated.forEach { gift ->
                array.put(JSONObject().apply {
                    put("id", gift.id); put("title", gift.title); put("message", gift.message); put("timestamp", gift.timestamp); put("read", gift.isRead)
                })
            }
            preferences[GIFTS] = array.toString()
        }
    }

    override suspend fun markGiftRead(id: String) {
        dataStore.edit { preferences ->
            val current = preferences[GIFTS]?.let { json ->
                try {
                    val array = JSONArray(json)
                    (0 until array.length()).map { i ->
                        val obj = array.getJSONObject(i)
                        MascotGift(obj.getString("id"), obj.getString("title"), obj.getString("message"), obj.getLong("timestamp"), obj.getBoolean("read"))
                    }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()
            
            val updated = current.map { if (it.id == id) it.copy(isRead = true) else it }
            val array = JSONArray()
            updated.forEach { gift ->
                array.put(JSONObject().apply {
                    put("id", gift.id); put("title", gift.title); put("message", gift.message); put("timestamp", gift.timestamp); put("read", gift.isRead)
                })
            }
            preferences[GIFTS] = array.toString()
        }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FONT = stringPreferencesKey("app_font")
        val PALETTE = stringPreferencesKey("app_palette")
        val TEXT_SCALE = floatPreferencesKey("app_text_scale")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val DEFAULT_REMINDER_MODE = stringPreferencesKey("default_reminder_mode")
        val LOCK_SCREEN_PRIVACY_ENABLED = booleanPreferencesKey("lock_screen_privacy_enabled")
        val ALARM_GRADE_ENABLED = booleanPreferencesKey("alarm_grade_enabled")
        val WIDGET_PRIVACY_ENABLED = booleanPreferencesKey("widget_privacy_enabled")
        val WEEK_START = stringPreferencesKey("week_start")
        val FONT_WEIGHT_SCALE = floatPreferencesKey("app_font_weight_scale")
        val CUSTOM_COLOR_HEX = stringPreferencesKey("custom_color_hex")
        val NOTES = stringPreferencesKey("app_notes")
        val SAVED_PALETTES = stringPreferencesKey("app_saved_palettes")
        val AI_ASSISTANT_ENABLED = booleanPreferencesKey("ai_assistant_enabled")
        val HAPTIC_HEARTBEAT_ENABLED = booleanPreferencesKey("haptic_heartbeat_enabled")
        val TOTAL_FOCUS_MINUTES = longPreferencesKey("total_focus_minutes")
        val LAST_APP_OPEN_TIMESTAMP = longPreferencesKey("last_app_open_timestamp")
        val BACKGROUND_SERVICE_SECONDS = longPreferencesKey("background_service_seconds")
        val GIFTS = stringPreferencesKey("app_mascot_gifts")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T = enumValues<T>()
    .firstOrNull { value -> value.name == this }
    ?: default
