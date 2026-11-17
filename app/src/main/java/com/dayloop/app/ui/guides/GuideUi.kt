package com.dayloop.app.ui.guides

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.theme.dayLoopTokens

/** Unique IDs for guide targets within a screen. */
object GuideTargets {
    const val TODAY_SUMMARY = "today_summary"
    const val TODAY_ADD_TASK = "today_add_task"
    const val TODAY_TASK_ROW = "today_task_row"
    const val TODAY_NAVIGATION = "today_navigation"
    const val TODAY_MORE = "today_more"
    const val EDITOR_TITLE = "editor_title"
    const val EDITOR_DESCRIPTION = "editor_description"
    const val EDITOR_SUBTASKS = "editor_subtasks"
    const val EDITOR_SETTINGS = "editor_settings"
    const val EDITOR_SAVE = "editor_save"
    const val SETTINGS_SCHEDULE = "settings_schedule"
    const val PLANS_TABS = "plans_tabs"
    const val PLANS_ADD = "plans_add"
    const val FOCUS_TIMER = "focus_timer"
    const val WARDROBE_ADD = "wardrobe_add"
    const val CALENDAR_DOTS = "calendar_dots"
}

data class GuideStep(
    val id: String,
    val targetId: String,
    val title: String,
    val message: String,
    val nextActionLabel: String = "Next",
    val isFinal: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
private data class GuideAnchor(val owner: Any, val bounds: Rect, val requester: BringIntoViewRequester? = null)
private val LocalGuideTargetMap = compositionLocalOf<MutableMap<String, GuideAnchor>?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuideHost(
    activeStep: GuideStep?,
    onAction: (GuideAction) -> Unit,
    nextAction: GuideAction? = null,
    busy: Boolean = false,
    content: @Composable () -> Unit
) {
    // The app owns the overlay; screen hosts reuse its registry instead of drawing twice.
    // A screen rendered independently (preview/test) still gets its own host.
    if (LocalGuideTargetMap.current != null) {
        content()
        return
    }
    val targets = remember { mutableStateMapOf<String, GuideAnchor>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    BackHandler(enabled = activeStep != null) { if (!busy) onAction(GuideAction.Skip) }

    // Auto-scroll target into view
    LaunchedEffect(activeStep?.id) {
        activeStep?.let { step ->
            targets[step.targetId]?.requester?.bringIntoView()
        }
    }

    CompositionLocalProvider(LocalGuideTargetMap provides targets) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
            content()

            if (activeStep != null) {
                val target = targets[activeStep.targetId]?.bounds
                GuideOverlay(
                    step = activeStep,
                    targetBounds = target?.translate(-origin.x, -origin.y),
                    busy = busy,
                    canAdvance = nextAction != null,
                    onNext = { nextAction?.let(onAction) },
                    onSkip = { if (!busy) onAction(GuideAction.Skip) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.guideTarget(id: String): Modifier {
    val targets = LocalGuideTargetMap.current ?: return this
    val owner = remember(id) { Any() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    
    DisposableEffect(targets, id, owner) {
        onDispose { if (targets[id]?.owner === owner) targets.remove(id) }
    }
    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coordinates ->
            targets[id] = GuideAnchor(owner, coordinates.boundsInRoot(), bringIntoViewRequester)
        }
}

@Composable
private fun GuideOverlay(
    step: GuideStep,
    targetBounds: Rect?,
    busy: Boolean,
    canAdvance: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val scrimColor = Color.Black.copy(alpha = 0.7f)
    val tokens = MaterialTheme.dayLoopTokens
    var panelHeight by remember(step.id) { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = with(density) { maxHeight.toPx() }
        val width = with(density) { maxWidth.toPx() }
        val rect = targetBounds?.intersect(Rect(0f, 0f, width, height))
            ?.takeIf { it.width > 0f && it.height > 0f }
        // Drawing is non-intercepting: the real control and scrolling remain usable.
        Canvas(Modifier.fillMaxSize()) {
            if (rect == null) return@Canvas
            val path = Path().apply {
                addRect(Rect(Offset.Zero, this@Canvas.size))
            }
            val targetPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(tokens.shapeScale.cardCorner.toPx(), tokens.shapeScale.cardCorner.toPx())
                    )
                )
            }
            val cutPath = Path.combine(PathOperation.Difference, path, targetPath)
            drawPath(cutPath, color = scrimColor)
        }

        val gap = with(density) { tokens.spacing.cardPadding.toPx() }
        val tooltipY = guidePanelTop(height, panelHeight.toFloat(), rect?.top, rect?.bottom, gap)

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, tooltipY.roundToInt()) }
                .heightIn(max = maxHeight)
                .onSizeChanged { panelHeight = it.height }
                .semantics { paneTitle = step.title; liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = tokens.spacing.pageMargin),
            shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
            color = tokens.colors.surface,
            contentColor = tokens.colors.text,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(tokens.spacing.cardPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)
            ) {
                Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(step.message, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip, enabled = !busy) {
                        Text("Skip", color = tokens.colors.secondaryText)
                    }
                    DayLoopButton(onClick = onNext, enabled = !busy && canAdvance) {
                        Text(if (step.isFinal) "Got it" else step.nextActionLabel)
                    }
                }
            }
        }
    }
}
