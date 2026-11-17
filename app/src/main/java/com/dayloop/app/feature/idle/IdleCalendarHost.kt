package com.dayloop.app.feature.idle

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.OutfitRecord
import com.dayloop.app.core.model.Task
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun IdleCalendarHost(date: LocalDate, tasks: List<Task>, outfits: List<OutfitRecord>, enabled: Boolean, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val threshold = with(density) { 64.dp.toPx() }
    
    // Trigger region restricted to the title block area (top 88dp)
    val topEdge = with(density) { 88.dp.toPx() }

    LaunchedEffect(enabled) { if (!enabled) visible = false }
    Box(Modifier.fillMaxSize()
        .semantics {
            if (enabled) customActions = listOf(CustomAccessibilityAction("Open idle calendar") { visible = true; true })
        }
        .pointerInput(enabled, topEdge, threshold) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.y > topEdge) return@awaitEachGesture
                var claimed = false
                // Inspect before children only for top-origin downward gestures.
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } > 1) break
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - down.position
                    if (!claimed && (abs(delta.x) > viewConfiguration.touchSlop || delta.y < -viewConfiguration.touchSlop)) break
                    if (delta.y > viewConfiguration.touchSlop && delta.y > abs(delta.x)) claimed = true
                    if (claimed) change.consume()
                    if (!change.pressed) {
                        if (claimed && delta.y >= threshold) visible = true
                        break
                    }
                }
            }
        }) {
        content()
    }
    if (visible && enabled) IdleScreen(date, tasks, outfits, onExit = { visible = false })
}
