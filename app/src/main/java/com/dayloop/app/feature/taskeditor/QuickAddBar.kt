package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun QuickAddBar(
    draft: QuickAddDraft,
    onDraftChange: (QuickAddDraft) -> Unit,
    onTaskCreated: (Task) -> Unit,
    modifier: Modifier = Modifier,
    createTaskId: () -> TaskId = { TaskId.generate() },
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.contentGap),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
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
            )
            Button(
                onClick = {
                    when (val result = draft.submit(createTaskId = createTaskId)) {
                        is QuickAddSubmitResult.Invalid -> onDraftChange(result.draft)
                        is QuickAddSubmitResult.Submitted -> {
                            onTaskCreated(result.task)
                            onDraftChange(result.nextDraft)
                        }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Add task" },
            ) {
                Text(text = "Add")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddBarPreview() {
    DayLoopTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dayLoopTokens.spacing.screenPadding)) {
            QuickAddBar(
                draft = QuickAddDraft(),
                onDraftChange = {},
                onTaskCreated = {},
            )
        }
    }
}
