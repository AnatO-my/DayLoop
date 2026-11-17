package com.dayloop.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.feature.diagnostics.DiagnosticsRoute
import com.dayloop.app.settings.GuideKey

@Composable
fun AdvancedSettingsScreen(
    onDismiss: () -> Unit,
    refreshKey: Int = 0,
    onReplayGuide: ((GuideKey) -> Unit)? = null,
    onReplayIntroduction: (() -> Unit)? = null,
    replayEnabled: Boolean = true
) {
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClickLabel = "Close Advanced", onClick = onDismiss))
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max = 480.dp)
                .fillMaxWidth(0.94f),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                DiagnosticsRoute(
                    onBack = onDismiss,
                    refreshKey = refreshKey,
                    onReplayGuide = onReplayGuide,
                    onReplayIntroduction = onReplayIntroduction,
                    replayEnabled = replayEnabled
                )
            }
        }
    }
}
