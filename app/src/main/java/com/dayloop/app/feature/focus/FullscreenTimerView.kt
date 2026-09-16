package com.dayloop.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayloop.app.R
import com.dayloop.app.ui.components.DayLoopBottomActionBar
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.theme.DayLoopFontFamily
import com.dayloop.app.ui.theme.dayLoopTokens
import java.time.Instant
import java.util.Locale

@Composable
fun FullscreenTimerView(
    session: FocusState,
    now: Instant,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val tokens = MaterialTheme.dayLoopTokens
    val seconds = session.secondsAt(now)
    val totalSeconds = session.sessionDuration.toFloat()
    val progress = if (totalSeconds > 0) (totalSeconds - seconds) / totalSeconds else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "TimerProgress",
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    iconRes = R.drawable.ic_action_back,
                    contentDescription = "Back",
                    onClick = onDismiss,
                    enabled = true,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = session.mode.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                            fontFamily = DayLoopFontFamily,
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
                DayLoopBottomActionBar(
                    text = if (isActive) "Pause" else "Start",
                    onClick = { if (isActive) onPause() else onStart() },
                ) {
                    DayLoopIcon(if (isActive) R.drawable.ic_action_pause else R.drawable.ic_action_play)
                }

                DayLoopIconButton(
                    iconRes = R.drawable.ic_action_reset,
                    contentDescription = "Reset session",
                    onClick = onReset
                )
            }
        }
    }
}
