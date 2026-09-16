package com.dayloop.app.core.projects

import com.dayloop.app.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoalProgressCalculatorTest {

    private val calculator = GoalProgressCalculator()
    private val goalId = GoalId("goal-1")

    @Test
    fun `calculates binary progress correctly`() {
        val goal = Goal(goalId, "Title", progressPolicy = GoalProgressPolicy.BINARY, status = GoalStatus.ACTIVE)
        assertEquals(0.0, calculator.calculate(goal, emptyList(), emptyList()), 0.0)

        val completedGoal = goal.copy(status = GoalStatus.COMPLETED)
        assertEquals(1.0, calculator.calculate(completedGoal, emptyList(), emptyList()), 0.0)
    }

    @Test
    fun `calculates manual progress correctly`() {
        val goal = Goal(goalId, "Title", progressPolicy = GoalProgressPolicy.MANUAL, manualProgress = 0.45)
        assertEquals(0.45, calculator.calculate(goal, emptyList(), emptyList()), 0.0)
    }

    @Test
    fun `calculates count progress correctly for mixed units`() {
        val goal = Goal(goalId, "Title", progressPolicy = GoalProgressPolicy.COUNT)
        
        val task1 = Task(TaskId("t1"), "T1", status = TaskStatus.COMPLETED)
        val task2 = Task(TaskId("t2"), "T2", status = TaskStatus.ACTIVE)
        val project1 = Project(ProjectId("p1"), goalId, "P1", status = ProjectStatus.COMPLETED)
        val project2 = Project(ProjectId("p2"), goalId, "P2", status = ProjectStatus.ACTIVE)

        val progress = calculator.calculate(
            goal,
            tasks = listOf(task1, task2),
            projects = listOf(project1, project2)
        )

        // 2 completed out of 4 total units
        assertEquals(0.5, progress, 0.0)
    }

    @Test
    fun `calculates count progress correctly for empty goal`() {
        val goal = Goal(goalId, "Title", progressPolicy = GoalProgressPolicy.COUNT)
        assertEquals(0.0, calculator.calculate(goal, emptyList(), emptyList()), 0.0)
    }
}
