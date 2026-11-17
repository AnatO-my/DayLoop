package com.dayloop.app.feature.today

import com.dayloop.app.ui.components.DayLoopOutlinedButton
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.notifications.ReminderCapabilities
import com.dayloop.app.settings.HomePresentation
import com.dayloop.app.ui.components.DayLoopMainNav
import com.dayloop.app.ui.components.DayLoopPageHeader
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

/** Alternate Today arrangements; both deliberately emit the same task/navigation events as Editorial. */
@Composable
internal fun TodayAlternatePresentation(
    presentation: HomePresentation,
    uiState: TodayUiState,
    actionsEnabled: Boolean,
    onCompleteTask: (TaskId) -> Unit,
    onEditTask: (TaskId) -> Unit,
    onAddTask: () -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    onReviewMissed: () -> Unit,
    reminderCapabilities: ReminderCapabilities?,
    onFixReminderCapability: (String) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    Scaffold(
        topBar = {
            Surface(color = tokens.colors.background) {
                Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = tokens.spacing.pageMargin, vertical = 8.dp)) {
                    DayLoopPageHeader(if (presentation == HomePresentation.SIGNAL_GRID) "Signals" else "Cadence", { onBottomNavItemSelected("more") })
                }
            }
        },
        bottomBar = { DayLoopMainNav("today", onBottomNavItemSelected, onAddTask, actionsEnabled) },
    ) { padding ->
        DayLoopScreen(modifier = Modifier.padding(padding), contentPadding = PaddingValues(tokens.spacing.pageMargin)) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)) {
                if (reminderCapabilities?.notificationsAllowed == false) item {
                    OutlinedButton(onClick = { onFixReminderCapability("notifications") }, enabled = actionsEnabled, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable reminders")
                    }
                } else if (reminderCapabilities?.exactAlarmsAllowed == false) item {
                    OutlinedButton(onClick = { onFixReminderCapability("exact_alarms") }, enabled = actionsEnabled, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow alarms and reminders")
                    }
                }
                item {
                    if (presentation == HomePresentation.SIGNAL_GRID) SignalGrid(uiState)
                    else WeekLedger(uiState)
                }
                if (uiState.hasMissedTasks) item {
                    DayLoopOutlinedButton(onClick = onReviewMissed, enabled = actionsEnabled, modifier = Modifier.fillMaxWidth()) {
                        Text("Review ${uiState.missedCount} unfinished task${if (uiState.missedCount == 1) "" else "s"}")
                    }
                }
                uiState.visibleSections.forEach { section ->
                    item { Text(section.title.uppercase(), style = MaterialTheme.typography.labelMedium) }
                    items(section.items, key = { it.taskId.value }) { task ->
                        OutlinedTaskRow(task, actionsEnabled, onCompleteTask, onEditTask)
                    }
                }
                if (uiState.isEmpty) item { Text("No tasks planned. Capture one when you are ready.", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun SignalGrid(state: TodayUiState) {
    val tokens = MaterialTheme.dayLoopTokens
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TODAY / ${state.dateLabel.uppercase()}", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SignalCell("OPEN", state.remainingCount.toString(), "tasks", Modifier.weight(1f))
            SignalCell("FOCUS", state.focusTimeValue, "today", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SignalCell("DONE", state.progressPercentLabel, state.progressSummary, Modifier.weight(1f))
            SignalCell("NEXT", state.visibleSections.firstOrNull()?.title?.uppercase() ?: "CLEAR", "queue", Modifier.weight(1f))
        }
        Text("Task signals only — no health or biometric data.", style = MaterialTheme.typography.bodySmall, color = tokens.colors.secondaryText)
    }
}

@Composable
private fun SignalCell(label: String, value: String, detail: String, modifier: Modifier) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(modifier, border = BorderStroke(1.dp, tokens.colors.decorativeBorder), color = tokens.colors.background) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = tokens.colors.secondaryText)
        }
    }
}

@Composable
private fun WeekLedger(state: TodayUiState) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(border = BorderStroke(1.dp, tokens.colors.decorativeBorder), color = tokens.colors.background) {
        Column {
            Text("THIS WEEK", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(12.dp))
            listOf("TODAY" to state.remainingLabel, "NEXT" to state.visibleSections.firstOrNull()?.title.orEmpty(), "DONE" to state.progressSummary).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = tokens.colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun OutlinedTaskRow(
    task: TodayTaskUiState,
    enabled: Boolean,
    onComplete: (TaskId) -> Unit,
    onEdit: (TaskId) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onEdit(task.taskId) },
        border = BorderStroke(1.dp, tokens.colors.decorativeBorder),
        color = tokens.colors.background,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onComplete(task.taskId) }, enabled = enabled, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) { Text("✓") }
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                task.projectTitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = tokens.colors.secondaryText) }
            }
            Text("EDIT", style = MaterialTheme.typography.labelSmall)
        }
    }
}
