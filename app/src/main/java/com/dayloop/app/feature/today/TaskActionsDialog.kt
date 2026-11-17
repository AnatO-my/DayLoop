package com.dayloop.app.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.ui.components.DayLoopIcon

@Composable
fun TaskActionsDialog(task: Task, menu: TaskActionMenu, busy: Boolean, canSnooze: Boolean,
    onDismiss: () -> Unit, onEdit: () -> Unit, onComplete: () -> Unit, onReopen: () -> Unit,
    onSnooze: () -> Unit, onDelete: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(task.title) }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (menu == TaskActionMenu.ALL) {
                TaskAction(R.drawable.ic_action_edit, "Edit task", !busy, onEdit)
                if (task.status == TaskStatus.ACTIVE) {
                    TaskAction(R.drawable.ic_action_check, "Complete", !busy, onComplete)
                    TaskAction(R.drawable.ic_action_close, "Skip once", !busy, onSkip)
                }
                else TaskAction(R.drawable.ic_action_reset, "Reopen task", !busy, onReopen)
            }
            if (task.status == TaskStatus.ACTIVE) {
                TaskAction(R.drawable.ic_nav_focus, "Snooze reminder 10 minutes", !busy && canSnooze, onSnooze)
                TaskAction(R.drawable.ic_nav_plan, "Reschedule", !busy && task.reminderPolicy.rescheduleAllowed, onEdit)
                if (!canSnooze) Text(if (task.reminderPolicy.snoozeAllowed) "No active reminder to snooze."
                    else "Snoozing is disabled for this task.", style = MaterialTheme.typography.labelMedium)
            }
            if (menu == TaskActionMenu.ALL && task.provenance == null) {
                TaskAction(R.drawable.ic_action_delete, "Delete task", !busy, onDelete)
            }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun TaskAction(icon: Int, title: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        DayLoopIcon(icon)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f))
    }
}
