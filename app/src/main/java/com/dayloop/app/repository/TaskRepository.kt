package com.dayloop.app.repository

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>

    fun observeTask(id: TaskId): Flow<Task?>

    suspend fun getTask(id: TaskId): Task?

    suspend fun saveTask(task: Task)

    suspend fun deleteTask(id: TaskId): Boolean
}
