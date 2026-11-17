package com.dayloop.app.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayloop.app.R
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.ui.components.DayLoopBottomActionBar
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopMainNav
import com.dayloop.app.ui.components.DayLoopPageHeader
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.components.DayLoopTextField
import com.dayloop.app.ui.guides.GuideHost
import com.dayloop.app.ui.guides.GuideStep
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun FocusRoute(
    store: FocusStore,
    now: Instant,
    onNavigate: (String) -> Unit,
    onOpenNotes: () -> Unit = {},
    activeGuideStep: GuideStep? = null,
    guideNextAction: GuideAction? = null,
    guideBusy: Boolean = false,
    onGuideAction: (GuideAction) -> Unit = {}
) {
    val state by store.state.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun update(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
                error = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not save the focus session. Try again."
            } finally {
                busy = false
            }
        }
    }

    GuideHost(activeStep = activeGuideStep, onAction = onGuideAction, nextAction = guideNextAction, busy = guideBusy) {
        FocusScreen(
            state = state,
            now = now,
            busy = busy,
            error = error,
            onNavigate = onNavigate,
            onSelectMode = { mode -> update { store.selectMode(mode) } },
            onStart = { update { store.start(now) } },
            onPause = { callback ->
                update {
                    store.pause(now) { minutes ->
                        scope.launch { appGraph.settingsRepository.addFocusMinutes(minutes) }
                        callback(minutes)
                    }
                }
            },
            onReset = { mode -> update { store.reset(mode) } },
            onClearSets = { update { store.clearSets() } },
            onSetDuration = { mode, minutes, sets ->
                update {
                    store.setDuration(mode, minutes)
                    if (mode == FocusMode.LONG_BREAK && sets != null) {
                        store.setSetsUntilLongBreak(sets)
                    }
                }
            },
            onSetSound = { sound -> update { store.setPreferredSound(sound) } },
            onSaveReflection = { reflection -> update { store.saveReflection(reflection) } }
        )
    }
}

@Composable
fun FocusScreen(
    state: FocusState?,
    now: Instant,
    busy: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onSelectMode: (FocusMode) -> Unit,
    onStart: () -> Unit,
    onPause: (((Long) -> Unit) -> Unit),
    onReset: (FocusMode) -> Unit,
    onClearSets: () -> Unit,
    onSetDuration: (FocusMode, Int, Int?) -> Unit,
    onSetSound: (FocusSound) -> Unit,
    onSaveReflection: (String) -> Unit
) {
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

    val tokens = MaterialTheme.dayLoopTokens

    Scaffold(bottomBar = {
        DayLoopMainNav(
            selected = "focus",
            onSelected = onNavigate,
            enabled = !busy
        )
    }) { padding ->
        DayLoopScreen(Modifier.padding(padding).consumeWindowInsets(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
            ) {
                DayLoopPageHeader("Focus", { onNavigate("more") }, enabled = !busy)
                state?.let { session ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)
                    ) {
                        FocusMode.entries.forEach { mode ->
                            val selected = session.mode == mode
                            Surface(
                                modifier = Modifier.weight(1f).height(48.dp).combinedClickable(
                                    enabled = !busy, role = Role.Tab,
                                    onClick = { onSelectMode(mode) },
                                    onLongClickLabel = "Edit ${mode.label} duration",
                                    onLongClick = { editingMode = mode }),
                                shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                                color = tokens.colors.background
                            ) {
                                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        BasicText(
                                            mode.label, maxLines = 1,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = if (selected) tokens.colors.accentText else tokens.colors.secondaryText
                                            ),
                                            autoSize = TextAutoSize.StepBased(9.sp, 13.sp)
                                        )
                                        if (session.activeMode == mode && session.endsAt != null) {
                                            Surface(
                                                Modifier.size(6.dp), shape = CircleShape,
                                                color = tokens.colors.accentText
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val containerColor by animateColorAsState(
                        targetValue = tokens.colors.background,
                        label = "modeCrossFade"
                    )
                    Surface(
                        color = containerColor,
                        contentColor = tokens.colors.text,
                        shape = RoundedCornerShape(tokens.shapeScale.cardCorner)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(tokens.spacing.cardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)
                            ) {
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
                                    Surface(
                                        color = tokens.colors.accentSubtle,
                                        contentColor = tokens.colors.text,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            "Set ${session.sets}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            val seconds =
                                if (session.activeMode == session.mode) session.secondsAt(now) else session.durationFor(
                                    session.mode
                                )
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
                            val finalScale =
                                if (session.activeMode == session.mode && session.endsAt != null && seconds <= 60) pulseScale else 1f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { editingMode = session.mode },
                                        onLongClickLabel = "Edit ${session.mode.label} duration"
                                    )
                                    .guideTarget(GuideTargets.FOCUS_TIMER),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60),
                                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                                        scaleX = finalScale
                                        scaleY = finalScale
                                    },
                                    maxLines = 1,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                                        textAlign = TextAlign.Center,
                                        color = tokens.colors.text
                                    ),
                                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 56.sp),
                                )
                            }
                            FocusSoundPicker(
                                selected = session.preferredSound,
                                onSelect = onSetSound,
                                enabled = !busy,
                            )
                            val isActive = session.activeMode == session.mode && session.endsAt != null
                            DayLoopBottomActionBar(
                                text = if (isActive) "Pause" else "Start",
                                enabled = !busy,
                                onClick = { if (isActive) onPause {} else onStart() },
                            ) {
                                DayLoopIcon(if (isActive) R.drawable.ic_action_pause else R.drawable.ic_action_play,
                                    tint = tokens.colors.onAccent)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                DayLoopIconButton(
                                    resource = R.drawable.ic_action_reset,
                                    label = "Reset session",
                                    enabled = !busy,
                                    onClick = { onReset(session.mode) }
                                )
                                if (session.mode == FocusMode.WORK && session.sets > 0) {
                                    TextButton(onClick = onClearSets, enabled = !busy) {
                                        Text("Clear sets", color = tokens.colors.secondaryText)
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Focused time today", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${session.minutesOn(now.atZone(ZoneId.systemDefault()).toLocalDate())} min",
                            style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center
                        )
                    }
                } ?: CircularProgressIndicator()
                error?.let { Text(it, color = tokens.colors.error) }
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
            val appGraph = LocalAppGraph.current
            val scope = rememberCoroutineScope()
            FullscreenTimerView(
                session = session,
                now = now,
                onPause = { callback ->
                    val instant = appGraph.timeProvider.now()
                    // Need a way to communicate this back to store if we are in Fullscreen
                    // This is complex, for now keep the simplified passing
                },
                onStart = onStart,
                onReset = { onReset(session.mode) },
                onSetSound = onSetSound,
                busy = busy,
                onDismiss = { isFullscreen = false }
            )
        }
    }

    editingMode?.let { mode ->
        state?.let { session ->
            key(mode) {
                var minutes by rememberSaveable { mutableStateOf((session.durationFor(mode) / 60).toString()) }
                var setsUntilLongBreak by rememberSaveable { mutableStateOf(session.setsUntilLongBreak.toString()) }
                val value = minutes.toIntOrNull()
                val setsValue = setsUntilLongBreak.toIntOrNull()
                val isValid =
                    value != null && value in 1..180 && (mode != FocusMode.LONG_BREAK || (setsValue != null && setsValue in 1..20))
                AlertDialog(onDismissRequest = { if (!busy) editingMode = null },
                    title = { Text(if (mode == FocusMode.WORK) "Pomodoro Work duration" else mode.label) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DayLoopTextField(
                                minutes, { minutes = it }, enabled = !busy,
                                label = "Minutes", isError = value == null || value !in 1..180,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                            if (mode == FocusMode.LONG_BREAK) {
                                DayLoopTextField(
                                    setsUntilLongBreak, { setsUntilLongBreak = it }, enabled = !busy,
                                    label = "Sets until long break", isError = setsValue == null || setsValue !in 1..20,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    )
                                )
                            }
                            if (mode == FocusMode.WORK)
                                Text(
                                    "A standard pomodoro loops work and rests. This setting changes the duration of each work period.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            else if (session.mode == mode && session.endsAt != null)
                                Text(
                                    "The current timer is already running. This change will apply to the next session.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            TextButton(
                                enabled = !busy,
                                onClick = { minutes = (mode.seconds / 60).toString(); setsUntilLongBreak = "4" }) {
                                Text("Use default")
                            }
                            error?.let { Text(it, color = tokens.colors.error) }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = !busy && isValid,
                            onClick = {
                                onSetDuration(mode, value!!, setsValue)
                                editingMode = null
                            }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !busy,
                            onClick = { editingMode = null }) { Text("Cancel") }
                    })
            }
        }
    }

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
                    Text(
                        "What did you achieve during this focus session?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    DayLoopTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "e.g. Finished Chapter 1",
                        enabled = !busy
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSaveReflection(text.trim())
                            showReflection = false
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

@Preview(showBackground = true)
@Composable
private fun FocusScreenPreview() {
    val now = Instant.now()
    val state = FocusState(
        mode = FocusMode.WORK,
        activeMode = FocusMode.WORK,
        remaining = 1500L,
        endsAt = now.plusSeconds(1500L),
        sessionDuration = 1500L,
        minutes = mapOf(now.atZone(ZoneId.systemDefault()).toLocalDate() to 45L),
        sets = 2,
        preferredSound = FocusSound.RAIN
    )
    DayLoopTheme {
        FocusScreen(
            state = state,
            now = now,
            busy = false,
            error = null,
            onNavigate = {},
            onSelectMode = {},
            onStart = {},
            onPause = {},
            onReset = {},
            onClearSets = {},
            onSetDuration = { _, _, _ -> },
            onSetSound = {},
            onSaveReflection = {}
        )
    }
}
