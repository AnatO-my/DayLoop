package com.dayloop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY title ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE projects SET goal_id = NULL WHERE goal_id = :id")
    suspend fun unlinkChildren(id: String)

    @Transaction
    suspend fun deleteAndUnlink(id: String): Int {
        unlinkChildren(id)
        return deleteById(id)
    }
}
