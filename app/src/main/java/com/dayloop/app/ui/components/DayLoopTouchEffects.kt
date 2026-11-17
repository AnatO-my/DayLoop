package com.dayloop.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class DayLoopHapticKind {
    LIGHT_TICK,
    MEDIUM_TICK,
    LONG_PRESS,
}

fun HapticFeedback.perform(kind: DayLoopHapticKind) {
    val type = when (kind) {
        DayLoopHapticKind.LIGHT_TICK -> HapticFeedbackType.TextHandleMove
        DayLoopHapticKind.MEDIUM_TICK -> HapticFeedbackType.TextHandleMove
        DayLoopHapticKind.LONG_PRESS -> HapticFeedbackType.LongPress
    }
    performHapticFeedback(type)
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    targetScale: Float = 0.95f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "bounceScale",
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
