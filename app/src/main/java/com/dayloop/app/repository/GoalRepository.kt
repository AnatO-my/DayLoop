package com.dayloop.app.repository

import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.GoalId
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    suspend fun getGoal(id: GoalId): Goal?
    suspend fun saveGoal(goal: Goal)
    suspend fun deleteGoal(id: GoalId): Boolean
}
