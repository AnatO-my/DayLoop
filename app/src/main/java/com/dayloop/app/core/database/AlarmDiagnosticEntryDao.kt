package com.dayloop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDiagnosticEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AlarmDiagnosticEntryEntity)

    @Query("SELECT * FROM alarm_diagnostic_entries ORDER BY created_at_epoch_millis DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AlarmDiagnosticEntryEntity>

    @Query("SELECT * FROM alarm_diagnostic_entries ORDER BY created_at_epoch_millis DESC, id DESC")
    fun observeAll(): Flow<List<AlarmDiagnosticEntryEntity>>

    @Query("DELETE FROM alarm_diagnostic_entries")
    suspend fun deleteAll()
}
