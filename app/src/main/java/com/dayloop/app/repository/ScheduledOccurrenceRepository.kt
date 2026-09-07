package com.dayloop.app.repository

import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow

interface ScheduledOccurrenceRepository {
    fun observeOccurrences(): Flow<List<ScheduledOccurrence>>

    suspend fun getOccurrence(id: ScheduledOccurrenceId): ScheduledOccurrence?

    suspend fun getOccurrencesForTask(taskId: TaskId): List<ScheduledOccurrence>

    suspend fun getScheduledOccurrences(): List<ScheduledOccurrence>

    suspend fun saveOccurrence(occurrence: ScheduledOccurrence)

    suspend fun deleteOccurrence(id: ScheduledOccurrenceId): Boolean

    suspend fun deleteOccurrencesForTask(taskId: TaskId): Int
}