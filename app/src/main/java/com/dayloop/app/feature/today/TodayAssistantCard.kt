package com.dayloop.app.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.today.AssistantSuggestion
import com.dayloop.app.today.SuggestionType
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun TodayAssistantCard(
    suggestion: AssistantSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = MaterialTheme.dayLoopTokens
    
    val containerColor = when (suggestion.suggestionType) {
        SuggestionType.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        SuggestionType.PRIORITY_UPGRADE -> MaterialTheme.colorScheme.primaryContainer
        SuggestionType.GAP_FILL -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = contentColorFor(containerColor)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayLoopIcon(
                    resource = R.drawable.ic_nav_more, // Sparkle-like icon placeholder
                    description = null,
                    tint = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                DayLoopIconButton(
                    resource = R.drawable.ic_action_close,
                    label = "Dismiss",
                    onClick = onDismiss,
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.9f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onApply,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) {
                    Text(
                        text = if (suggestion.suggestionType == SuggestionType.OVERDUE) "Start Now" else "Schedule Now",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
