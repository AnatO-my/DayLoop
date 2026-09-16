package com.dayloop.app.repository

import com.dayloop.app.core.database.ProjectDao
import com.dayloop.app.core.model.GoalId
import com.dayloop.app.core.model.Project
import com.dayloop.app.core.model.ProjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.dayloop.app.repository.toDomain
import com.dayloop.app.repository.toEntity

class RoomProjectRepository(
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getProject(id: ProjectId): Project? = projectDao.getById(id.value)?.toDomain()

    override suspend fun getProjectsForGoal(goalId: GoalId): List<Project> =
        projectDao.getForGoal(goalId.value).map { it.toDomain() }

    override suspend fun saveProject(project: Project) {
        projectDao.upsert(project.toEntity())
    }

    override suspend fun deleteProject(id: ProjectId): Boolean = projectDao.deleteAndUnlink(id.value) > 0
}
