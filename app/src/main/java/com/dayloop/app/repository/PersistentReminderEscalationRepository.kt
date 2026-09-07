package com.dayloop.app.repository

import com.dayloop.app.core.alarms.PersistentReminderEscalation
import com.dayloop.app.core.alarms.PersistentReminderEscalationId
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow

interface PersistentReminderEscalationRepository {
    fun observeEscalations(): Flow<List<PersistentReminderEscalation>>

    suspend fun getEscalation(id: PersistentReminderEscalationId): PersistentReminderEscalation?

    suspend fun getEscalationsForTask(taskId: TaskId): List<PersistentReminderEscalation>

    suspend fun getActiveEscalations(): List<PersistentReminderEscalation>

    suspend fun saveEscalation(escalation: PersistentReminderEscalation)

    suspend fun deleteEscalation(id: PersistentReminderEscalationId): Boolean

    suspend fun deleteEscalationsForTask(taskId: TaskId): Int
}
