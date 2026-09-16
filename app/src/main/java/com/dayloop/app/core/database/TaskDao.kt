package com.dayloop.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY id")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY id")
    suspend fun getForProject(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY id")
    fun observeForProject(projectId: String): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
