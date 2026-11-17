package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.R
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.outfit.recognition.GarmentDraft
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Edit the saved details of an existing item. Works fully offline. */
@Composable
fun GarmentDetailsDialog(item: ClothingItem, onDismiss: () -> Unit) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    var draft by remember(item.id) { mutableStateOf<GarmentDraft?>(null) }
    var recognition by remember(item.id) { mutableStateOf<com.dayloop.app.core.model.RecognitionStamp?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        try {
            val attributes = graph.wardrobeAttributesRepository.get(item.id)
            recognition = attributes?.recognition
            draft = GarmentDraft.from(item, attributes)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Could not load this item's details."
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = tokens.spacing.pageMargin)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DayLoopIconButton(R.drawable.ic_action_back, "Close item details", onDismiss, enabled = !busy)
                    Text("Item details", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                }
                val current = draft
                if (current == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (error == null) CircularProgressIndicator() else Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        GarmentDraftEditor(current, { draft = it }, enabled = !busy)
                        Spacer(Modifier.height(8.dp))
                    }
                    (error ?: current.blockingProblem)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = if (error != null) MaterialTheme.colorScheme.error else colors.secondaryText,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DayLoopOutlinedButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Cancel") }
                        DayLoopButton(
                            enabled = current.blockingProblem == null && !busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        graph.wardrobeAttributesRepository.saveItem(
                                            current.toItem(item.id, item.isArchived), current.toAttributes(recognition),
                                        )
                                        onDismiss()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        error = "Could not save changes. Try again."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}
