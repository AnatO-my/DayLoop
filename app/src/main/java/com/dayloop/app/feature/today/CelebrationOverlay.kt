package com.dayloop.app.feature.today

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

@Composable
fun CelebrationOverlay(
    active: Boolean,
    onFinished: () -> Unit
) {
    if (!active) return

    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                color = listOf(
                    Color(0xFFA8CF3D), // DayLoop Lime
                    Color(0xFF54DDA2), // Emerald
                    Color(0xFFF4A7BF), // Rose
                    Color(0xFF6200EE)  // Purple
                ).random(),
                speed = 0.01f + Random.nextFloat() * 0.02f,
                rotationSpeed = Random.nextFloat() * 5f,
                size = 10f + Random.nextFloat() * 20f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )

    LaunchedEffect(progress) {
        if (progress > 0.95f) {
            onFinished()
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val currentY = (p.y + progress * 1.5f) % 1.5f
            if (currentY in 0f..1f) {
                rotate(degrees = progress * 360f * p.rotationSpeed, pivot = Offset(p.x * size.width, currentY * size.height)) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x * size.width, currentY * size.height),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val rotationSpeed: Float,
    val size: Float
)
