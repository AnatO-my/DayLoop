package com.dayloop.app.repository

import com.dayloop.app.core.database.GoalDao
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.GoalId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGoalRepository(
    private val goalDao: GoalDao,
) : GoalRepository {
    override fun observeGoals(): Flow<List<Goal>> = goalDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getGoal(id: GoalId): Goal? = goalDao.getById(id.value)?.toDomain()

    override suspend fun saveGoal(goal: Goal) {
        goalDao.upsert(goal.toEntity())
    }

    override suspend fun deleteGoal(id: GoalId): Boolean = goalDao.deleteById(id.value) > 0
}
