package com.dayloop.app.repository

import com.dayloop.app.core.model.Project
import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.core.model.GoalId
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun getProject(id: ProjectId): Project?
    suspend fun getProjectsForGoal(goalId: GoalId): List<Project>
    suspend fun saveProject(project: Project)
    suspend fun deleteProject(id: ProjectId): Boolean
}
