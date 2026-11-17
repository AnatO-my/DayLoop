package com.dayloop.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayloop.app.R
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopBottomActionBar
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

@Composable
fun FullscreenTimerView(
    session: FocusState,
    now: Instant,
    onPause: ((Long) -> Unit) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onSetSound: (FocusSound) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    BackHandler(onBack = onDismiss)
    val scope = rememberCoroutineScope()

    val seconds = session.secondsAt(now)
    val totalSeconds = session.sessionDuration.toFloat()
    val progress = if (totalSeconds > 0) (totalSeconds - seconds) / totalSeconds else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "TimerProgress",
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = modifier.fillMaxSize(),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                DayLoopIconButton(
                    resource = R.drawable.ic_action_back,
                    label = "Back",
                    onClick = onDismiss
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = session.mode.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor.copy(alpha = 0.7f)
                )

                FocusSoundPicker(
                    selected = session.preferredSound,
                    onSelect = onSetSound,
                    enabled = !busy,
                )

                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    
                    BasicText(
                        text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        autoSize = TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 72.sp),
                    )
                }

                if (session.mode == FocusMode.WORK && session.sets > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Set ${session.sets}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isActive = session.endsAt != null
                val appGraph = LocalAppGraph.current
                DayLoopBottomActionBar(
                    text = if (isActive) "Pause" else "Start",
                    onClick = { if (isActive) onPause { minutes ->
                        scope.launch { appGraph.settingsRepository.addFocusMinutes(minutes) }
                    } else onStart() },
                ) {
                    DayLoopIcon(if (isActive) R.drawable.ic_action_pause else R.drawable.ic_action_play)
                }

                DayLoopIconButton(
                    resource = R.drawable.ic_action_reset,
                    label = "Reset session",
                    onClick = onReset
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FullscreenTimerPreview() {
    val session = FocusState(
        mode = FocusMode.WORK,
        remaining = 1500L,
        endsAt = Instant.now().plusSeconds(1500L),
        sessionDuration = 1500L
    )
    val now = Instant.now()
    com.dayloop.app.ui.theme.DayLoopTheme {
        FullscreenTimerView(
            session = session,
            now = now,
            onPause = {},
            onStart = {},
            onReset = {},
            onSetSound = {},
            onDismiss = {}
        )
    }
}
