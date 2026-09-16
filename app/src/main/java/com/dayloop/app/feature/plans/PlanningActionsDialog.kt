package com.dayloop.app.feature.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun PlanningActionsDialog(title: String, isGoal: Boolean, archived: Boolean, busy: Boolean,
    onDismiss: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    val kind = if (isGoal) "goal" else "project"
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (deleting) "Delete $kind?" else title) },
        text = {
            if (deleting) Text(
                if (isGoal) "Delete \"$title\" permanently? Linked projects and their tasks will be kept, without this goal link."
                else "Delete \"$title\" permanently? Saved tasks and their reminders will be kept, without this project link. Its cycle will stop generating new tasks."
            ) else Column {
                TextButton(enabled = !busy, onClick = onArchive) { Text(if (archived) "Reactivate" else "Archive") }
                TextButton(enabled = !busy, onClick = { deleting = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                Text(if (isGoal) "Archiving keeps linked projects and tasks unchanged."
                    else "Archiving stops new cycle tasks and keeps saved tasks and reminders.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (deleting) TextButton(enabled = !busy, onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(if (busy) "Deleting..." else "Delete")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { if (deleting) deleting = false else onDismiss() }) {
            Text(if (deleting) "Cancel" else "Close")
        } },
    )
}
