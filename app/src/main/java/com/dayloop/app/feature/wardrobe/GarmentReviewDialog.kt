package com.dayloop.app.feature.wardrobe

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.outfit.recognition.GarmentDraft
import com.dayloop.app.core.outfit.recognition.RecognitionResult
import com.dayloop.app.core.outfit.recognition.ReviewEntry
import com.dayloop.app.core.outfit.recognition.ReviewSession
import com.dayloop.app.core.outfit.recognition.ReviewSummary
import com.dayloop.app.core.outfit.recognition.WardrobeImageException
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface ReviewState {
    data object Loading : ReviewState
    data class Failed(val message: String, val canEnterManually: Boolean) : ReviewState

    /** [result] is null when the user is typing the details without recognition. */
    data class Ready(val result: RecognitionResult?, val aspect: Float, val severalItems: Boolean) : ReviewState
}

/** Which part of the review is on screen. */
private enum class ReviewPane { OVERVIEW, QUICK_CONFIRM, FORM }

/**
 * Photo -> on-device recognition -> review -> save. Nothing reaches the wardrobe until the
 * user presses a save button.
 *
 * - One confident item: a short "Navy jeans?" confirmation, with the full form one tap away.
 * - One unsure item: the full form, with "couldn't tell" where the recogniser had no answer.
 * - Several items: the photo with numbered outlines and a tick list; each item opens the
 *   form, and "Add N items" saves the ticked, complete ones together.
 */
@Composable
fun GarmentReviewDialog(photo: Uri, onFinished: (savedCount: Int) -> Unit, onCancel: () -> Unit) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    var state by remember(photo) { mutableStateOf<ReviewState>(ReviewState.Loading) }
    var session by remember(photo) { mutableStateOf(ReviewSession(emptyList())) }
    val cutouts = remember(photo) { mutableStateMapOf<Int, Bitmap>() }
    var pane by remember(photo) { mutableStateOf(ReviewPane.FORM) }
    var activeKey by remember(photo) { mutableIntStateOf(0) }
    /** The working copy in the form; written back to the session on Done or Save. */
    var formDraft by remember(photo) { mutableStateOf(GarmentDraft()) }
    var savedCount by remember(photo) { mutableIntStateOf(0) }
    var drawing by remember(photo) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun openForm(key: Int) {
        val entry = session.entry(key) ?: return
        activeKey = key
        formDraft = entry.draft
        error = null
        pane = ReviewPane.FORM
    }

    fun close() = if (savedCount > 0) onFinished(savedCount) else onCancel()

    LaunchedEffect(photo) {
        state = try {
            val recognized = graph.garmentRecognizer.recognize(photo)
            var loaded = ReviewSession(emptyList())
            recognized.garments.forEach { garment ->
                loaded = loaded.add(garment.prediction, drawnByUser = false)
                garment.cutout?.let { cutouts[loaded.entries.last().key] = it }
            }
            session = loaded
            val several = loaded.entries.size > 1
            val first = loaded.entries.firstOrNull()
            if (several || first == null) {
                pane = ReviewPane.OVERVIEW
            } else {
                activeKey = first.key
                formDraft = first.draft
                pane = if (ReviewSummary.quickConfirmAvailable(first.prediction, first.draft)) ReviewPane.QUICK_CONFIRM else ReviewPane.FORM
            }
            ReviewState.Ready(
                recognized.result,
                aspect = recognized.imageWidth.toFloat() / recognized.imageHeight.coerceAtLeast(1),
                severalItems = several,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (image: WardrobeImageException) {
            ReviewState.Failed(image.userMessage, canEnterManually = false)
        } catch (_: Exception) {
            ReviewState.Failed("Recognition did not work this time. You can still enter the details yourself.", canEnterManually = true)
        }
    }

    /** Saves one item with its details, then its picture. A picture failure never loses the item. */
    suspend fun save(entry: ReviewEntry, draft: GarmentDraft, result: RecognitionResult?, severalItems: Boolean) {
        val item = draft.toItem(ClothingItemId.generate())
        val stamp = entry.prediction?.let { prediction -> result?.let { GarmentDraft.stamp(prediction, draft, it.models) } }
        graph.wardrobeAttributesRepository.saveItem(item, draft.toAttributes(stamp))
        savedCount += 1
        val cutout = cutouts[entry.key]
        val mediaId = when {
            cutout != null -> graph.wardrobeMediaRepository.importBitmap(cutout)
            // With several items in one photo the whole picture would be wrong for each of them.
            severalItems -> null
            else -> graph.wardrobeMediaRepository.importImage(photo)
        }
        if (mediaId != null) graph.wardrobeMediaRepository.linkMediaToItem(item.id, mediaId, isPrimary = true)
    }

    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = tokens.spacing.pageMargin)) {
                val ready = state as? ReviewState.Ready
                val inItemOfSeveral = ready?.severalItems == true && pane == ReviewPane.FORM
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DayLoopIconButton(
                        R.drawable.ic_action_back,
                        if (inItemOfSeveral) "Back to photo" else "Cancel adding item",
                        { if (inItemOfSeveral) { error = null; pane = ReviewPane.OVERVIEW } else close() },
                        enabled = !busy,
                    )
                    Text(
                        when {
                            ready != null && pane == ReviewPane.OVERVIEW ->
                                if (session.entries.size == 1) "1 item found" else "${session.entries.size} items found"
                            else -> "Review item"
                        },
                        Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                    )
                }
                when (val current = state) {
                    ReviewState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Looking at your photo on this device…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is ReviewState.Failed -> Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                        Text(current.message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Spacer(Modifier.height(16.dp))
                        if (current.canEnterManually) {
                            DayLoopButton(
                                onClick = {
                                    session = ReviewSession(emptyList()).add(null, drawnByUser = false)
                                    activeKey = session.entries.first().key
                                    formDraft = GarmentDraft()
                                    pane = ReviewPane.FORM
                                    state = ReviewState.Ready(result = null, aspect = 1f, severalItems = false)
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text("Enter details myself") }
                        }
                        DayLoopOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Choose another photo") }
                    }

                    is ReviewState.Ready -> {
                        val entry = session.entry(activeKey)
                        when {
                            pane == ReviewPane.OVERVIEW -> {
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GarmentPhotoOverview(
                                        photo = photo,
                                        aspect = current.aspect,
                                        session = session,
                                        drawing = drawing,
                                        enabled = !busy,
                                        onToggleIncluded = { key, included -> session = session.setIncluded(key, included) },
                                        onReview = { key -> if (!drawing) openForm(key) },
                                        onToggleDrawing = { drawing = !drawing; error = null },
                                        onDrawn = { box ->
                                            busy = true
                                            scope.launch {
                                                try {
                                                    val found = graph.garmentRecognizer.recognize(photo, box).garments.first()
                                                    session = session.add(found.prediction, drawnByUser = true)
                                                    val key = session.entries.last().key
                                                    found.cutout?.let { cutouts[key] = it }
                                                    drawing = false
                                                    openForm(key)
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (_: Exception) {
                                                    error = "That part of the photo could not be read. Try a larger box."
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        },
                                    )
                                    Text(
                                        "Recognised on this device. Your photo is not uploaded.",
                                        style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
                                    )
                                }
                                val waiting = session.tickedButIncomplete.size
                                val note = error ?: when (waiting) {
                                    0 -> null
                                    1 -> "1 ticked item still needs details and will not be added yet."
                                    else -> "$waiting ticked items still need details and will not be added yet."
                                }
                                note?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = if (error != null) MaterialTheme.colorScheme.error else colors.secondaryText,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                }
                                val count = session.toSave.size
                                DayLoopButton(
                                    enabled = count > 0 && !busy,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 48.dp),
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            val saved = mutableListOf<Int>()
                                            try {
                                                for (item in session.toSave) {
                                                    save(item, item.draft, current.result, severalItems = true)
                                                    saved += item.key
                                                }
                                                onFinished(savedCount)
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                // Keep what was saved out of the list so a retry cannot add it twice.
                                                session = session.remove(saved)
                                                error = "Some items could not be saved. Try again."
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    },
                                ) { Text(if (count == 1) "Add 1 item" else "Add $count items") }
                            }

                            entry == null -> Box(Modifier.weight(1f))

                            pane == ReviewPane.QUICK_CONFIRM && entry.prediction != null -> GarmentQuickConfirm(
                                image = cutouts[entry.key] ?: photo,
                                headline = ReviewSummary.headline(entry.draft),
                                rows = ReviewSummary.rows(entry.prediction),
                                busy = busy,
                                error = error,
                                onEdit = { openForm(entry.key) },
                                onStartBlank = {
                                    // Forget the guess entirely so no hint from the wrong item lingers.
                                    session = ReviewSession(session.entries.map {
                                        if (it.key == entry.key) it.copy(prediction = null, draft = GarmentDraft(), included = false) else it
                                    })
                                    openForm(entry.key)
                                },
                                onSave = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            save(entry, entry.draft, current.result, severalItems = false)
                                            onFinished(savedCount)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            error = "Could not save the item. Try again."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                            )

                            else -> {
                                val notes = (current.result?.notes.orEmpty() + entry.prediction?.notes.orEmpty()).toList()
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    WardrobeImageFrame(
                                        model = cutouts[entry.key] ?: photo,
                                        contentDescription = "Photo of the item being added",
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                    )
                                    Text(
                                        if (entry.prediction != null) "Recognised on this device. Your photo is not uploaded. These are suggestions: check and correct them before saving."
                                        else "Enter the details yourself. Nothing is uploaded.",
                                        style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
                                    )
                                    if (entry.prediction != null) notes.forEach { note ->
                                        Text("• ${note.message}", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                                    }
                                    GarmentDraftEditor(formDraft, { formDraft = it }, enabled = !busy, prediction = entry.prediction)
                                    Spacer(Modifier.height(8.dp))
                                }
                                val problem = formDraft.blockingProblem
                                (error ?: problem)?.let {
                                    Text(
                                        it, style = MaterialTheme.typography.bodySmall,
                                        color = if (error != null) MaterialTheme.colorScheme.error else colors.secondaryText,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (current.severalItems) {
                                        DayLoopOutlinedButton(
                                            onClick = { error = null; pane = ReviewPane.OVERVIEW },
                                            enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        ) { Text("Discard changes") }
                                        // "Done" keeps the edits; saving happens for all ticked items on the photo screen.
                                        DayLoopButton(
                                            onClick = { session = session.update(entry.key, formDraft); error = null; pane = ReviewPane.OVERVIEW },
                                            enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        ) { Text("Done") }
                                    } else {
                                        DayLoopOutlinedButton(onClick = { close() }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Cancel") }
                                        DayLoopButton(
                                            enabled = problem == null && !busy,
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                            onClick = {
                                                busy = true
                                                scope.launch {
                                                    try {
                                                        save(entry, formDraft, current.result, severalItems = false)
                                                        onFinished(savedCount)
                                                    } catch (cancelled: CancellationException) {
                                                        throw cancelled
                                                    } catch (_: Exception) {
                                                        error = "Could not save the item. Try again."
                                                    } finally {
                                                        busy = false
                                                    }
                                                }
                                            },
                                        ) { Text("Save to wardrobe") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
