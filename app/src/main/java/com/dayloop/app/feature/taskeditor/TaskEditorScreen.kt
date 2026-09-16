package com.dayloop.app.feature.taskeditor

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.*
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.designsystem.DayLoopChoice
import com.dayloop.app.ui.designsystem.DayLoopHeader
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TaskEditorScreen(draft: TaskEditorDraft, onDraftChange: (TaskEditorDraft) -> Unit, onTaskSaved: (Task) -> Unit,
    modifier: Modifier = Modifier, existingTask: Task? = null, onCancel: () -> Unit = {},
    createTaskId: () -> TaskId = { TaskId.generate() }, saving: Boolean = false,
    projects: List<Project> = emptyList(), selectedProjectId: String? = null, onProjectSelected: (String?) -> Unit = {},
    onDelete: (() -> Unit)? = null, onReopen: (() -> Unit)? = null, today: LocalDate? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null) {
    var scheduleDraft by rememberSaveable { mutableStateOf<TaskEditorDraft?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val mode = when {
        draft.deadlineEnabled -> "deadline"
        draft.timeBlockEnabled -> "block"
        draft.intervalEnabled -> "interval"
        draft.recurrenceEnabled -> "repeat"
        else -> "day"
    }
    val fallbackDate = draft.allDayDate.ifBlank { today?.toString().orEmpty() }
    fun submit() {
        if (saving) return
        when (val result = if (existingTask == null) draft.createTask(createTaskId) else draft.applyTo(existingTask)) {
            is TaskEditorSubmitResult.Invalid -> onDraftChange(result.draft)
            is TaskEditorSubmitResult.Saved -> onTaskSaved(result.task)
        }
    }
    fun editRange(interval: Boolean) {
        scheduleDraft = if (interval) draft.onIntervalEnabledChanged(true).copy(
            intervalStartDate = draft.intervalStartDate.ifBlank { fallbackDate },
            intervalEveryMinutes = draft.intervalEveryMinutes.ifBlank { "60" },
            intervalWindowStartTime = draft.intervalWindowStartTime.ifBlank { "09:00" },
            intervalWindowEndTime = draft.intervalWindowEndTime.ifBlank { "17:00" })
        else draft.onTimeBlockEnabledChanged(true).copy(
            timeBlockStartDate = draft.timeBlockStartDate.ifBlank { fallbackDate },
            timeBlockEndDate = draft.timeBlockEndDate.ifBlank { fallbackDate },
            timeBlockStartTime = draft.timeBlockStartTime.ifBlank { "09:00" },
            timeBlockEndTime = draft.timeBlockEndTime.ifBlank { "10:00" })
    }
    scheduleDraft?.let { staged ->
        ScheduleEditor(staged, today, { scheduleDraft = null }, {
            onDraftChange(it.copy(timeBlockErrorMessage = null, intervalErrorMessage = null))
            scheduleDraft = null
        })
    }
    Surface(modifier = modifier.then(if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "row-${existingTask?.id?.value ?: "new"}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else Modifier), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(resource = R.drawable.ic_action_close, label = "Cancel", enabled = !saving, onClick = onCancel)
                Text(if (existingTask == null) "New task" else "Edit task", modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                DayLoopIconButton(resource = R.drawable.ic_action_check, label = "Save task", enabled = !saving && draft.canSubmit, onClick = ::submit)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title", style = MaterialTheme.typography.labelMedium)
                    TextField(value = draft.title, onValueChange = { onDraftChange(draft.onTitleChanged(it)) },
                        enabled = !saving, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Task title" }
                            .then(if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "text-${existingTask?.id?.value ?: "new"}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier),
                        placeholder = { Text("Task title") }, shape = RoundedCornerShape(12.dp),
                        singleLine = false, maxLines = 3, isError = draft.errorMessage != null,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    DayLoopPillTabRow(TaskPriority.entries.map { DayLoopPillOption(it.name, it.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        draft.priority.name, { if (!saving) onDraftChange(draft.onPriorityChanged(TaskPriority.valueOf(it))) })
                }
                DayLoopChoice("Schedule", listOf("day" to "Anytime", "deadline" to "Deadline", "block" to "Time block",
                    "interval" to "Interval", "repeat" to "Repeat"), mode, { selected ->
                    when (selected) {
                        "day" -> onDraftChange(draft.copy(deadlineEnabled = false, timeBlockEnabled = false,
                            intervalEnabled = false, recurrenceEnabled = false, allDayDate = fallbackDate))
                        "deadline" -> onDraftChange(draft.onDeadlineEnabledChanged(true).copy(
                            deadlineDate = draft.deadlineDate.ifBlank { fallbackDate }, deadlineTime = draft.deadlineTime.ifBlank { "17:00" }))
                        "block" -> editRange(false)
                        "interval" -> editRange(true)
                        "repeat" -> onDraftChange(draft.onRecurrenceEnabledChanged(true).copy(
                            recurrenceStartDate = draft.recurrenceStartDate.ifBlank { fallbackDate }))
                    }
                }, !saving)
                when (mode) {
                    "day" -> TemporalPickerField("Planning date", draft.allDayDate, TemporalKind.DATE,
                        { onDraftChange(draft.copy(allDayDate = it, errorMessage = null)) }, enabled = !saving)
                    "deadline" -> {
                        TemporalPickerField("Due date", draft.deadlineDate, TemporalKind.DATE,
                            { onDraftChange(draft.onDeadlineDateChanged(it)) }, enabled = !saving)
                        TemporalPickerField("Due time", draft.deadlineTime, TemporalKind.TIME,
                            { onDraftChange(draft.onDeadlineTimeChanged(it)) }, enabled = !saving)
                    }
                    "block", "interval" -> Surface(onClick = { editRange(mode == "interval") }, enabled = !saving,
                        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (mode == "block") "${draft.timeBlockStartDate}\n${draft.timeBlockStartTime} - ${draft.timeBlockEndTime}"
                                else "Every ${draft.intervalEveryMinutes} min\n${draft.intervalWindowStartTime} - ${draft.intervalWindowEndTime}",
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            DayLoopIcon(R.drawable.ic_action_edit, "Edit schedule")
                        }
                    }
                    "repeat" -> RecurrenceFields(draft, onDraftChange, !saving)
                }
                DayLoopChoice("Project", listOf("" to "No project") + projects.map { it.id.value to it.title },
                    selectedProjectId.orEmpty(), { onProjectSelected(it.takeIf(String::isNotEmpty)) },
                    enabled = !saving && existingTask?.provenance == null)
                DayLoopChoice("Reminder", ReminderMode.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    draft.reminderMode.name, { onDraftChange(draft.onReminderModeChanged(ReminderMode.valueOf(it))) }, !saving)
                
                SubtasksSection(draft.subtasks, !saving) { onDraftChange(draft.onSubtasksChanged(it)) }

                if (draft.reminderMode == ReminderMode.PERSISTENT || draft.reminderMode == ReminderMode.STRICT) {
                    NumberField("Re-alert every (minutes)", draft.reminderRealertMinutes, !saving) { onDraftChange(draft.onReminderRealertMinutesChanged(it)) }
                    NumberField("Maximum re-alerts", draft.reminderMaximumRealerts, !saving) { onDraftChange(draft.onReminderMaximumRealertsChanged(it)) }
                }
                TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide options" else "More options") }
                if (advanced) {
                    ToggleField("Allow snooze", draft.reminderSnoozeAllowed, !saving) { onDraftChange(draft.onReminderSnoozeAllowedChanged(it)) }
                    ToggleField("Allow reschedule", draft.reminderRescheduleAllowed, !saving) { onDraftChange(draft.onReminderRescheduleAllowedChanged(it)) }
                    Row {
                        onReopen?.let { DayLoopIconButton(resource = R.drawable.ic_action_reset, label = "Reopen task", enabled = !saving, onClick = it) }
                        onDelete?.let { DayLoopIconButton(resource = R.drawable.ic_action_delete, label = "Delete task", enabled = !saving, onClick = it) }
                    }
                }
                listOfNotNull(draft.errorMessage, draft.deadlineErrorMessage, draft.timeBlockErrorMessage,
                    draft.intervalErrorMessage, draft.recurrenceErrorMessage, draft.reminderPolicyErrorMessage).distinct().forEach {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SubtasksSection(subtasks: List<Subtask>, enabled: Boolean, onSubtasksChanged: (List<Subtask>) -> Unit) {
    var newSubtaskTitle by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Subtasks", style = MaterialTheme.typography.labelMedium)
        subtasks.forEachIndexed { index, subtask ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = subtask.isCompleted, onCheckedChange = { checked ->
                    onSubtasksChanged(subtasks.toMutableList().apply { set(index, subtask.copy(isCompleted = checked)) })
                }, enabled = enabled)
                Text(subtask.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
                DayLoopIconButton(resource = R.drawable.ic_action_close, label = "Remove subtask", enabled = enabled, onClick = {
                    onSubtasksChanged(subtasks.toMutableList().apply { removeAt(index) })
                })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newSubtaskTitle, onValueChange = { newSubtaskTitle = it },
                modifier = Modifier.weight(1f), placeholder = { Text("Add subtask...") },
                enabled = enabled, singleLine = true, shape = RoundedCornerShape(12.dp))
            DayLoopIconButton(resource = R.drawable.ic_action_add, label = "Add subtask", enabled = enabled && newSubtaskTitle.isNotBlank(), onClick = {
                onSubtasksChanged(subtasks + Subtask(UUID.randomUUID().toString(), newSubtaskTitle.trim()))
                newSubtaskTitle = ""
            })
        }
    }
}

@Composable
private fun RecurrenceFields(draft: TaskEditorDraft, change: (TaskEditorDraft) -> Unit, enabled: Boolean) {
    DayLoopChoice("Frequency", RecurrenceFrequency.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) },
        draft.recurrenceFrequency.name, { change(draft.onRecurrenceFrequencyChanged(RecurrenceFrequency.valueOf(it))) }, enabled)
    NumberField("Repeat interval", draft.recurrenceEvery, enabled) { change(draft.onRecurrenceEveryChanged(it)) }
    TemporalPickerField("Starts", draft.recurrenceStartDate, TemporalKind.DATE,
        { change(draft.onRecurrenceStartDateChanged(it)) }, enabled = enabled)
    TemporalPickerField("Ends", draft.recurrenceEndDate, TemporalKind.DATE,
        { change(draft.onRecurrenceEndDateChanged(it)) }, enabled = enabled, optional = true)
    if (draft.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
        DayOfWeek.entries.forEach { day ->
            ToggleField(day.name.lowercase().replaceFirstChar(Char::uppercase), day in draft.recurrenceDaysOfWeek, enabled) {
                change(draft.onRecurrenceDayToggled(day))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, change, enabled = enabled, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
}

@Composable
private fun ToggleField(label: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, change, enabled = enabled)
    }
}
