package com.dayloop.app.feature.taskeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ScheduleRuler(start: Int, end: Int, limit: Int, enabled: Boolean,
    onChange: (Int, Int) -> Unit) {
    val latestStart by rememberUpdatedState(start)
    val latestEnd by rememberUpdatedState(end)
    val change by rememberUpdatedState(onChange)
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = MaterialTheme.colorScheme.onPrimary
    val line = MaterialTheme.colorScheme.outline
    fun move(delta: Int) {
        val shift = delta.coerceIn(-start, limit - end)
        onChange(start + shift, end + shift)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, limit / 2, limit).forEach { minute ->
                Text(if (minute < 1440) "%02d:00".format(minute / 60) else "+${minute / 1440}d ${minute % 1440 / 60}:00",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(96.dp)
            .semantics {
                contentDescription = "Schedule time ruler"
                stateDescription = "Start minute $start, end minute $end"
                customActions = if (!enabled) emptyList() else listOf(
                    CustomAccessibilityAction("Move 15 minutes earlier") { move(-15); true },
                    CustomAccessibilityAction("Move 15 minutes later") { move(15); true },
                )
            }
            .onKeyEvent {
                if (!enabled || it.type != KeyEventType.KeyDown) false
                else when (it.key) {
                    Key.DirectionLeft -> { move(-15); true }
                    Key.DirectionRight -> { move(15); true }
                    else -> false
                }
            }.focusable(enabled)
            .pointerInput(enabled, limit) {
                if (!enabled) return@pointerInput
                var originStart = 0
                var originEnd = 0
                var accumulated = 0f
                var mode = 0
                detectDragGestures(onDragStart = { point ->
                    originStart = latestStart; originEnd = latestEnd; accumulated = 0f
                    val left = originStart.toFloat() / limit * size.width
                    val right = originEnd.toFloat() / limit * size.width
                    val hit = 24.dp.toPx()
                    mode = when {
                        abs(point.x - left) <= hit && abs(point.x - left) <= abs(point.x - right) -> 1
                        abs(point.x - right) <= hit -> 2
                        point.x in left..right -> 3
                        else -> 0
                    }
                }) { pointer, amount ->
                    if (mode != 0) {
                        pointer.consume()
                        accumulated += amount.x / size.width.coerceAtLeast(1) * limit
                        val delta = (accumulated / 15).roundToInt() * 15
                        when (mode) {
                            1 -> change((originStart + delta).coerceIn(0, originEnd - 1), originEnd)
                            2 -> change(originStart, (originEnd + delta).coerceIn(originStart + 1, limit))
                            3 -> {
                                val shift = delta.coerceIn(-originStart, limit - originEnd)
                                change(originStart + shift, originEnd + shift)
                            }
                        }
                    }
                }
            }) {
            val top = 24.dp.toPx()
            val height = 48.dp.toPx()
            drawRoundRect(track, topLeft = Offset(0f, top), size = Size(size.width, height), cornerRadius = CornerRadius(8.dp.toPx()))
            for (tick in 0..24) {
                val x = size.width * tick / 24
                drawLine(line, Offset(x, 8.dp.toPx()), Offset(x, if (tick % 6 == 0) 22.dp.toPx() else 16.dp.toPx()), 1.dp.toPx())
            }
            val left = size.width * start / limit
            val right = size.width * end / limit
            drawRoundRect(accent, Offset(left, top), Size((right - left).coerceAtLeast(2.dp.toPx()), height), CornerRadius(8.dp.toPx()))
            listOf(left, right).forEach { x ->
                drawCircle(ink, 6.dp.toPx(), Offset(x.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), top + height / 2))
            }
        }
    }
}
