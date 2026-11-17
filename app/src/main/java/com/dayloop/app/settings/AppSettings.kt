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
    val font: AppFont = AppFont.FUNNEL_DISPLAY,
    val readableBodyText: Boolean = true,
    val homePresentation: HomePresentation = HomePresentation.EDITORIAL,
    val planningIcons: Map<String, String> = emptyMap(),
    val notes: List<Note> = emptyList(),
    val savedPalettes: List<SavedPalette> = emptyList(),
    val aiAssistantEnabled: Boolean = true,
    val hapticHeartbeatEnabled: Boolean = true,
    val gifts: List<MascotGift> = emptyList(),
    val usageStats: UsageStats = UsageStats(),
    val displayName: String = "",
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
    FUNNEL_DISPLAY("Funnel Display"),
    JETBRAINS_MONO("JetBrains Mono"), GEIST_MONO("Geist Mono"), STACK_SANS_TEXT("Stack Sans Text"), SYSTEM_MONO("System monospace"),
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

/** Visual treatment for Today. Task, reminder, and navigation behavior is shared by every option. */
enum class HomePresentation(val label: String) {
    EDITORIAL("Editorial cadence"),
    SIGNAL_GRID("Signal grid"),
    WEEK_LEDGER("Week ledger"),
}
