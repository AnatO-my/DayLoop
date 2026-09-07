package com.dayloop.app.repository

import com.dayloop.app.core.database.TaskDao
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val taskDao: TaskDao,
) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> = taskDao.observeAll()
        .map { rows -> rows.map { row -> row.toDomain() } }

    override fun observeTask(id: TaskId): Flow<Task?> = taskDao.observeById(id.value)
        .map { row -> row?.toDomain() }

    override suspend fun getTask(id: TaskId): Task? = taskDao.getById(id.value)?.toDomain()

    override suspend fun saveTask(task: Task) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun deleteTask(id: TaskId): Boolean = taskDao.deleteById(id.value) > 0
}
