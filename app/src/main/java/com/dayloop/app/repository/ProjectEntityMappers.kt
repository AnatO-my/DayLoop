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
    protocolType = when (protocol) {
        is com.dayloop.app.core.model.SimplePhaseCycle -> "SIMPLE_PHASE_CYCLE"
        null -> null
    },
    protocolActiveDays = protocol?.activeDays,
    protocolRestDays = protocol?.restDays,
    protocolAnchorDate = protocol?.anchorDate?.toString(),
    protocolTaskTitle = protocol?.taskTemplate?.title,
    protocolTaskPriority = protocol?.taskTemplate?.priority?.name,
    version = version,
)

internal fun ProjectEntity.toDomain(): Project = Project(
    id = ProjectId(id),
    goalId = goalId?.let(::GoalId),
    title = title,
    status = ProjectStatus.valueOf(status),
    version = version,
    protocol = when (protocolType) {
        "SIMPLE_PHASE_CYCLE" -> com.dayloop.app.core.model.SimplePhaseCycle(
            anchorDate = java.time.LocalDate.parse(requireNotNull(protocolAnchorDate)),
            activeDays = requireNotNull(protocolActiveDays),
            restDays = requireNotNull(protocolRestDays),
            taskTemplate = com.dayloop.app.core.model.TaskTemplate(
                title = requireNotNull(protocolTaskTitle),
                priority = com.dayloop.app.core.model.TaskPriority.valueOf(requireNotNull(protocolTaskPriority))
            )
        )
        else -> null
    },
)
