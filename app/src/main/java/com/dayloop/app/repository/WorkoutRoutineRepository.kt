package com.dayloop.app.repository

import com.dayloop.app.core.database.WorkoutRoutineDao
import com.dayloop.app.core.database.WorkoutRoutineEntity
import com.dayloop.app.core.model.WorkoutExercise
import com.dayloop.app.core.model.WorkoutRoutine
import com.dayloop.app.core.model.WorkoutRoutineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

interface WorkoutRoutineRepository {
    fun observeRoutines(): Flow<List<WorkoutRoutine>>
    suspend fun save(routine: WorkoutRoutine)
    suspend fun delete(id: WorkoutRoutineId)
}

class RoomWorkoutRoutineRepository(private val dao: WorkoutRoutineDao) : WorkoutRoutineRepository {
    override fun observeRoutines(): Flow<List<WorkoutRoutine>> = dao.observeAll().map { rows ->
        rows.mapNotNull(WorkoutRoutineEntity::toDomain)
    }

    override suspend fun save(routine: WorkoutRoutine) = dao.save(routine.toEntity())
    override suspend fun delete(id: WorkoutRoutineId) = dao.delete(id.value)
}

private fun WorkoutRoutine.toEntity() = WorkoutRoutineEntity(
    id = id.value,
    title = title.trim(),
    exercisesPayload = exercises.joinToString("\n") { exercise ->
        listOf(exercise.name, exercise.sets, exercise.repetitions, exercise.note).joinToString("\u001f") { field ->
            field.toString().replace("\\", "\\\\").replace("\u001f", "\\u001f").replace("\n", "\\n")
        }
    },
    scheduledDays = scheduledDays.sortedBy(DayOfWeek::getValue).joinToString(",") { it.value.toString() },
)

internal fun WorkoutRoutineEntity.toDomain(): WorkoutRoutine? = runCatching {
    val exercises = exercisesPayload.lineSequence().filter { it.isNotBlank() }.map { row ->
        val fields = row.split("\u001f")
        require(fields.size == 4)
        WorkoutExercise(unescape(fields[0]), fields[1].toInt(), fields[2].toInt(), unescape(fields[3]))
    }.toList()
    val days = scheduledDays.split(',').filter { it.isNotBlank() }.map { DayOfWeek.of(it.toInt()) }.toSet()
    WorkoutRoutine(WorkoutRoutineId(id), title, exercises, days)
}.getOrNull()

private fun unescape(value: String): String = buildString {
    var escaped = false
    value.forEach { char ->
        if (escaped) { append(if (char == 'n') '\n' else char); escaped = false }
        else if (char == '\\') escaped = true else append(char)
    }
    if (escaped) append('\\')
}
