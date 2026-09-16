package com.dayloop.app.feature.today

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.theme.LocalReducedMotion
import kotlin.math.abs

enum class TaskActionMenu { TIMING, ALL }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TaskGestureRow(enabled: Boolean, color: Color, onEdit: () -> Unit,
    onComplete: (() -> Unit)?, onTiming: (() -> Unit)?, onOptions: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var offset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var width by remember { mutableIntStateOf(1) }
    val density = LocalDensity.current
    val threshold = minOf(width * 0.32f, with(density) { 112.dp.toPx() })
    val haptic = LocalHapticFeedback.current
    val complete by rememberUpdatedState(onComplete)
    val timing by rememberUpdatedState(onTiming)
    val reduced = LocalReducedMotion.current
    val settling by animateFloatAsState(offset, if (reduced) snap() else spring(), label = "taskReturn")
    val configuration = LocalViewConfiguration.current
    val longHold = remember(configuration) { object : ViewConfiguration by configuration {
        override val longPressTimeoutMillis: Long = 2000L
    } }
    LaunchedEffect(enabled) { if (!enabled) { offset = 0f; dragging = false } }
    CompositionLocalProvider(LocalViewConfiguration provides longHold) {
        Box(modifier.fillMaxWidth().onSizeChanged { width = it.width }
            .clip(RoundedCornerShape(20.dp))) {
        val fraction = (offset / threshold).coerceIn(-1f, 1f)
        val actionColor = if (offset < 0f) Color(0xFF6B8E23) else MaterialTheme.colorScheme.secondary // Matcha-ish green
        val bgColor = lerp(color, actionColor, if (reduced) (if (abs(fraction) > 0.5f) 0.8f else 0f) else abs(fraction) * 0.8f)

        Box(Modifier.matchParentSize().background(bgColor))

        if (offset != 0f || settling != 0f) {
            Row(Modifier.matchParentSize().padding(horizontal = 16.dp),
                horizontalArrangement = if (offset < 0f) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically) {
                val iconScale = if (reduced) 1f else (1f + abs(fraction) * 0.2f).coerceAtMost(1.2f)
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                    Box(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
                        DayLoopIcon(if (offset < 0f) R.drawable.ic_action_check else R.drawable.ic_nav_focus,
                            if (offset < 0f) "Complete" else "Snooze or reschedule")
                    }
                }
            }
        }
        Surface(color = color, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
            .graphicsLayer { translationX = if (dragging) offset else settling }
            .pointerInput(enabled, width) {
                    if (enabled) detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragCancel = { dragging = false; offset = 0f },
                        onDragEnd = {
                            val action = if (offset <= -threshold) complete else if (offset >= threshold) timing else null
                            dragging = false; offset = 0f
                            action?.let { haptic.performHapticFeedback(HapticFeedbackType.LongPress); it() }
                        },
                    ) { change, delta ->
                        change.consume()
                        val proposed = offset + delta
                        offset = when {
                            proposed < 0 && complete == null -> 0f
                            proposed > 0 && timing == null -> 0f
                            else -> proposed.coerceIn(-width * 0.55f, width * 0.55f)
                        }
                    }
                }
                .combinedClickable(enabled = enabled, onClickLabel = "Edit task", onClick = onEdit,
                    onLongClickLabel = "Task options", onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress); onOptions()
                    })
                .semantics {
                    customActions = if (!enabled) emptyList() else buildList {
                        onComplete?.let { add(CustomAccessibilityAction("Complete task") { it(); true }) }
                        onTiming?.let { add(CustomAccessibilityAction("Snooze or reschedule") { it(); true }) }
                        add(CustomAccessibilityAction("Task options") { onOptions(); true })
                    }
                }) { content() }
        }
    }
}
