package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun QuickAddBar(
    draft: QuickAddDraft,
    onDraftChange: (QuickAddDraft) -> Unit,
    onTaskCreated: (Task, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    createTaskId: () -> TaskId = { TaskId.generate() },
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.relatedGap),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { title -> onDraftChange(draft.onTitleChanged(title)) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Quick add task title" },
                    label = { Text(text = "New task") },
                    isError = draft.errorMessage != null,
                    supportingText = draft.errorMessage?.let { message ->
                        { Text(text = message) }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
                )
                Button(
                    onClick = {
                        when (val result = draft.submit(createTaskId = createTaskId)) {
                            is QuickAddSubmitResult.Invalid -> onDraftChange(result.draft)
                            is QuickAddSubmitResult.Submitted -> {
                                onTaskCreated(result.task, result.startFocus)
                                onDraftChange(result.nextDraft)
                            }
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Add task" },
                    shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
                ) {
                    Text(text = "Add")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickAddScheduleMode.entries.forEach { mode ->
                    val selected = draft.scheduleMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onDraftChange(draft.onScheduleModeChanged(mode)) },
                        label = { Text(text = mode.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.semantics { contentDescription = "Schedule mode ${mode.label}" },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.colors.accent,
                            selectedLabelColor = tokens.colors.onAccent,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddBarPreview() {
    DayLoopTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dayLoopTokens.spacing.pageMargin)) {
            QuickAddBar(
                draft = QuickAddDraft(),
                onDraftChange = {},
                onTaskCreated = { _, _ -> },
            )
        }
    }
}
