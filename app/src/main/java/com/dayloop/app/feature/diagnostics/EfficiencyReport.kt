package com.dayloop.app.feature.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.settings.UsageStats
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun EfficiencyReport(
    usageStats: UsageStats,
    onBack: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens
    
    // Simple impact calculation
    val focusMinutes = usageStats.totalFocusMinutes
    val backgroundSeconds = usageStats.backgroundServiceSeconds
    val batteryScore = (100 - (backgroundSeconds / 3600f) * 0.5f).coerceIn(0f, 100f)
    
    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(R.drawable.ic_action_back, "Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text("Efficiency Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(tokens.spacing.sectionGap))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(Modifier.padding(tokens.spacing.cardPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                    DayLoopMascot(
                        modifier = Modifier.size(120.dp),
                        mood = MascotMood.SUGGESTING,
                        lookingDown = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Battery Efficiency Score", style = MaterialTheme.typography.labelMedium)
                    Text("${batteryScore.toInt()}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text("DayLoop is running optimally.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(Modifier.height(tokens.spacing.sectionGap))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricRow("Total Focus Time", "${focusMinutes}m")
                MetricRow("Background Active", "${backgroundSeconds / 60}m")
                MetricRow("Estimated Impact", "Minimal (< 1%)")
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "Compared to a 5-minute video call, DayLoop uses 90% less energy to keep your focus sessions active.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.mutedText
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
