package com.dayloop.app.feature.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.Note
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens
import java.util.UUID

@Composable
fun NoteEditorScreen(
    initialNote: Note? = null,
    onSaveNote: (Note) -> Unit,
    onConvertToTask: (title: String, description: String) -> Unit,
    onDeleteNote: ((String) -> Unit)? = null,
    onBack: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var tagInput by remember { mutableStateOf(initialNote?.tags?.joinToString(", ") ?: "") }

    DayLoopScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DayLoopIconButton(R.drawable.ic_action_back, "Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (initialNote == null) "New Note" else "Edit Note",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Pin toggle button
                DayLoopIconButton(
                    resource = if (isPinned) R.drawable.ic_action_sparkle else R.drawable.ic_action_edit,
                    label = if (isPinned) "Unpin Note" else "Pin Note",
                    onClick = { isPinned = !isPinned }
                )

                if (initialNote != null && onDeleteNote != null) {
                    DayLoopIconButton(
                        resource = R.drawable.ic_action_delete,
                        label = "Delete Note",
                        onClick = {
                            onDeleteNote(initialNote.id)
                            onBack()
                        }
                    )
                }
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Note Title") },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
            )

            Spacer(Modifier.height(tokens.spacing.itemGap))

            // Tags Input
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tags (comma separated, e.g. Work, Ideas)") },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
            )

            Spacer(Modifier.height(tokens.spacing.itemGap))

            // Note Content Editor Field
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Write your thoughts or plans here...") },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
            )

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Convert to Task button
                DayLoopOutlinedButton(
                    onClick = {
                        val taskTitle = title.ifBlank { "Task from Note" }
                        onConvertToTask(taskTitle, content)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() || content.isNotBlank()
                ) {
                    Text("Convert to Task")
                }

                // Save Note button
                DayLoopButton(
                    onClick = {
                        val parsedTags = tagInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        val noteToSave = Note(
                            id = initialNote?.id ?: UUID.randomUUID().toString(),
                            title = title.ifBlank { "Untitled Note" },
                            content = content,
                            isPinned = isPinned,
                            isArchived = initialNote?.isArchived ?: false,
                            linkedTaskId = initialNote?.linkedTaskId,
                            linkedProjectId = initialNote?.linkedProjectId,
                            createdAt = initialNote?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            tags = parsedTags
                        )
                        onSaveNote(noteToSave)
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() || content.isNotBlank()
                ) {
                    Text("Save Note")
                }
            }
        }
    }
}
