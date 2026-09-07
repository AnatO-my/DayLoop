package com.dayloop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledOccurrenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(occurrence: ScheduledOccurrenceEntity)

    @Query("SELECT * FROM scheduled_occurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduledOccurrenceEntity?

    @Query("SELECT * FROM scheduled_occurrences WHERE task_id = :taskId ORDER BY fire_at_epoch_millis, id")
    suspend fun getForTask(taskId: String): List<ScheduledOccurrenceEntity>

    @Query("SELECT * FROM scheduled_occurrences WHERE status = 'SCHEDULED' ORDER BY fire_at_epoch_millis, id")
    suspend fun getScheduled(): List<ScheduledOccurrenceEntity>

    @Query("SELECT * FROM scheduled_occurrences ORDER BY fire_at_epoch_millis, id")
    fun observeAll(): Flow<List<ScheduledOccurrenceEntity>>

    @Query("DELETE FROM scheduled_occurrences WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM scheduled_occurrences WHERE task_id = :taskId")
    suspend fun deleteForTask(taskId: String): Int
}