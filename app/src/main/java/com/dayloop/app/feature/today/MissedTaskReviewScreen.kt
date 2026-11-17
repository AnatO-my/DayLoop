package com.dayloop.app.feature.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayItem
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens
import java.time.format.DateTimeFormatter

@Composable
internal fun MissedTaskReviewScreen(
    missed: List<TodayItem>,
    onKeepForToday: (TaskId) -> Unit,
    onReschedule: (TaskId) -> Unit,
    onSomeday: (TaskId) -> Unit,
    onDismiss: (TaskId) -> Unit,
    onBack: () -> Unit,
    busy: Boolean = false,
) {
    ReviewLayout("Missed work", onBack, busy) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (missed.isEmpty()) item { Text("No missed work to review.") }
            items(missed, key = { it.taskId.value }) { item ->
                ReviewTaskCard(item.task) {
                    ReviewActions(busy,
                        "Keep for today" to { onKeepForToday(item.taskId) },
                        "Reschedule" to { onReschedule(item.taskId) },
                        "Someday" to { onSomeday(item.taskId) },
                        "Dismiss" to { onDismiss(item.taskId) })
                }
            }
        }
    }
}

@Composable
internal fun SavedWorkScreen(
    tasks: List<Task>, busy: Boolean,
    onRestore: (TaskId) -> Unit, onReschedule: (TaskId) -> Unit, onBack: () -> Unit,
) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    val status = if (dismissed) TaskStatus.SKIPPED else TaskStatus.SOMEDAY
    val visible = tasks.filter { it.status == status }
    ReviewLayout("Saved work", onBack, busy) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.dayLoopTokens.spacing.pageMargin),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !dismissed, onClick = { dismissed = false }, enabled = !busy,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.dayLoopTokens.colors.accent,
                    selectedLabelColor = MaterialTheme.dayLoopTokens.colors.onAccent
                ),
                label = { Text("Someday (${tasks.count { it.status == TaskStatus.SOMEDAY }})") })
            FilterChip(selected = dismissed, onClick = { dismissed = true }, enabled = !busy,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.dayLoopTokens.colors.accent,
                    selectedLabelColor = MaterialTheme.dayLoopTokens.colors.onAccent
                ),
                label = { Text("Dismissed (${tasks.count { it.status == TaskStatus.SKIPPED }})") })
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (visible.isEmpty()) item { Text(if (dismissed) "No dismissed tasks." else "No someday tasks.") }
            items(visible, key = { it.id.value }) { task ->
                ReviewTaskCard(task) {
                    ReviewActions(busy,
                        "Bring to today" to { onRestore(task.id) },
                        "Reschedule" to { onReschedule(task.id) })
                }
            }
        }
    }
}

@Composable
private fun ReviewLayout(
    title: String, onBack: () -> Unit, busy: Boolean, content: @Composable ColumnScope.() -> Unit,
) {
    DayLoopScreen(modifier = Modifier.safeDrawingPadding(), contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(resource = R.drawable.ic_action_back, label = "Back", onClick = onBack, enabled = !busy)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            content()
        }
    }
}

@Composable
private fun ReviewTaskCard(task: Task, actions: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.cardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            task.schedule.reviewLabel()?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions()
        }
    }
}

@Composable
private fun ReviewActions(busy: Boolean, vararg actions: Pair<String, () -> Unit>) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { (label, action) ->
            TextButton(onClick = action, enabled = !busy) { Text(label) }
        }
    }
}

private fun TaskSchedule?.reviewLabel(): String? = when (this) {
    is TaskSchedule.AllDay -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    is TaskSchedule.Deadline -> "Due ${dueAt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}"
    is TaskSchedule.TimeBlock -> "Ended ${endsAt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}"
    else -> null
}
