package com.dayloop.app.core.model

import java.time.DayOfWeek
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutRoutineTest {
    @Test fun `routine requires exercise and scheduled day`() {
        val id = WorkoutRoutineId("routine")
        assertThrows(IllegalArgumentException::class.java) { WorkoutRoutine(id, "Strength", emptyList(), setOf(DayOfWeek.MONDAY)) }
        assertThrows(IllegalArgumentException::class.java) { WorkoutRoutine(id, "Strength", listOf(WorkoutExercise("Squat", 3, 8)), emptySet()) }
    }

    @Test fun `exercise validates prescription bounds`() {
        assertThrows(IllegalArgumentException::class.java) { WorkoutExercise("Squat", 0, 8) }
        assertThrows(IllegalArgumentException::class.java) { WorkoutExercise("Squat", 3, 0) }
    }
}
