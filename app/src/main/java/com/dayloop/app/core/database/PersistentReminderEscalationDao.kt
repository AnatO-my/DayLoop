package com.dayloop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistentReminderEscalationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(escalation: PersistentReminderEscalationEntity)

    @Query("SELECT * FROM persistent_reminder_escalations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PersistentReminderEscalationEntity?

    @Query("SELECT * FROM persistent_reminder_escalations WHERE task_id = :taskId ORDER BY created_at_epoch_millis, id")
    suspend fun getForTask(taskId: String): List<PersistentReminderEscalationEntity>

    @Query(
        """
        SELECT * FROM persistent_reminder_escalations
        WHERE status = 'ACTIVE'
        ORDER BY next_realert_at_epoch_millis, id
        """,
    )
    suspend fun getActive(): List<PersistentReminderEscalationEntity>

    @Query("SELECT * FROM persistent_reminder_escalations ORDER BY created_at_epoch_millis, id")
    fun observeAll(): Flow<List<PersistentReminderEscalationEntity>>

    @Query("DELETE FROM persistent_reminder_escalations WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM persistent_reminder_escalations WHERE task_id = :taskId")
    suspend fun deleteForTask(taskId: String): Int
}
