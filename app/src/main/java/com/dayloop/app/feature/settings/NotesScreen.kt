package com.dayloop.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.Note
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    onOpenNoteEditor: (Note?) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePinNote: (Note) -> Unit,
    onBack: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    // Collect all available tags across notes
    val allTags = remember(notes) {
        notes.flatMap { it.tags }.distinct().sorted()
    }

    // Filter notes by search query and selected tag
    val filteredNotes = remember(notes, searchQuery, selectedTag) {
        notes.filter { note ->
            val matchesQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesTag = selectedTag == null || note.tags.contains(selectedTag)

            matchesQuery && matchesTag
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val unpinnedNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            // Header Bar
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(R.drawable.ic_action_back, "Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Notes Hub",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                DayLoopIconButton(
                    resource = R.drawable.ic_action_add,
                    label = "Create Note",
                    onClick = { onOpenNoteEditor(null) }
                )
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes or tags...") },
                singleLine = true,
                shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
            )

            // Tags Row Filter
            if (allTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("All") }
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)
            ) {
                if (filteredNotes.isEmpty()) {
                    item {
                        Text(
                            text = if (searchQuery.isBlank()) "No notes yet. Tap + to add one!" else "No matching notes found.",
                            color = tokens.colors.mutedText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Pinned Notes Section
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "PINNED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNoteEditor(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onTogglePin = { onTogglePinNote(note) }
                        )
                    }
                }

                // All/Unpinned Notes Section
                if (unpinnedNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "OTHERS",
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.colors.mutedText,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    items(unpinnedNotes, key = { note -> note.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNoteEditor(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onTogglePin = { onTogglePinNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                DayLoopIconButton(
                    resource = if (note.isPinned) R.drawable.ic_action_sparkle else R.drawable.ic_action_edit,
                    label = if (note.isPinned) "Unpin" else "Pin",
                    onClick = onTogglePin
                )

                DayLoopIconButton(
                    resource = R.drawable.ic_action_delete,
                    label = "Delete note",
                    onClick = onDelete
                )
            }

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.mutedText
                )

                if (note.tags.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = note.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
