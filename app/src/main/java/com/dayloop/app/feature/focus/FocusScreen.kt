package com.dayloop.app.feature.focus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.designsystem.DayLoopHeader
import com.dayloop.app.ui.designsystem.DayLoopNavItem
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.R
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun FocusRoute(store: FocusStore, now: Instant, onNavigate: (String) -> Unit, onOpenSettings: () -> Unit = {}, onOpenNotes: () -> Unit = {}) {
    val state by store.state.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingMode by rememberSaveable { mutableStateOf<FocusMode?>(null) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showReflection by rememberSaveable { mutableStateOf(false) }

    // Trigger reflection dialog when a WORK session completes
    LaunchedEffect(state?.activeMode, state?.endsAt, state?.remaining) {
        val s = state ?: return@LaunchedEffect
        if (s.activeMode == FocusMode.WORK && s.endsAt == null && s.remaining == 0L) {
            showReflection = true
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val appGraph = LocalAppGraph.current

    fun update(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block(); error = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not save the focus session. Try again." }
            finally { busy = false }
        }
    }

    val onPause: () -> Unit = {
        update { 
            store.pause(now) { minutes ->
                scope.launch { appGraph.settingsRepository.addFocusMinutes(minutes) }
            }
        }
    }

    Scaffold(bottomBar = { DayLoopMainNav(
        selected = "focus",
        onSelected = onNavigate,
        onHoldAdd = onOpenNotes,
        onOpenSettings = onOpenSettings,
        enabled = !busy) }) { padding ->
        DayLoopScreen(Modifier.padding(padding).consumeWindowInsets(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DayLoopHeader("Focus")
                state?.let { session ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FocusMode.entries.forEach { mode ->
                            val selected = session.mode == mode
                            Surface(modifier = Modifier.weight(1f).height(56.dp).combinedClickable(
                                enabled = !busy, role = androidx.compose.ui.semantics.Role.Tab,
                                onClick = { update { store.selectMode(mode) } },
                                onLongClickLabel = "Edit ${mode.label} duration",
                                onLongClick = { editingMode = mode }),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        BasicText(mode.label, maxLines = 1,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface),
                                            autoSize = TextAutoSize.StepBased(9.sp, 13.sp))
                                        if (session.activeMode == mode && session.endsAt != null) {
                                            Surface(Modifier.size(6.dp), shape = CircleShape,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val containerColor by animateColorAsState(
                        targetValue = if (session.mode == FocusMode.WORK) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        label = "modeCrossFade"
                    )
                    Surface(color = containerColor,
                        contentColor = contentColorFor(containerColor),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            
                            // Sound Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FocusSound.entries.forEach { sound ->
                                    val isSelected = session.preferredSound == sound
                                    Surface(
                                        onClick = { update { store.setPreferredSound(sound) } },
                                        enabled = !busy,
                                        shape = CircleShape,
                                        color = if (isSelected) contentColorFor(containerColor) else Color.Transparent,
                                        contentColor = if (isSelected) containerColor else contentColorFor(containerColor),
                                        modifier = Modifier.size(44.dp),
                                        border = if (isSelected) null else BorderStroke(1.dp, contentColorFor(containerColor).copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = when (sound) {
                                                    FocusSound.NONE -> "❌"
                                                    FocusSound.RAIN -> "🌧️"
                                                    FocusSound.FIRE -> "🔥"
                                                    FocusSound.STREAM -> "🏞️"
                                                    FocusSound.LIBRARY -> "📚"
                                                    FocusSound.CAFE -> "☕"
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = sound.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 7.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isComplete = session.remaining == 0L && session.endsAt == null
                                val isActive = session.activeMode == session.mode && session.endsAt != null
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable(enabled = isActive) { isFullscreen = true }
                                ) {
                                    Text(
                                        text = when {
                                            isActive -> session.mode.label
                                            isComplete && session.activeMode == session.mode -> "Session complete"
                                            else -> session.mode.label
                                        }
                                    )
                                }
                                if (session.mode == FocusMode.WORK && session.sets > 0) {
                                    Surface(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                        shape = CircleShape) {
                                        Text("Set ${session.sets}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            val seconds = if (session.activeMode == session.mode) session.secondsAt(now) else session.durationFor(session.mode)
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "timerPulse"
                            )
                            val finalScale = if (session.activeMode == session.mode && session.endsAt != null && seconds <= 60) pulseScale else 1f
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { editingMode = session.mode },
                                        onLongClickLabel = "Edit ${session.mode.label} duration"
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60),
                                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                                        scaleX = finalScale
                                        scaleY = finalScale
                                    },
                                    maxLines = 1,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = contentColorFor(containerColor)
                                    ),
                                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 56.sp),
                                )
                            }
                            val isActive = session.activeMode == session.mode && session.endsAt != null
                            DayLoopBottomActionBar(
                                text = if (isActive) "Pause" else "Start",
                                enabled = !busy,
                                onClick = { if (isActive) onPause() else update { store.start(now) } },
                            ) { DayLoopIcon(if (isActive) R.drawable.ic_action_pause else R.drawable.ic_action_play) }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                DayLoopIconButton(
                                    resource = R.drawable.ic_action_reset,
                                    label = "Reset session",
                                    enabled = !busy,
                                    onClick = { update { store.reset(session.mode) } }
                                )
                                if (session.mode == FocusMode.WORK && session.sets > 0) {
                                    TextButton(onClick = { update { store.clearSets() } }, enabled = !busy) {
                                        Text("Clear sets", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                    Text("${session.minutesOn(now.atZone(ZoneId.systemDefault()).toLocalDate())} minutes completed today",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                } ?: CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    state?.let { session ->
        AnimatedVisibility(
            visible = isFullscreen,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            val isActive = session.activeMode == session.mode && session.endsAt != null
            if (!isActive && isFullscreen) {
                LaunchedEffect(Unit) { isFullscreen = false }
            }
            FullscreenTimerView(
                session = session,
                now = now,
                onPause = { update { store.pause(now) } },
                onStart = { update { store.start(now) } },
                onReset = { update { store.reset(session.mode) } },
                onSetSound = { sound -> update { store.setPreferredSound(sound) } },
                onDismiss = { isFullscreen = false }
            )
        }
    }

    editingMode?.let { mode -> state?.let { session ->
        key(mode) {
            var minutes by rememberSaveable { mutableStateOf((session.durationFor(mode) / 60).toString()) }
            var setsUntilLongBreak by rememberSaveable { mutableStateOf(session.setsUntilLongBreak.toString()) }
            val value = minutes.toIntOrNull()
            val setsValue = setsUntilLongBreak.toIntOrNull()
            val isValid = value != null && value in 1..180 && (mode != FocusMode.LONG_BREAK || (setsValue != null && setsValue in 1..20))
            AlertDialog(onDismissRequest = { if (!busy) editingMode = null },
                title = { Text(if (mode == FocusMode.WORK) "Pomodoro Work duration" else mode.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(minutes, { minutes = it }, enabled = !busy, singleLine = true,
                            label = { Text("Minutes") }, isError = value == null || value !in 1..180,
                            supportingText = { Text("1-180 minutes") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                        if (mode == FocusMode.LONG_BREAK) {
                            OutlinedTextField(setsUntilLongBreak, { setsUntilLongBreak = it }, enabled = !busy, singleLine = true,
                                label = { Text("Sets until long break") }, isError = setsValue == null || setsValue !in 1..20,
                                supportingText = { Text("Work sessions before a long break") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                        }
                        if (mode == FocusMode.WORK)
                            Text("A standard pomodoro loops work and rests. This setting changes the duration of each work period.", style = MaterialTheme.typography.bodySmall)
                        else if (session.mode == mode && session.endsAt != null)
                            Text("The current timer is already running. This change will apply to the next session.", style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !busy, onClick = { minutes = (mode.seconds / 60).toString(); setsUntilLongBreak = "4" }) { Text("Use default") }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = { TextButton(enabled = !busy && isValid,
                    onClick = { update { 
                        value?.let { store.setDuration(mode, it) }
                        if (mode == FocusMode.LONG_BREAK) setsValue?.let { store.setSetsUntilLongBreak(it) }
                        editingMode = null 
                    } }) { Text("Save") } },
                dismissButton = { TextButton(enabled = !busy, onClick = { editingMode = null }) { Text("Cancel") } })
        }
    } }

    if (showReflection) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReflection = false },
            title = { Text("Session complete") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DayLoopMascot(
                        modifier = Modifier.size(120.dp),
                        mood = MascotMood.ACCOMPLISHED
                    )
                    Text("What did you achieve during this focus session?", 
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center)
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Finished Chapter 1") },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            update { store.saveReflection(text.trim()); showReflection = false }
                        } else {
                            showReflection = false
                        }
                    },
                    enabled = !busy
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showReflection = false }, enabled = !busy) { Text("Skip") }
            }
        )
    }
}
