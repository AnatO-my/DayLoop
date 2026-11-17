package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dayloop.app.R
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.outfit.styling.FeedbackVerdict
import com.dayloop.app.core.outfit.styling.OutfitFeedback
import com.dayloop.app.core.outfit.styling.OutfitFeedbackPolicy
import com.dayloop.app.core.outfit.styling.OutfitRecommendation
import com.dayloop.app.core.outfit.styling.OutfitRecommender
import com.dayloop.app.core.outfit.styling.StylePreference
import com.dayloop.app.core.outfit.styling.StylingRequest
import com.dayloop.app.core.outfit.styling.StylingResult
import com.dayloop.app.core.outfit.styling.WardrobeEntry
import com.dayloop.app.core.outfit.styling.Weather
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Styling ideas for one item ([anchor]) or complete outfits from the whole wardrobe.
 * Everything is computed on the device from saved wardrobe data; no connection is needed.
 * Suggestions are drafts: nothing is recorded as worn without an explicit confirmation.
 */
@Composable
fun StylingScreen(anchor: ClothingItem?, onDismiss: () -> Unit) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    val items by remember { graph.outfitRepository.observeActiveWardrobe() }.collectAsState(initial = null)
    val attributes by remember { graph.wardrobeAttributesRepository.observeAll() }.collectAsState(initial = null)
    val history by remember { graph.outfitRepository.observeAllOutfits() }.collectAsState(initial = null)
    val today = remember { graph.timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate() }
    val feedback by remember(today) { graph.outfitFeedbackRepository.observeRecent(today) }.collectAsState(initial = emptyList())
    val rejected = remember(feedback, today) { OutfitFeedbackPolicy.rejectedCombinations(feedback, today) }

    var occasion by rememberSaveable { mutableStateOf<String?>(null) }
    var weather by rememberSaveable { mutableStateOf<String?>(null) }
    var style by rememberSaveable { mutableStateOf<String?>(null) }
    var rain by rememberSaveable { mutableStateOf(false) }
    var comfort by rememberSaveable { mutableStateOf(false) }
    var avoidRecent by rememberSaveable { mutableStateOf(true) }
    var pendingWear by remember { mutableStateOf<OutfitRecommendation?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val request = StylingRequest(
        today = today,
        anchorItemId = anchor?.id,
        occasion = occasion?.let(Occasion::valueOf),
        weather = weather?.let(Weather::valueOf),
        rainExpected = rain,
        style = style?.let(StylePreference::valueOf),
        comfortFirst = comfort,
        avoidWornWithinDays = if (avoidRecent) 2 else 0,
        rejectedCombinations = rejected,
    )
    val result by produceState<StylingResult?>(null, items, attributes, history, request) {
        val wardrobe = items
        val details = attributes
        val worn = history
        value = if (wardrobe == null || details == null || worn == null) null else withContext(Dispatchers.Default) {
            val lastWorn = HashMap<ClothingItemId, LocalDate>()
            for (record in worn) for (piece in record.items) {
                val previous = lastWorn[piece.id]
                if (previous == null || record.date.isAfter(previous)) lastWorn[piece.id] = record.date
            }
            OutfitRecommender.recommend(wardrobe.map { WardrobeEntry(it, details[it.id], lastWorn[it.id]) }, request)
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = tokens.spacing.pageMargin)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DayLoopIconButton(R.drawable.ic_action_back, stringResource(R.string.styling_close), onDismiss, enabled = !busy)
                    Text(
                        anchor?.let { stringResource(R.string.styling_title_for_item, it.name) } ?: stringResource(R.string.styling_title),
                        Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(tokens.spacing.cardPadding)) {
                    Text(stringResource(R.string.styling_offline_note), style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                    Section(stringResource(R.string.styling_occasion), stringResource(R.string.styling_optional)) {
                        ChipChoice(Occasion.entries, occasion?.let(Occasion::valueOf), { it.label }, !busy) { occasion = it?.name }
                    }
                    Section(stringResource(R.string.styling_weather), stringResource(R.string.styling_weather_optional)) {
                        ChipChoice(Weather.entries, weather?.let(Weather::valueOf), { it.label }, !busy) { weather = it?.name }
                    }
                    Section(stringResource(R.string.styling_style), stringResource(R.string.styling_optional)) {
                        ChipChoice(StylePreference.entries, style?.let(StylePreference::valueOf), { it.label }, !busy) { style = it?.name }
                    }
                    ToggleRow(stringResource(R.string.styling_rain_expected), rain, !busy) { rain = it }
                    ToggleRow(stringResource(R.string.styling_comfort_first), comfort, !busy) { comfort = it }
                    ToggleRow(stringResource(R.string.styling_avoid_recent), avoidRecent, !busy) { avoidRecent = it }

                    message?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    val current = result
                    if (current == null) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        current.notes.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        current.outfits.forEachIndexed { number, outfit ->
                            OutfitCard(number + 1, outfit, enabled = !busy) { pendingWear = outfit }
                        }
                        if (current.suggestedAdditions.isNotEmpty()) {
                            Section(stringResource(R.string.styling_ideas_to_add), stringResource(R.string.styling_ideas_to_add_note)) {
                                current.suggestedAdditions.forEach { addition ->
                                    Text(stringResource(R.string.styling_addition, addition.description, addition.reason), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (current.outfits.isNotEmpty()) {
                            Text(StylingResult.DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                        }
                    }
                    Spacer(Modifier.height(tokens.spacing.cardPadding))
                }
            }
        }
    }

    pendingWear?.let { outfit ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingWear = null },
            title = { Text(stringResource(R.string.styling_confirm_title)) },
            text = { Text(stringResource(R.string.styling_confirm_message, outfit.pieces.joinToString { it.item.name })) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            graph.outfitRepository.recordOutfit(today, outfit.pieces.map { it.item.id.value })
                            message = context.getString(R.string.styling_recorded)
                            // Remembering the choice is a nicety; failing to must not undo the record.
                            runCatching {
                                graph.outfitFeedbackRepository.record(OutfitFeedback(outfit.itemIds, FeedbackVerdict.WORN, null, today))
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            message = context.getString(R.string.styling_record_failed)
                        } finally {
                            busy = false
                            pendingWear = null
                        }
                    }
                }) { Text(stringResource(R.string.styling_record)) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingWear = null }) { Text(stringResource(R.string.wardrobe_cancel)) } },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun OutfitCard(number: Int, outfit: OutfitRecommendation, enabled: Boolean, onWear: () -> Unit) {
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shapeScale.cardCorner)).background(colors.raised).padding(tokens.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap),
    ) {
        Text(
            if (outfit.complete) stringResource(R.string.styling_option, number) else stringResource(R.string.styling_option_partial, number),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.styling_guideline_match, outfit.matchScore),
            style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
        )
        outfit.pieces.forEach { piece ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                WardrobeThumbnail(piece.item.id, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(piece.item.name, style = MaterialTheme.typography.titleSmall)
                    Text(piece.slot.displayLabel(), style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                }
            }
        }
        Bullets(stringResource(R.string.styling_why), outfit.reasons)
        Bullets(stringResource(R.string.styling_unmet), outfit.unmetConstraints)
        Bullets(stringResource(R.string.styling_cautions), outfit.cautions)
        DayLoopButton(onClick = onWear, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.styling_wear)) }
    }
}

@Composable
private fun Bullets(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge)
    lines.forEach { Text(stringResource(R.string.styling_bullet, it), style = MaterialTheme.typography.bodyMedium) }
}
