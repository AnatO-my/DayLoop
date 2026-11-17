package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.*
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import java.time.DayOfWeek

@Composable
internal fun TaskSettingsFields(draft: TaskEditorDraft, enabled: Boolean,
    change: (TaskEditorDraft) -> Unit, projects: List<Project>, projectId: String?,
    selectProject: (String?) -> Unit) {
    val selected = when {
        draft.deadlineEnabled -> "deadline"
        draft.timeBlockEnabled -> "block"
        draft.intervalEnabled -> "interval"
        draft.recurrenceEnabled -> "repeat"
        else -> "day"
    }
    Text("Schedule", style = MaterialTheme.typography.titleMedium)
    DayLoopOptionGrid(listOf(DayLoopPillOption("day", "On a date"),
        DayLoopPillOption("deadline", "Deadline"), DayLoopPillOption("block", "Time block"),
        DayLoopPillOption("interval", "Interval"), DayLoopPillOption("repeat", "Repeat")), selected,
        { choice -> change(when (choice) {
            "deadline" -> draft.onDeadlineEnabledChanged(true)
            "block" -> draft.onTimeBlockEnabledChanged(true)
            "interval" -> draft.onIntervalEnabledChanged(true)
            "repeat" -> draft.onRecurrenceEnabledChanged(true)
            else -> draft.copy(deadlineEnabled = false, timeBlockEnabled = false,
                intervalEnabled = false, recurrenceEnabled = false)
        }) }, enabled = enabled, modifier = Modifier.guideTarget(GuideTargets.SETTINGS_SCHEDULE))
    @Composable fun date(label: String, value: String, set: (String) -> TaskEditorDraft, optional: Boolean = false) {
        TemporalPickerField(label, value, TemporalKind.DATE, { change(set(it)) }, enabled = enabled, optional = optional)
    }
    @Composable fun time(label: String, value: String, set: (String) -> TaskEditorDraft) {
        TemporalPickerField(label, value, TemporalKind.TIME, { change(set(it)) }, enabled = enabled)
    }
    @Composable fun number(label: String, value: String, set: (String) -> TaskEditorDraft) {
        OutlinedTextField(value, { change(set(it)) }, label = { Text(label) }, enabled = enabled,
            singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    when (selected) {
        "day" -> date("Planning date", draft.allDayDate, { draft.copy(allDayDate = it) })
        "deadline" -> {
            date("Due date", draft.deadlineDate, draft::onDeadlineDateChanged)
            time("Due time", draft.deadlineTime, draft::onDeadlineTimeChanged)
        }
        "block" -> {
            date("Start date", draft.timeBlockStartDate, draft::onTimeBlockStartDateChanged)
            time("Start time", draft.timeBlockStartTime, draft::onTimeBlockStartTimeChanged)
            date("End date", draft.timeBlockEndDate, draft::onTimeBlockEndDateChanged)
            time("End time", draft.timeBlockEndTime, draft::onTimeBlockEndTimeChanged)
        }
        "interval" -> {
            date("Start date", draft.intervalStartDate, draft::onIntervalStartDateChanged)
            time("Window start", draft.intervalWindowStartTime, draft::onIntervalWindowStartTimeChanged)
            time("Window end", draft.intervalWindowEndTime, draft::onIntervalWindowEndTimeChanged)
            number("Every (minutes)", draft.intervalEveryMinutes, draft::onIntervalEveryMinutesChanged)
        }
        "repeat" -> {
            DayLoopOptionGrid(RecurrenceFrequency.entries.map { DayLoopPillOption(it.name, it.name.lowercase().replaceFirstChar { c -> c.uppercase() }) },
                draft.recurrenceFrequency.name, { change(draft.onRecurrenceFrequencyChanged(RecurrenceFrequency.valueOf(it))) }, enabled = enabled)
            date("Starts on", draft.recurrenceStartDate, draft::onRecurrenceStartDateChanged)
            date("Ends on", draft.recurrenceEndDate, draft::onRecurrenceEndDateChanged, optional = true)
            number("Repeat every", draft.recurrenceEvery, draft::onRecurrenceEveryChanged)
            if (draft.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                DayOfWeek.entries.forEach { day ->
                    Row {
                        Checkbox(day in draft.recurrenceDaysOfWeek, { change(draft.onRecurrenceDayToggled(day)) }, enabled = enabled)
                        Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
    Text("Priority", style = MaterialTheme.typography.titleMedium)
    DayLoopOptionGrid(TaskPriority.entries.map { DayLoopPillOption(it.name, it.name.lowercase()) },
        draft.priority.name, { change(draft.onPriorityChanged(TaskPriority.valueOf(it))) }, enabled = enabled)
    Text("Project", style = MaterialTheme.typography.titleMedium)
    DayLoopOptionGrid(listOf(DayLoopPillOption("", "No project")) + projects.map { DayLoopPillOption(it.id.value, it.title) },
        projectId.orEmpty(), { selectProject(it.ifBlank { null }) }, enabled = enabled)
    Text("Reminder behavior", style = MaterialTheme.typography.titleMedium)
    DayLoopOptionGrid(ReminderMode.entries.map { DayLoopPillOption(it.name, it.name.lowercase()) },
        draft.reminderMode.name, { change(draft.onReminderModeChanged(ReminderMode.valueOf(it))) }, enabled = enabled)
    if (draft.reminderMode.supportsRealerts) {
        number("Repeat alert every (minutes)", draft.reminderRealertMinutes, draft::onReminderRealertMinutesChanged)
        number("Maximum repeat alerts", draft.reminderMaximumRealerts, draft::onReminderMaximumRealertsChanged)
    }
    Row { Checkbox(draft.reminderSnoozeAllowed, { change(draft.onReminderSnoozeAllowedChanged(it)) }, enabled = enabled); Text("Allow snooze") }
    Row { Checkbox(draft.reminderRescheduleAllowed, { change(draft.onReminderRescheduleAllowedChanged(it)) }, enabled = enabled); Text("Allow rescheduling") }
}
