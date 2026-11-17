package com.dayloop.app.settings

import com.dayloop.app.core.model.ReminderMode
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    fun observeGuideProgress(key: GuideKey): Flow<GuideProgress> = error("Guide persistence unavailable")

    suspend fun updateGuideProgress(key: GuideKey, expected: GuideProgress, action: GuideAction): Boolean {
        error("Guide persistence unavailable")
    }

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setFont(font: AppFont) { error("Font persistence unavailable") }
    suspend fun setReadableBodyText(enabled: Boolean) { error("Typography persistence unavailable") }
    suspend fun setHomePresentation(presentation: HomePresentation) { error("Home presentation persistence unavailable") }
    suspend fun setDisplayName(name: String) { error("Display name persistence unavailable") }
    suspend fun setPlanningIcon(key: String, icon: String) { error("Icon persistence unavailable") }

    suspend fun completeOnboarding() { error("Onboarding persistence is unavailable") }

    suspend fun setAppearance(
        palette: AppPalette,
        textScale: Float,
        reducedMotion: Boolean,
        fontWeightScale: Float,
        customColorHex: String?
    ) {
        error("Appearance persistence is unavailable")
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setDefaultReminderMode(reminderMode: ReminderMode)

    suspend fun setLockScreenPrivacyEnabled(enabled: Boolean)

    suspend fun setAlarmGradeEnabled(enabled: Boolean) {}

    suspend fun setWidgetPrivacyEnabled(enabled: Boolean) {}

    suspend fun setWeekStart(dayOfWeek: DayOfWeek)

    suspend fun setAiAssistantEnabled(enabled: Boolean)
    suspend fun setHapticHeartbeatEnabled(enabled: Boolean)

    suspend fun recordAppOpen(timestamp: Long) {}
    suspend fun addFocusMinutes(minutes: Long) {}
    suspend fun addBackgroundServiceSeconds(seconds: Long) {}

    suspend fun addGift(title: String, message: String) {}
    suspend fun markGiftRead(id: String) {}

    suspend fun addNote(content: String) { error("Note persistence is unavailable") }
    suspend fun deleteNote(id: String) { error("Note persistence is unavailable") }

    suspend fun savePalette(name: String, hex: String) { error("Palette persistence is unavailable") }
    suspend fun deletePalette(name: String) { error("Palette persistence is unavailable") }
}
