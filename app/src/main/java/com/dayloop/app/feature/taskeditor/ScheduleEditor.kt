package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.R
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.ui.components.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
internal fun ScheduleEditor(draft: TaskEditorDraft, today: LocalDate?, onDismiss: () -> Unit,
    onDone: (TaskEditorDraft) -> Unit) {
    var working by rememberSaveable { mutableStateOf(if (draft.timeBlockEnabled || draft.intervalEnabled) draft else
        draft.onTimeBlockEnabledChanged(true).copy(timeBlockStartDate = today?.toString().orEmpty(),
            timeBlockEndDate = today?.toString().orEmpty(), timeBlockStartTime = "09:00", timeBlockEndTime = "10:00")) }
    val interval = working.intervalEnabled
    val date = if (interval) working.intervalStartDate else working.timeBlockStartDate
    val startTime = if (interval) working.intervalWindowStartTime else working.timeBlockStartTime
    val endTime = if (interval) working.intervalWindowEndTime else working.timeBlockEndTime
    val start = runCatching { LocalDate.parse(date).atTime(LocalTime.parse(startTime)) }.getOrNull()
    val end = runCatching { LocalDate.parse(if (interval) date else working.timeBlockEndDate).atTime(LocalTime.parse(endTime)) }.getOrNull()
    val origin = start?.toLocalDate()?.atStartOfDay()
    val endMinutes = if (origin != null && end != null) ChronoUnit.MINUTES.between(origin, end) else 0L
    val validation = remember(working) {
        working.copy(title = working.title.ifBlank { "Schedule" }, reminderMode = ReminderMode.NORMAL)
            .createTask(createTaskId = { com.dayloop.app.core.model.TaskId("schedule-validation") })
    }
    val valid = validation is TaskEditorSubmitResult.Saved
    val error = (validation as? TaskEditorSubmitResult.Invalid)?.draft?.let {
        it.timeBlockErrorMessage ?: it.intervalErrorMessage
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    DayLoopIconButton(R.drawable.ic_action_back, "Cancel schedule", onClick = onDismiss)
                    Text(if (interval) "Interval" else "Time block", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    DayLoopIconButton(resource = R.drawable.ic_action_check, label = "Done", enabled = valid, onClick = { onDone(working) })
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DayLoopPillTabRow(listOf(DayLoopPillOption("block", "Time block"), DayLoopPillOption("interval", "Interval")),
                        if (interval) "interval" else "block", { mode ->
                            working = if (mode == "interval") working.onIntervalEnabledChanged(true).copy(
                                intervalStartDate = working.intervalStartDate.ifBlank { date.ifBlank { today?.toString().orEmpty() } },
                                intervalEveryMinutes = working.intervalEveryMinutes.ifBlank { "60" },
                                intervalWindowStartTime = working.intervalWindowStartTime.ifBlank { "09:00" },
                                intervalWindowEndTime = working.intervalWindowEndTime.ifBlank { "17:00" })
                            else working.onTimeBlockEnabledChanged(true).copy(
                                timeBlockStartDate = working.timeBlockStartDate.ifBlank { date },
                                timeBlockEndDate = working.timeBlockEndDate.ifBlank { date },
                                timeBlockStartTime = working.timeBlockStartTime.ifBlank { "09:00" },
                                timeBlockEndTime = working.timeBlockEndTime.ifBlank { "10:00" })
                        })
                    TemporalPickerField("Start date", date, TemporalKind.DATE,
                        { working = if (interval) working.onIntervalStartDateChanged(it) else working.onTimeBlockStartDateChanged(it) })
                    TemporalPickerField(if (interval) "Window start" else "Start time", startTime, TemporalKind.TIME,
                        { working = if (interval) working.onIntervalWindowStartTimeChanged(it) else working.onTimeBlockStartTimeChanged(it) })
                    if (!interval) TemporalPickerField("End date", working.timeBlockEndDate, TemporalKind.DATE,
                        { working = working.onTimeBlockEndDateChanged(it) })
                    TemporalPickerField(if (interval) "Window end" else "End time", endTime, TemporalKind.TIME,
                        { working = if (interval) working.onIntervalWindowEndTimeChanged(it) else working.onTimeBlockEndTimeChanged(it) })
                    if (interval) OutlinedTextField(working.intervalEveryMinutes, { working = working.onIntervalEveryMinutesChanged(it) },
                        label = { Text("Repeat every (minutes)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (start != null && end != null && origin != null && end.isAfter(start) && endMinutes in 1..10080) {
                        val limit = if (interval) 1439 else (((endMinutes + 1439) / 1440) * 1440).toInt()
                        ScheduleRuler(ChronoUnit.MINUTES.between(origin, start).toInt(), endMinutes.toInt(), limit, true) { a, b ->
                            val first = origin.plusMinutes(a.toLong())
                            val last = origin.plusMinutes(b.toLong())
                            val format = DateTimeFormatter.ofPattern("HH:mm")
                            working = if (interval) working.copy(intervalWindowStartTime = first.format(format), intervalWindowEndTime = last.format(format))
                            else working.copy(timeBlockStartDate = first.toLocalDate().toString(), timeBlockEndDate = last.toLocalDate().toString(),
                                timeBlockStartTime = first.format(format), timeBlockEndTime = last.format(format))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                DayLoopBottomActionBar(if (interval) "Save interval" else "Save block", { onDone(working) }, enabled = valid) {
                    DayLoopIcon(R.drawable.ic_action_save)
                }
            }
        }
    }
}
