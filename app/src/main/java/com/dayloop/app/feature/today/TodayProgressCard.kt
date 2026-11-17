package com.dayloop.app.feature.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.dayLoopTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayProgressCard(
    completedCount: Int,
    remainingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val completed = completedCount.coerceAtLeast(0)
    val remaining = remainingCount.coerceAtLeast(0)
    val total = completed.toLong() + remaining.toLong()
    val progress = if (total == 0L) 0f else completed.toFloat() / total.toFloat()
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.inputCorner),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Counts wrap instead of squeezing OCR glyphs on narrow screens or large text.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Today's progress", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 16.dp))
                Text("$completed done · $remaining left", style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = colors.accentText,
                trackColor = colors.progressTrack,
                strokeCap = StrokeCap.Butt,
            )
        }
    }
}
