package com.dayloop.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim.value - 200f, translateAnim.value - 200f),
        end = Offset(translateAnim.value, translateAnim.value),
    )

    return this.background(brush)
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(),
            )
        }
    }
}

@Composable
fun TodaySkeletonScreen() {
    val tokens = MaterialTheme.dayLoopTokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(tokens.spacing.pageMargin),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
    ) {
        // Greeting skeleton
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(),
            )
        }

        // Summary metrics skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                    .shimmer(),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                    .shimmer(),
            )
        }

        // Task cards skeleton
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonCard()
        SkeletonCard()
        SkeletonCard()
    }
}
