package com.dayloop.app.settings

import com.dayloop.app.core.model.ReminderMode
import java.time.DayOfWeek

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val defaultReminderMode: ReminderMode = ReminderMode.NORMAL,
    val lockScreenPrivacyEnabled: Boolean = true,
    val alarmGradeEnabled: Boolean = false,
    val widgetPrivacyEnabled: Boolean = true,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val onboardingCompleted: Boolean = false,
    val palette: AppPalette = AppPalette.MATCHA,
    val textScale: Float = 1f,
    val reducedMotion: Boolean = false,
    val fontWeightScale: Float = 1f,
    val customColorHex: String? = null,
    val font: AppFont = AppFont.JETBRAINS_MONO,
    val planningIcons: Map<String, String> = emptyMap(),
    val notes: List<Note> = emptyList(),
    val savedPalettes: List<SavedPalette> = emptyList(),
    val aiAssistantEnabled: Boolean = true,
    val hapticHeartbeatEnabled: Boolean = true,
    val gifts: List<MascotGift> = emptyList(),
    val usageStats: UsageStats = UsageStats(),
)

data class MascotGift(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

data class UsageStats(
    val totalFocusMinutes: Long = 0,
    val lastAppOpenTimestamp: Long = 0,
    val backgroundServiceSeconds: Long = 0,
)

data class SavedPalette(
    val name: String,
    val colorHex: String
)

data class Note(
    val id: String,
    val content: String,
    val createdAt: Long // timestamp
)

enum class AppFont(val label: String) {
    JETBRAINS_MONO("JetBrains Mono"), SYSTEM_MONO("System monospace"),
    ROBOTO("Roboto"), OPEN_SANS("Open Sans"), LATO("Lato"), MONTSERRAT("Montserrat"), POPPINS("Poppins"),
}

enum class AppPalette(val label: String) {
    MATCHA("Matcha"), EMERALD("Emerald"), ROSE("Rose"),
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
