package com.dayloop.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.components.*

@Composable
fun WelcomeScreen(saving: Boolean, onStart: () -> Unit, onClose: (() -> Unit)? = null) {
    DayLoopScreen(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
            onClose?.let { close ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DayLoopIconButton(
                        resource = R.drawable.ic_action_close,
                        label = "Close welcome",
                        enabled = !saving,
                        onClick = close
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Image(painterResource(if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                R.drawable.dayloop_wordmark_dark else R.drawable.dayloop_wordmark_light),
                contentDescription = "DayLoop", modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth().aspectRatio(405f / 102f))
            DayLoopMascot(
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f)
            )
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(R.drawable.ic_nav_plan to "Plan your day with time",
                    R.drawable.ic_nav_focus to "Focus on what matters",
                    R.drawable.ic_nav_goals to "Build lasting momentum").forEach { (icon, title) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center) {
                            CompositionLocalProviderForIcon(icon)
                        }
                        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
            }
            DayLoopBottomActionBar(text = if (saving) "Starting..." else "Start the day", onClick = onStart, enabled = !saving) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    contentColor = Color(0xFF111416)) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { DayLoopIcon(R.drawable.ic_action_forward) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompositionLocalProviderForIcon(icon: Int) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
        DayLoopIcon(icon)
    }
}
