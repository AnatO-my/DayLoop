package com.dayloop.app.repository

import androidx.room.withTransaction
import com.dayloop.app.core.alarms.PersistentReminderEscalation
import com.dayloop.app.core.alarms.PersistentReminderEscalationId
import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceStatus
import com.dayloop.app.core.database.DayLoopDatabase
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPersistentReminderEscalationRepository(
    private val database: DayLoopDatabase,
) : PersistentReminderEscalationRepository {
    private val escalationDao = database.persistentReminderEscalationDao()

    override suspend fun advanceAfterDelivery(
        delivered: ScheduledOccurrence,
        previous: PersistentReminderEscalation?,
        next: PersistentReminderEscalation,
        nextOccurrence: ScheduledOccurrence?,
    ): Boolean = database.withTransaction {
        if (delivered.status != ScheduledOccurrenceStatus.FIRED ||
            database.scheduledOccurrenceDao().getById(delivered.id.value) != delivered.toEntity()
        ) return@withTransaction false
        val matching = escalationDao.getForTask(delivered.taskId.value)
            .filter { it.sourceOccurrenceId == delivered.id.value }
        if (matching.size > 1) return@withTransaction false
        val stored = matching.singleOrNull()
        if (stored != previous?.toEntity()) return@withTransaction false
        if (stored != null && stored.status != "ACTIVE") return@withTransaction false
        escalationDao.upsert(next.toEntity())
        nextOccurrence?.let { database.scheduledOccurrenceDao().upsert(it.toEntity()) }
        true
    }

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
