package com.dayloop.app.feature.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.WorkoutExercise
import com.dayloop.app.core.model.WorkoutRoutine
import com.dayloop.app.core.model.WorkoutRoutineId
import java.time.DayOfWeek

@Composable
fun WorkoutRoutineEditor(
    routine: WorkoutRoutine?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorkoutRoutine) -> Unit,
    onDelete: (() -> Unit)?,
    onAddToToday: (() -> Unit)?,
) {
    var title by rememberSaveable { mutableStateOf(routine?.title.orEmpty()) }
    var exerciseRows by rememberSaveable { mutableStateOf(routine?.exercises?.joinToString("\n") { "${it.name} | ${it.sets} | ${it.repetitions} | ${it.note}" }.orEmpty()) }
    var days by rememberSaveable { mutableStateOf(routine?.scheduledDays?.map(DayOfWeek::name)?.toSet().orEmpty()) }
    val routineId = rememberSaveable { routine?.id?.value ?: WorkoutRoutineId.generate().value }
    val exercises = exerciseRows.lineSequence().filter { it.isNotBlank() }.mapNotNull { row ->
        val fields = row.split("|", limit = 4).map(String::trim)
        if (fields.size !in 3..4) null else runCatching { WorkoutExercise(fields[0], fields[1].toInt(), fields[2].toInt(), fields.getOrElse(3) { "" }) }.getOrNull()
    }.toList()
    val validExercises = exercises.isNotEmpty() && exercises.size == exerciseRows.lineSequence().count { it.isNotBlank() }
    val canSave = title.isNotBlank() && validExercises && days.isNotEmpty() && !busy

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (routine == null) "Create routine" else "Workout routine") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Plan one exercise at a time; adapt activity to your abilities and seek qualified advice for health concerns.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(title, { title = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = true)
                Text("Exercise", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(exerciseRows, { exerciseRows = it }, label = { Text("One per line: name | sets | reps | optional note") }, modifier = Modifier.fillMaxWidth(), enabled = !busy, minLines = 3, isError = exerciseRows.isNotBlank() && !validExercises)
                if (!validExercises) Text("Add at least one valid exercise, for example: Squat | 3 | 8 | controlled pace", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Schedule", style = MaterialTheme.typography.titleMedium)
                WeekdaySelector(selected = days, enabled = !busy) { day ->
                    days = if (day in days) days - day else days + day
                }
                if (days.isEmpty()) Text("Choose at least one day.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = {
                onSave(WorkoutRoutine(WorkoutRoutineId(routineId), title.trim(), exercises, days.map(DayOfWeek::valueOf).toSet()))
            }) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = {
            Row {
                if (onAddToToday != null) TextButton(enabled = !busy, onClick = onAddToToday) { Text("Add to Today") }
                if (onDelete != null) TextButton(enabled = !busy, onClick = onDelete) { Text("Delete") }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdaySelector(
    selected: Set<String>,
    enabled: Boolean,
    onDayToggled: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day.name in selected,
                onClick = { onDayToggled(day.name) },
                enabled = enabled,
                label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
            )
        }
    }
}
