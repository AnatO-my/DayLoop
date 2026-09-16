package com.dayloop.app.repository

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.ProjectId
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>

    fun observeTask(id: TaskId): Flow<Task?>

    suspend fun getTask(id: TaskId): Task?

    suspend fun saveTask(task: Task)

    suspend fun getTasksForProject(projectId: ProjectId): List<Task>

    fun observeTasksForProject(projectId: ProjectId): Flow<List<Task>>

    suspend fun deleteTask(id: TaskId): Boolean
}
