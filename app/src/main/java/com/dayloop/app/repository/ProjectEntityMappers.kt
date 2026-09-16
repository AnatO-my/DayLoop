package com.dayloop.app.repository

import com.dayloop.app.core.database.ProjectEntity
import com.dayloop.app.core.model.GoalId
import com.dayloop.app.core.model.Project
import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.core.model.ProjectStatus

internal fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id.value,
    goalId = goalId?.value,
    title = title,
    status = status.name,
)

internal fun ProjectEntity.toDomain(): Project = Project(
    id = ProjectId(id),
    goalId = goalId?.let(::GoalId),
    title = title,
    status = ProjectStatus.valueOf(status),
)
