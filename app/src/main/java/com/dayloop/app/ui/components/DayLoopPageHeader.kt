package com.dayloop.app.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DayLoopPageHeader(
    title: String,
    onMore: () -> Unit,
    enabled: Boolean = true,
    mood: MascotMood = MascotMood.NEUTRAL,
    modifier: Modifier = Modifier,
    mascotModifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f).semantics { heading() })
        Box(mascotModifier.size(48.dp).clip(CircleShape)
            .combinedClickable(enabled = enabled, role = Role.Button,
                onClickLabel = "Open More", onLongClickLabel = "Open More",
                onClick = onMore, onLongClick = onMore)
            .semantics(mergeDescendants = true) { contentDescription = "More settings" },
            contentAlignment = Alignment.Center) {
            DayLoopMascot(Modifier.size(36.dp).clearAndSetSemantics {}, mood = mood)
        }
    }
}
