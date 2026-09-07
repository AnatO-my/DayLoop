package com.dayloop.app.repository

import com.dayloop.app.core.alarms.PersistentReminderEscalation
import com.dayloop.app.core.alarms.PersistentReminderEscalationId
import com.dayloop.app.core.database.PersistentReminderEscalationDao
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPersistentReminderEscalationRepository(
    private val escalationDao: PersistentReminderEscalationDao,
) : PersistentReminderEscalationRepository {
    override fun observeEscalations(): Flow<List<PersistentReminderEscalation>> = escalationDao.observeAll()
        .map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun getEscalation(id: PersistentReminderEscalationId): PersistentReminderEscalation? =
        escalationDao.getById(id.value)?.toDomain()

    override suspend fun getEscalationsForTask(taskId: TaskId): List<PersistentReminderEscalation> =
        escalationDao.getForTask(taskId.value).map { row -> row.toDomain() }

    override suspend fun getActiveEscalations(): List<PersistentReminderEscalation> =
        escalationDao.getActive().map { row -> row.toDomain() }

    override suspend fun saveEscalation(escalation: PersistentReminderEscalation) {
        escalationDao.upsert(escalation.toEntity())
    }

    override suspend fun deleteEscalation(id: PersistentReminderEscalationId): Boolean =
        escalationDao.deleteById(id.value) > 0

    override suspend fun deleteEscalationsForTask(taskId: TaskId): Int =
        escalationDao.deleteForTask(taskId.value)
}
