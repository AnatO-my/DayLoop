package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import java.time.DayOfWeek

@Composable
fun TaskEditorScreen(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    onTaskSaved: (Task) -> Unit,
    modifier: Modifier = Modifier,
    existingTask: Task? = null,
    onCancel: () -> Unit = {},
    createTaskId: () -> TaskId = { TaskId.generate() },
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
        ) {
            Text(
                text = if (existingTask == null) "New task" else "Edit task",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = draft.title,
                onValueChange = { title -> onDraftChange(draft.onTitleChanged(title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Task title" },
                label = { Text(text = "Task title") },
                isError = draft.errorMessage != null,
                supportingText = draft.errorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
            PrioritySelector(
                selectedPriority = draft.priority,
                onPrioritySelected = { priority -> onDraftChange(draft.onPriorityChanged(priority)) },
            )
            DeadlineEditor(
                draft = draft,
                onDraftChange = onDraftChange,
            )
            TimeBlockEditor(
                draft = draft,
                onDraftChange = onDraftChange,
            )
            IntervalEditor(
                draft = draft,
                onDraftChange = onDraftChange,
            )
            RecurrenceEditor(
                draft = draft,
                onDraftChange = onDraftChange,
            )
            ReminderPolicyEditor(
                draft = draft,
                onDraftChange = onDraftChange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Cancel")
                }
                Button(
                    onClick = {
                        when (val result = if (existingTask == null) {
                            draft.createTask(createTaskId = createTaskId)
                        } else {
                            draft.applyTo(existingTask)
                        }) {
                            is TaskEditorSubmitResult.Invalid -> onDraftChange(result.draft)
                            is TaskEditorSubmitResult.Saved -> onTaskSaved(result.task)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Save")
                }
            }
        }
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task priority" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Text(
            text = "Priority",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
        ) {
            TaskPriority.entries.forEach { priority ->
                FilterChip(
                    selected = priority == selectedPriority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(text = priority.displayLabel()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DeadlineEditor(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task deadline" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = draft.deadlineEnabled,
                onCheckedChange = { enabled -> onDraftChange(draft.onDeadlineEnabledChanged(enabled)) },
                modifier = Modifier.semantics { contentDescription = "Add deadline" },
            )
            Text(
                text = "Add deadline",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (draft.deadlineEnabled) {
            OutlinedTextField(
                value = draft.deadlineDate,
                onValueChange = { date -> onDraftChange(draft.onDeadlineDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Deadline date" },
                label = { Text(text = "Deadline date") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.deadlineErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.deadlineTime,
                onValueChange = { time -> onDraftChange(draft.onDeadlineTimeChanged(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Deadline time" },
                label = { Text(text = "Deadline time") },
                placeholder = { Text(text = "HH:mm") },
                isError = draft.deadlineErrorMessage != null,
                supportingText = draft.deadlineErrorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun TimeBlockEditor(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task time block" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = draft.timeBlockEnabled,
                onCheckedChange = { enabled -> onDraftChange(draft.onTimeBlockEnabledChanged(enabled)) },
                modifier = Modifier.semantics { contentDescription = "Add time block" },
            )
            Text(
                text = "Add time block",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (draft.timeBlockEnabled) {
            OutlinedTextField(
                value = draft.timeBlockStartDate,
                onValueChange = { date -> onDraftChange(draft.onTimeBlockStartDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Time block start date" },
                label = { Text(text = "Start date") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.timeBlockErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.timeBlockStartTime,
                onValueChange = { time -> onDraftChange(draft.onTimeBlockStartTimeChanged(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Time block start time" },
                label = { Text(text = "Start time") },
                placeholder = { Text(text = "HH:mm") },
                isError = draft.timeBlockErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.timeBlockEndDate,
                onValueChange = { date -> onDraftChange(draft.onTimeBlockEndDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Time block end date" },
                label = { Text(text = "End date") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.timeBlockErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.timeBlockEndTime,
                onValueChange = { time -> onDraftChange(draft.onTimeBlockEndTimeChanged(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Time block end time" },
                label = { Text(text = "End time") },
                placeholder = { Text(text = "HH:mm") },
                isError = draft.timeBlockErrorMessage != null,
                supportingText = draft.timeBlockErrorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun IntervalEditor(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task interval" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = draft.intervalEnabled,
                onCheckedChange = { enabled -> onDraftChange(draft.onIntervalEnabledChanged(enabled)) },
                modifier = Modifier.semantics { contentDescription = "Add interval" },
            )
            Text(
                text = "Add interval",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (draft.intervalEnabled) {
            OutlinedTextField(
                value = draft.intervalStartDate,
                onValueChange = { date -> onDraftChange(draft.onIntervalStartDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Interval start date" },
                label = { Text(text = "Start date") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.intervalErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.intervalEveryMinutes,
                onValueChange = { minutes -> onDraftChange(draft.onIntervalEveryMinutesChanged(minutes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Interval duration minutes" },
                label = { Text(text = "Every minutes") },
                placeholder = { Text(text = "90") },
                isError = draft.intervalErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.intervalWindowStartTime,
                onValueChange = { time -> onDraftChange(draft.onIntervalWindowStartTimeChanged(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Interval window start time" },
                label = { Text(text = "Window start") },
                placeholder = { Text(text = "HH:mm") },
                isError = draft.intervalErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.intervalWindowEndTime,
                onValueChange = { time -> onDraftChange(draft.onIntervalWindowEndTimeChanged(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Interval window end time" },
                label = { Text(text = "Window end") },
                placeholder = { Text(text = "HH:mm") },
                isError = draft.intervalErrorMessage != null,
                supportingText = draft.intervalErrorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun RecurrenceEditor(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Task recurrence" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = draft.recurrenceEnabled,
                onCheckedChange = { enabled -> onDraftChange(draft.onRecurrenceEnabledChanged(enabled)) },
                modifier = Modifier.semantics { contentDescription = "Add recurrence" },
            )
            Text(
                text = "Add recurrence",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (draft.recurrenceEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            ) {
                RecurrenceFrequency.entries.forEach { frequency ->
                    FilterChip(
                        selected = frequency == draft.recurrenceFrequency,
                        onClick = { onDraftChange(draft.onRecurrenceFrequencyChanged(frequency)) },
                        label = { Text(text = frequency.displayLabel()) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = frequency.contentDescription() },
                    )
                }
            }
            OutlinedTextField(
                value = draft.recurrenceEvery,
                onValueChange = { every -> onDraftChange(draft.onRecurrenceEveryChanged(every)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Recurrence interval count" },
                label = { Text(text = "Every") },
                placeholder = { Text(text = "1") },
                isError = draft.recurrenceErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.recurrenceStartDate,
                onValueChange = { date -> onDraftChange(draft.onRecurrenceStartDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Recurrence start date" },
                label = { Text(text = "Start date") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.recurrenceErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.recurrenceEndDate,
                onValueChange = { date -> onDraftChange(draft.onRecurrenceEndDateChanged(date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Recurrence end date" },
                label = { Text(text = "End date optional") },
                placeholder = { Text(text = "YYYY-MM-DD") },
                isError = draft.recurrenceErrorMessage != null,
                supportingText = draft.recurrenceErrorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
            if (draft.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                WeekdaySelector(
                    selectedDays = draft.recurrenceDaysOfWeek,
                    onDayToggled = { day -> onDraftChange(draft.onRecurrenceDayToggled(day)) },
                )
            }
        }
    }
}

@Composable
private fun WeekdaySelector(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val days = DayOfWeek.values().toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Recurrence weekdays" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
    ) {
        days.chunked(4).forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            ) {
                rowDays.forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = { onDayToggled(day) },
                        label = { Text(text = day.shortLabel()) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Toggle ${day.displayLabel()}" },
                    )
                }
                repeat(4 - rowDays.size) {
                    Row(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}


@Composable
private fun ReminderPolicyEditor(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Reminder policy" },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Text(
            text = "Reminder",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
        ) {
            ReminderMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == draft.reminderMode,
                    onClick = { onDraftChange(draft.onReminderModeChanged(mode)) },
                    label = { Text(text = mode.displayLabel()) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = mode.contentDescription() },
                )
            }
        }
        if (draft.reminderMode.supportsRealerts) {
            OutlinedTextField(
                value = draft.reminderRealertMinutes,
                onValueChange = { minutes -> onDraftChange(draft.onReminderRealertMinutesChanged(minutes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Reminder re-alert interval minutes" },
                label = { Text(text = "Re-alert every minutes") },
                placeholder = { Text(text = "15") },
                isError = draft.reminderPolicyErrorMessage != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.reminderMaximumRealerts,
                onValueChange = { count -> onDraftChange(draft.onReminderMaximumRealertsChanged(count)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Reminder maximum re-alerts" },
                label = { Text(text = "Maximum re-alerts") },
                placeholder = { Text(text = "3") },
                isError = draft.reminderPolicyErrorMessage != null,
                supportingText = draft.reminderPolicyErrorMessage?.let { message ->
                    { Text(text = message) }
                },
                singleLine = true,
            )
        } else if (draft.reminderPolicyErrorMessage != null) {
            Text(
                text = draft.reminderPolicyErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (draft.reminderMode == ReminderMode.STRICT) {
            ReminderPolicyToggle(
                checked = draft.reminderSnoozeAllowed,
                onCheckedChange = { allowed -> onDraftChange(draft.onReminderSnoozeAllowedChanged(allowed)) },
                label = "Allow snooze",
                contentDescription = "Allow snooze",
            )
            ReminderPolicyToggle(
                checked = draft.reminderRescheduleAllowed,
                onCheckedChange = { allowed -> onDraftChange(draft.onReminderRescheduleAllowedChanged(allowed)) },
                label = "Allow reschedule",
                contentDescription = "Allow reschedule",
            )
        }
    }
}

@Composable
private fun ReminderPolicyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    contentDescription: String,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
private fun TaskPriority.displayLabel(): String = when (this) {
    TaskPriority.LOW -> "Low"
    TaskPriority.NORMAL -> "Normal"
    TaskPriority.HIGH -> "High"
}

private fun ReminderMode.displayLabel(): String = when (this) {
    ReminderMode.NORMAL -> "Normal"
    ReminderMode.PERSISTENT -> "Persistent"
    ReminderMode.STRICT -> "Strict"
    ReminderMode.FOCUS -> "Focus"
}

private fun ReminderMode.contentDescription(): String = "Reminder ${displayLabel()}"

private fun RecurrenceFrequency.displayLabel(): String = when (this) {
    RecurrenceFrequency.DAILY -> "Daily"
    RecurrenceFrequency.WEEKLY -> "Weekly"
    RecurrenceFrequency.MONTHLY -> "Monthly"
    RecurrenceFrequency.YEARLY -> "Yearly"
}

private fun RecurrenceFrequency.contentDescription(): String = "Recurrence ${displayLabel()}"

private fun DayOfWeek.displayLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

@Preview(showBackground = true)
@Composable
private fun TaskEditorScreenPreview() {
    DayLoopTheme {
        TaskEditorScreen(
            draft = TaskEditorDraft(title = "Plan today"),
            onDraftChange = {},
            onTaskSaved = {},
        )
    }
}

