package com.dayloop.app.feature.diagnostics

import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import com.dayloop.app.core.notifications.ReminderCapabilities

data class DiagnosticsUiState(
    val capabilities: ReminderCapabilities? = null,
    val entries: List<AlarmDiagnosticEntry> = emptyList(),
    val isRepairing: Boolean = false,
) {
    val entriesCount: Int = entries.size
    val hasEntries: Boolean = entries.isNotEmpty()
}
