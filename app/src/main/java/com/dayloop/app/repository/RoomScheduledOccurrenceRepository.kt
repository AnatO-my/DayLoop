package com.dayloop.app.repository

import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.database.ScheduledOccurrenceDao
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduledOccurrenceRepository(
    private val scheduledOccurrenceDao: ScheduledOccurrenceDao,
) : ScheduledOccurrenceRepository {
    override fun observeOccurrences(): Flow<List<ScheduledOccurrence>> = scheduledOccurrenceDao.observeAll()
        .map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun getOccurrence(id: ScheduledOccurrenceId): ScheduledOccurrence? =
        scheduledOccurrenceDao.getById(id.value)?.toDomain()

    override suspend fun getOccurrencesForTask(taskId: TaskId): List<ScheduledOccurrence> =
        scheduledOccurrenceDao.getForTask(taskId.value).map { row -> row.toDomain() }

    override suspend fun getScheduledOccurrences(): List<ScheduledOccurrence> =
        scheduledOccurrenceDao.getScheduled().map { row -> row.toDomain() }

    override suspend fun saveOccurrence(occurrence: ScheduledOccurrence) {
        scheduledOccurrenceDao.upsert(occurrence.toEntity())
    }

    override suspend fun deleteOccurrence(id: ScheduledOccurrenceId): Boolean =
        scheduledOccurrenceDao.deleteById(id.value) > 0

    override suspend fun deleteOccurrencesForTask(taskId: TaskId): Int =
        scheduledOccurrenceDao.deleteForTask(taskId.value)
}