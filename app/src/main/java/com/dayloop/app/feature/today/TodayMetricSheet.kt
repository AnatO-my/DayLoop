package com.dayloop.app.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.ui.theme.dayLoopTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayMetricSheet(
    state: TodayUiState,
    completed: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onEdit: (TaskId) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val sections = if (completed) listOf(state.completedSection).filterNot { it.isEmpty }
        else state.visibleSections
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = tokens.spacing.pageMargin, end = tokens.spacing.pageMargin, bottom = tokens.spacing.extraGap),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)) {
            item {
                Text(if (completed) "Completed today" else "Remaining today",
                    style = MaterialTheme.typography.headlineSmall)
            }
            if (sections.isEmpty()) {
                item { Text(if (completed) "No completed tasks yet." else "No tasks remaining today.") }
            }
            sections.forEach { section ->
                item { Text("${section.title} (${section.count})", style = MaterialTheme.typography.titleMedium) }
                items(section.items) { task ->
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.shapeScale.cardCorner), color = tokens.colors.raised) {
                        Column(Modifier.fillMaxWidth()
                            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Open task",
                                onClick = { onEdit(task.taskId) })
                            .padding(tokens.spacing.cardPadding)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge)
                            task.projectTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            item { TextButton(onClick = onDismiss) { Text("Close") } }
        }
    }
}
