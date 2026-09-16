package com.dayloop.app.repository

import com.dayloop.app.core.database.GoalEntity
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.GoalId
import com.dayloop.app.core.model.GoalProgressPolicy
import com.dayloop.app.core.model.GoalStatus
import java.time.LocalDate

internal fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id.value,
    title = title,
    targetDate = targetDate?.toString(),
    progressPolicy = progressPolicy.name,
    status = status.name,
)

internal fun GoalEntity.toDomain(): Goal = Goal(
    id = GoalId(id),
    title = title,
    targetDate = targetDate?.let(LocalDate::parse),
    progressPolicy = GoalProgressPolicy.valueOf(progressPolicy),
    status = GoalStatus.valueOf(status),
)
