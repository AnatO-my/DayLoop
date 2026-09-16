package com.dayloop.app.feature.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun SettingsPullHost(
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    var hintVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    
    // Bottom region: 88dp height, placed above typical nav bar height (approx 80dp)
    val regionHeight = with(density) { 88.dp.toPx() }
    val navBarHeight = with(density) { 80.dp.toPx() } // Approximate
    val screenHeight = with(density) { config.screenHeightDp.dp.toPx() }
    
    val triggerBottom = screenHeight - navBarHeight
    val triggerTop = triggerBottom - regionHeight

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        
                        // Check if down event is in the trigger region
                        if (down.position.y < triggerTop || down.position.y > triggerBottom) return@awaitEachGesture
                        
                        var longPressed = false
                        var swipedUp = false
                        
                        // Start a timer for long press
                        val longPressTimer = scope.launch {
                            delay(1500) // Show hint after 1.5s
                            hintVisible = true
                            longPressed = true
                        }
                        
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                
                                if (!change.pressed) {
                                    if (longPressed && !swipedUp) {
                                        // If released after long press but no swipe, hide hint
                                        hintVisible = false
                                    }
                                    break
                                }
                                
                                val delta = change.position - down.position
                                
                                // Cancel long press timer if moved too much horizontally or downwards
                                if (!longPressed && (abs(delta.x) > viewConfiguration.touchSlop || delta.y > viewConfiguration.touchSlop)) {
                                    longPressTimer.cancel()
                                }
                                
                                // Detect swipe up after long press
                                if (longPressed && delta.y < -viewConfiguration.touchSlop * 2) {
                                    swipedUp = true
                                    change.consume()
                                }
                            }
                            
                            if (swipedUp) {
                                hintVisible = false
                                onOpenSettings()
                            }
                        } finally {
                            longPressTimer.cancel()
                        }
                    }
                }
        ) {
            content()
        }

        // Bouncing hint
        AnimatedVisibility(
            visible = hintVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Positioned above the nav bar area
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "bounce")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounceOffset"
            )

            Surface(
                modifier = Modifier
                    .graphicsLayer { translationY = offsetY }
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                tonalElevation = 4.dp
            ) {
                Text(
                    "pull-up for settings",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
