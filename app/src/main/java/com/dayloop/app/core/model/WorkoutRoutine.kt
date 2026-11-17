package com.dayloop.app.core.model

import java.time.DayOfWeek
import java.util.UUID

@JvmInline
value class WorkoutRoutineId(val value: String) {
    init { require(value.isNotBlank()) { "Workout routine id must not be blank." } }

    companion object { fun generate() = WorkoutRoutineId(UUID.randomUUID().toString()) }
}

/** A user-authored plan. It is intentionally not an alarm or health-data record. */
data class WorkoutRoutine(
    val id: WorkoutRoutineId,
    val title: String,
    val exercises: List<WorkoutExercise>,
    val scheduledDays: Set<DayOfWeek>,
) {
    init {
        require(title.isNotBlank()) { "Routine title must not be blank." }
        require(exercises.isNotEmpty()) { "A routine needs at least one exercise." }
        require(scheduledDays.isNotEmpty()) { "Choose at least one scheduled day." }
    }
}

data class WorkoutExercise(
    val name: String,
    val sets: Int,
    val repetitions: Int,
    val note: String = "",
) {
    init {
        require(name.isNotBlank()) { "Exercise name must not be blank." }
        require(sets in 1..99) { "Sets must be between 1 and 99." }
        require(repetitions in 1..999) { "Repetitions must be between 1 and 999." }
    }
}
