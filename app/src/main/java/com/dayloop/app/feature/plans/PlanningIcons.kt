package com.dayloop.app.feature.plans

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.dayloop.app.R
import com.dayloop.app.ui.components.DayLoopIcon

internal enum class PlanningIcon(val label: String, val drawable: Int) {
    FLAG("Goal", R.drawable.ic_nav_goals),
    CALENDAR("Planning", R.drawable.ic_nav_plan),
    TIMER("Focus", R.drawable.ic_nav_focus),
    CHECKLIST("Routine", R.drawable.ic_nav_today),
    CHECK("Milestone", R.drawable.ic_action_check),
    PLAY("Activity", R.drawable.ic_action_play),
    EDIT("Writing", R.drawable.ic_action_edit),
    SAVE("Files", R.drawable.ic_action_save),
    BRIEFCASE("Career", R.drawable.ic_nav_plan),
    HEART("Wellness", R.drawable.ic_nav_focus),
    BOOK("Education", R.drawable.ic_action_edit),
    PALETTE("Creative", R.drawable.ic_action_play),
    WALLET("Finance", R.drawable.ic_action_save),
    HOME("Home", R.drawable.ic_nav_today),
    CODE("Digital", R.drawable.ic_nav_more),
    PEOPLE("Social", R.drawable.ic_nav_goals),
    PLANE("Logistics", R.drawable.ic_action_forward),
    LIGHTBULB("Innovation", R.drawable.ic_action_menu);

    companion object {
        fun resolve(id: String?, fallback: PlanningIcon) = entries.firstOrNull { it.name == id } ?: fallback
    }
}

@Composable
internal fun PlanningIconPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val currentIcon = PlanningIcon.resolve(selected, PlanningIcon.FLAG)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Icon", style = MaterialTheme.typography.labelMedium)
        Surface(
            onClick = { if (enabled) showDialog = true },
            enabled = enabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DayLoopIcon(currentIcon.drawable, currentIcon.label)
                Text(currentIcon.label, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                DayLoopIcon(R.drawable.ic_action_forward)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose Icon") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlanningIcon.entries.forEach { icon ->
                        Surface(
                            onClick = {
                                onSelect(icon.name)
                                showDialog = false
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = if (icon.name == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DayLoopIcon(
                                    resource = icon.drawable,
                                    description = icon.label,
                                    tint = if (icon.name == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = icon.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (icon.name == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
