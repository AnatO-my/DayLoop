package com.dayloop.app.core.projects

import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.GoalProgressPolicy
import com.dayloop.app.core.model.GoalStatus
import com.dayloop.app.core.model.Project
import com.dayloop.app.core.model.ProjectStatus
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskStatus

class GoalProgressCalculator {
    fun calculate(goal: Goal, tasks: List<Task>, projects: List<Project>): Double {
        return when (goal.progressPolicy) {
            GoalProgressPolicy.BINARY -> if (goal.status == GoalStatus.COMPLETED) 1.0 else 0.0
            GoalProgressPolicy.MANUAL -> goal.manualProgress
            GoalProgressPolicy.COUNT -> {
                val workUnits = tasks.size + projects.size
                if (workUnits == 0) return 0.0
                
                val completedUnits = tasks.count { it.status == TaskStatus.COMPLETED } +
                        projects.count { it.status == ProjectStatus.COMPLETED }
                
                completedUnits.toDouble() / workUnits
            }
        }
    }
}
