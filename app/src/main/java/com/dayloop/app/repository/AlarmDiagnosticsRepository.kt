package com.dayloop.app.repository

import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import kotlinx.coroutines.flow.Flow

interface AlarmDiagnosticsRepository {
    fun observeEntries(): Flow<List<AlarmDiagnosticEntry>>

    suspend fun getRecentEntries(limit: Int = 100): List<AlarmDiagnosticEntry>

    suspend fun append(entry: AlarmDiagnosticEntry)
}
