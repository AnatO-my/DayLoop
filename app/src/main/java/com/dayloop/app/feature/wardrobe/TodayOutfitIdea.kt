package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.outfit.styling.FeedbackReason
import com.dayloop.app.core.outfit.styling.FeedbackVerdict
import com.dayloop.app.core.outfit.styling.OutfitFeedback
import com.dayloop.app.core.outfit.styling.OutfitFeedbackPolicy
import com.dayloop.app.core.outfit.styling.OutfitRecommendation
import com.dayloop.app.core.outfit.styling.OutfitRecommender
import com.dayloop.app.core.outfit.styling.StylingRequest
import com.dayloop.app.core.outfit.styling.StylingResult
import com.dayloop.app.core.outfit.styling.WardrobeEntry
import com.dayloop.app.core.outfit.styling.Weather
import com.dayloop.app.di.AppGraph
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Holds Today's outfit idea: the current suggestion, the quick context the user picked,
 * and the actions on it. A suggestion is never recorded as worn until [wearCurrent].
 */
@Stable
class TodayOutfitIdeaState internal constructor(
    private val date: LocalDate,
    private val graph: AppGraph,
    private val scope: CoroutineScope,
) {
    /** True once the user asked for an idea; false after wearing or dismissing it. */
    var active by mutableStateOf(false)
        private set
    var occasion by mutableStateOf<Occasion?>(null)
    var weather by mutableStateOf<Weather?>(null)
    var rainExpected by mutableStateOf(false)
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    internal var result by mutableStateOf<StylingResult?>(null)
    private var index by mutableIntStateOf(0)

    val optionCount: Int get() = result?.outfits?.size ?: 0

    /** The idea on screen, or null when none is being shown. */
    val current: OutfitRecommendation?
        get() = if (!active) null else result?.outfits?.takeIf { it.isNotEmpty() }?.let { it[index % it.size] }

    fun start() {
        index = 0
        message = null
        active = true
    }

    fun next() {
        if (optionCount > 0) index = (index + 1) % optionCount
    }

    fun cancel() {
        active = false
        index = 0
    }

    /** Records the idea as today's outfit, replacing nothing silently: Today only offers it when no outfit is recorded. */
    fun wearCurrent() {
        val outfit = current ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                graph.outfitRepository.recordOutfit(date, outfit.pieces.map { it.item.id.value })
                runCatching { graph.outfitFeedbackRepository.record(OutfitFeedback(outfit.itemIds, FeedbackVerdict.WORN, null, date)) }
                cancel()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Could not record the outfit. Try again."
            } finally {
                busy = false
            }
        }
    }

    /** Remembers that this combination is not wanted; it then drops out of the suggestions. */
    fun reject(reason: FeedbackReason) {
        val outfit = current ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                graph.outfitFeedbackRepository.record(OutfitFeedback(outfit.itemIds, FeedbackVerdict.NOT_FOR_ME, reason, date))
                message = "Thanks. That combination won't be suggested for a while."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Could not save that. Try again."
            } finally {
                busy = false
            }
        }
    }
}

/**
 * Creates the Today idea state and keeps its suggestions current as the wardrobe, wear
 * history, feedback or chosen context change. Runs entirely on the device.
 */
@Composable
fun rememberTodayOutfitIdea(date: LocalDate): TodayOutfitIdeaState {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val state = remember(date) { TodayOutfitIdeaState(date, graph, scope) }
    val items by remember { graph.outfitRepository.observeActiveWardrobe() }.collectAsState(initial = null)
    val attributes by remember { graph.wardrobeAttributesRepository.observeAll() }.collectAsState(initial = null)
    val history by remember { graph.outfitRepository.observeAllOutfits() }.collectAsState(initial = null)
    val feedback by remember(date) { graph.outfitFeedbackRepository.observeRecent(date) }.collectAsState(initial = null)

    LaunchedEffect(state, items, attributes, history, feedback, state.occasion, state.weather, state.rainExpected) {
        val wardrobe = items ?: return@LaunchedEffect
        val details = attributes ?: return@LaunchedEffect
        val worn = history ?: return@LaunchedEffect
        val given = feedback ?: return@LaunchedEffect
        val request = StylingRequest(
            today = date,
            occasion = state.occasion,
            weather = state.weather,
            rainExpected = state.rainExpected,
            maxResults = 5,
            rejectedCombinations = OutfitFeedbackPolicy.rejectedCombinations(given, date),
        )
        state.result = withContext(Dispatchers.Default) {
            val lastWorn = HashMap<ClothingItemId, LocalDate>()
            for (record in worn) for (piece in record.items) {
                val previous = lastWorn[piece.id]
                if (previous == null || record.date.isAfter(previous)) lastWorn[piece.id] = record.date
            }
            OutfitRecommender.recommend(wardrobe.map { WardrobeEntry(it, details[it.id], lastWorn[it.id]) }, request)
        }
    }
    return state
}

/**
 * The body of Today's outfit card while an idea is showing: pictures, one sentence on why,
 * quick context, and the actions. Shown inside the existing card frame.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayOutfitIdeaContent(idea: TodayOutfitIdeaState, onWear: () -> Unit, onCancel: () -> Unit) {
    val outfit = idea.current ?: return
    val colors = MaterialTheme.dayLoopTokens.colors
    var showWhy by remember { mutableStateOf(false) }
    var showNotForMe by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Outfit idea", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text("A suggestion, not recorded", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            outfit.pieces.take(4).forEach { piece ->
                WardrobeThumbnail(piece.item.id, Modifier.weight(1f).aspectRatio(1f))
            }
        }
        Text(outfit.pieces.joinToString(", ") { it.item.name }, style = MaterialTheme.typography.bodyLarge)
        Text(outfit.headline, style = MaterialTheme.typography.bodyMedium)
        if (outfit.unmetConstraints.isNotEmpty()) {
            Text(outfit.unmetConstraints.first(), style = MaterialTheme.typography.bodySmall, color = colors.warning)
        }
        idea.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = idea.occasion == Occasion.CASUAL, enabled = !idea.busy,
                onClick = { idea.occasion = if (idea.occasion == Occasion.CASUAL) null else Occasion.CASUAL },
                label = { Text("Casual") },
            )
            FilterChip(
                selected = idea.occasion == Occasion.WORK, enabled = !idea.busy,
                onClick = { idea.occasion = if (idea.occasion == Occasion.WORK) null else Occasion.WORK },
                label = { Text("Work") },
            )
            FilterChip(
                selected = idea.weather == Weather.HOT, enabled = !idea.busy,
                onClick = { idea.weather = if (idea.weather == Weather.HOT) null else Weather.HOT },
                label = { Text("Hot") },
            )
            FilterChip(
                selected = idea.rainExpected, enabled = !idea.busy,
                onClick = { idea.rainExpected = !idea.rainExpected },
                label = { Text("Rain") },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayLoopButton(onClick = onWear, enabled = !idea.busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Wear this") }
            DayLoopOutlinedButton(onClick = { idea.next() }, enabled = !idea.busy && idea.optionCount > 1, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Next idea")
            }
            DayLoopOutlinedButton(onClick = { showWhy = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Why") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showNotForMe = true }, enabled = !idea.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Not for me: tell DayLoop why", color = colors.secondaryText)
            }
            TextButton(onClick = onCancel, enabled = !idea.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Dismiss", color = colors.secondaryText)
            }
        }
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("Why this outfit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guideline match ${outfit.matchScore} out of 100", style = MaterialTheme.typography.bodySmall)
                    WhyLines("Why this could work", outfit.reasons)
                    WhyLines("Does not meet your request", outfit.unmetConstraints)
                    WhyLines("Worth knowing", outfit.cautions)
                    Text(StylingResult.DISCLAIMER, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showWhy = false }) { Text("Close") } },
        )
    }

    if (showNotForMe) {
        var reason by remember { mutableStateOf<FeedbackReason?>(null) }
        AlertDialog(
            onDismissRequest = { showNotForMe = false },
            title = { Text("Not for me") },
            text = {
                Column {
                    Text("Stays on this phone. It stops this exact combination being suggested.", style = MaterialTheme.typography.bodySmall)
                    FeedbackReason.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = reason == option, role = Role.RadioButton, onClick = { reason = option }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = reason == option, onClick = null)
                            Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = reason != null, onClick = {
                    reason?.let(idea::reject)
                    showNotForMe = false
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showNotForMe = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WhyLines(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
}
