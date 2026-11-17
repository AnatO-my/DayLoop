package com.dayloop.app.feature.wardrobe

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dayloop.app.core.outfit.recognition.NormalizedBox
import com.dayloop.app.core.outfit.recognition.RegionMath
import com.dayloop.app.core.outfit.recognition.ReviewSession
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A photo holding several items: numbered outlines on the picture and a tick list below.
 * The list is the accessible path (every action is a labelled control there); tapping an
 * outline is a shortcut for sighted touch users. In drawing mode a drag marks an item the
 * recogniser missed.
 */
@Composable
internal fun GarmentPhotoOverview(
    photo: Uri,
    /** Width divided by height of the upright photo. */
    aspect: Float,
    session: ReviewSession,
    drawing: Boolean,
    enabled: Boolean,
    onToggleIncluded: (key: Int, included: Boolean) -> Unit,
    onReview: (key: Int) -> Unit,
    onDrawn: (NormalizedBox) -> Unit,
    onToggleDrawing: () -> Unit,
) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val includedColor = colors.accent
    val otherColor = colors.secondaryText
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (drawing) "Drag a box around the item on the photo."
            else "Tap an outline to review it. Untick anything you don't want to add.",
            style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
        )
        Box(Modifier.fillMaxWidth().background(colors.surface), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.heightIn(max = 340.dp).aspectRatio(aspect.coerceIn(0.3f, 3f))) {
                val widthPx = constraints.maxWidth
                val heightPx = constraints.maxHeight
                var dragStart by remember { mutableStateOf<Offset?>(null) }
                var dragEnd by remember { mutableStateOf<Offset?>(null) }
                val regions = session.entries.map { it.key to it.prediction?.region }
                AsyncImage(
                    model = photo,
                    contentDescription = "Your photo with ${session.entries.size} items outlined",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Canvas(
                    Modifier.fillMaxSize().pointerInput(drawing, enabled, regions) {
                        if (!enabled) return@pointerInput
                        if (drawing) {
                            detectDragGestures(
                                onDragStart = { dragStart = it; dragEnd = it },
                                onDragCancel = { dragStart = null; dragEnd = null },
                                onDragEnd = {
                                    val start = dragStart
                                    val end = dragEnd
                                    dragStart = null
                                    dragEnd = null
                                    if (start != null && end != null) {
                                        RegionMath.fromDrag(start.x, start.y, end.x, end.y, size.width, size.height)?.let(onDrawn)
                                    }
                                },
                            ) { change, _ -> dragEnd = change.position }
                        } else {
                            detectTapGestures { tap ->
                                RegionMath.hit(regions, tap.x / size.width, tap.y / size.height)?.let(onReview)
                            }
                        }
                    }
                ) {
                    val stroke = Stroke(width = 2.dp.toPx())
                    session.entries.forEach { entry ->
                        val box = entry.prediction?.region ?: return@forEach
                        drawRect(
                            color = if (entry.included) includedColor else otherColor,
                            topLeft = Offset(box.left * size.width, box.top * size.height),
                            size = Size((box.right - box.left) * size.width, (box.bottom - box.top) * size.height),
                            style = stroke,
                        )
                    }
                    val start = dragStart
                    val end = dragEnd
                    if (start != null && end != null) {
                        drawRect(
                            color = includedColor,
                            topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                            size = Size(abs(end.x - start.x), abs(end.y - start.y)),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
                        )
                    }
                }
                session.entries.forEachIndexed { index, entry ->
                    val box = entry.prediction?.region ?: return@forEachIndexed
                    Text(
                        "${index + 1}",
                        Modifier
                            .offset { IntOffset((box.left * widthPx).roundToInt(), (box.top * heightPx).roundToInt()) }
                            .background(if (entry.included) includedColor else otherColor)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black,
                    )
                }
            }
        }
        session.entries.forEachIndexed { index, entry ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = entry.included,
                    onCheckedChange = { onToggleIncluded(entry.key, it) },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = "Add ${entry.title}" },
                )
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}  ${entry.title}", style = MaterialTheme.typography.titleSmall)
                    Text(entry.status, style = MaterialTheme.typography.bodySmall, color = if (entry.ready) colors.secondaryText else colors.warning)
                }
                TextButton(onClick = { onReview(entry.key) }, enabled = enabled) { Text("Review", color = colors.accentText) }
            }
            HorizontalDivider(color = colors.decorativeBorder)
        }
        TextButton(onClick = onToggleDrawing, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (drawing) "Stop drawing" else "Missed something? Draw around it", color = colors.accentText)
        }
    }
}
