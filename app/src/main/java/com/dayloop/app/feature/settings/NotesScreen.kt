package com.dayloop.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.settings.Note
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun NotesScreen(
    notes: List<Note>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens
    var newNoteContent by remember { mutableStateOf("") }
    
    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(R.drawable.ic_action_back, "Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text("Notes", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            
            Spacer(Modifier.height(tokens.spacing.contentGap))
            
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newNoteContent,
                    onValueChange = { newNoteContent = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Write a note...") },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (newNoteContent.isNotBlank()) {
                            onAddNote(newNoteContent)
                            newNoteContent = ""
                        }
                    },
                    enabled = newNoteContent.isNotBlank(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Add")
                }
            }
            
            Spacer(Modifier.height(tokens.spacing.contentGap))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap)
            ) {
                if (notes.isEmpty()) {
                    item {
                        Text("No notes yet.", color = tokens.colors.supportingText, modifier = Modifier.padding(16.dp))
                    }
                }
                items(notes.reversed(), key = { it.id }) { note ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.content, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            DayLoopIconButton(resource = R.drawable.ic_action_delete, label = "Delete note", onClick = {
                                onDeleteNote(note.id)
                            })
                        }
                    }
                }
            }
        }
    }
}
