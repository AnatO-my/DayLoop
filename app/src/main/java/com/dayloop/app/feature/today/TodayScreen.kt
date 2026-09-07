package com.dayloop.app.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.today.TodayItem
import com.dayloop.app.today.TodayMoment
import com.dayloop.app.today.TodaySection
import com.dayloop.app.today.TodaySnapshot
import com.dayloop.app.ui.components.DayLoopBottomNav
import com.dayloop.app.ui.components.DayLoopBottomNavItem
import com.dayloop.app.ui.components.DayLoopHeroCard
import com.dayloop.app.ui.components.DayLoopMetricCard
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import java.time.LocalDate
import java.time.ZoneId

private val TodayBottomNavItems = listOf(
    DayLoopBottomNavItem("today", "Today"),
    DayLoopBottomNavItem("plan", "Plan"),
    DayLoopBottomNavItem("focus", "Focus"),
    DayLoopBottomNavItem("goals", "Goals"),
    DayLoopBottomNavItem("more", "More"),
)

@Composable
fun TodayRoute(
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit = {},
    onAddTask: () -> Unit = {},
    onBottomNavItemSelected: (String) -> Unit = {},
) {
    val appGraph = LocalAppGraph.current
    val todayMoment = remember(appGraph.timeProvider) {
        TodayMoment(
            instant = appGraph.timeProvider.now(),
            zoneId = ZoneId.systemDefault(),
        )
    }

    TodayScreen(
        uiState = TodayUiState.empty(date = todayMoment.date),
        modifier = modifier,
        onCompleteTask = onCompleteTask,
        onAddTask = onAddTask,
        onBottomNavItemSelected = onBottomNavItemSelected,
    )
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit = {},
    onAddTask: () -> Unit = {},
    onBottomNavItemSelected: (String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        TodayScaffoldContent(
            uiState = uiState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            onCompleteTask = onCompleteTask,
            onAddTask = onAddTask,
            onBottomNavItemSelected = onBottomNavItemSelected,
        )
    }
}

@Composable
private fun TodayScaffoldContent(
    uiState: TodayUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
    onAddTask: () -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Box(
        modifier = modifier
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenPadding,
                top = tokens.spacing.screenPadding,
                end = tokens.spacing.screenPadding,
                bottom = 176.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
        ) {
            item {
                TodayTopBar(uiState = uiState)
            }
            item {
                TodayHeroDashboard(uiState = uiState)
            }
            item {
                TodayMetricsRow(uiState = uiState)
            }
            if (uiState.shouldShowDayMessage) {
                item {
                    TodayDayMessage(uiState = uiState)
                }
            }
            items(
                items = uiState.visibleSections,
                key = { section -> section.section.name },
            ) { section ->
                TodaySectionBlock(
                    section = section,
                    onCompleteTask = onCompleteTask,
                )
            }
            if (uiState.hasCompletedSection) {
                item(key = uiState.completedSection.section.name) {
                    TodayCompletedSection(section = uiState.completedSection)
                }
            }
        }

        TodayBottomDock(
            uiState = uiState,
            onAddTask = onAddTask,
            onBottomNavItemSelected = onBottomNavItemSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = tokens.spacing.screenPadding, vertical = tokens.spacing.contentGap),
        )
    }
}

@Composable
private fun TodayTopBar(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TodayIconButton(contentDescription = "Open menu") {
            TodayMenuGlyph()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
        ) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = uiState.dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.supportingText,
            )
        }
        TodayIconButton(contentDescription = "Open calendar") {
            TodayCalendarGlyph()
        }
    }
}

@Composable
private fun TodayHeroDashboard(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    DayLoopHeroCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            ) {
                Text(
                    text = uiState.dayStatusLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.onActionSurface,
                )
                Text(
                    text = uiState.progressSummary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = tokens.colors.onActionSurface,
                )
                Text(
                    text = uiState.remainingLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.onActionSurface.copy(alpha = 0.72f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap)) {
                    TodayHeroStat(value = uiState.taskCountValue, label = uiState.taskCountMetricLabel)
                    TodayHeroStat(value = uiState.focusTimeValue, label = uiState.focusTimeMetricLabel)
                }
            }
            TodayHeroProgressRing(uiState = uiState)
        }
    }
}

@Composable
private fun TodayHeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.defaultMinSize(minWidth = 68.dp),
        shape = RoundedCornerShape(tokens.shapeScale.compactCorner),
        color = tokens.colors.onActionSurface.copy(alpha = 0.09f),
        contentColor = tokens.colors.onActionSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.onActionSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun TodayHeroProgressRing(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val progress = uiState.progressFraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(100.dp)
            .semantics {
                contentDescription = uiState.progressAccessibilityLabel
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = tokens.colors.onActionSurface.copy(alpha = 0.18f),
            strokeWidth = 10.dp,
        )
        Text(
            text = uiState.progressPercentLabel,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TodayMetricsRow(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        DayLoopMetricCard(
            value = uiState.streakValue,
            label = uiState.streakMetricLabel,
            modifier = Modifier.weight(1f),
            accentColor = MaterialTheme.colorScheme.primary,
        )
        DayLoopMetricCard(
            value = uiState.completedMetricValue,
            label = uiState.completedMetricLabel,
            modifier = Modifier.weight(1f),
            accentColor = MaterialTheme.colorScheme.onSurface,
        )
        DayLoopMetricCard(
            value = uiState.focusScoreValue,
            label = uiState.focusScoreMetricLabel,
            modifier = Modifier.weight(1f),
            accentColor = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun TodayDayMessage(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.expressiveCorner),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(tokens.spacing.contentGap)
                .semantics { contentDescription = uiState.dayMessageAccessibilityLabel },
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
        ) {
            Text(
                text = uiState.dayMessageTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = uiState.dayMessageBody,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.supportingText,
            )
        }
    }
}

@Composable
private fun TodaySectionBlock(
    section: TodaySectionUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = section.accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        TodaySectionHeader(section = section)
        section.items.forEach { item ->
            TodayTaskRow(
                section = section.section,
                item = item,
                onCompleteTask = onCompleteTask,
            )
        }
    }
}

@Composable
private fun TodayCompletedSection(
    section: TodaySectionUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = section.accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TodaySectionHeader(
                section = section,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.semantics {
                    contentDescription = if (expanded) {
                        section.collapseActionLabel
                    } else {
                        section.expandActionLabel
                    }
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                },
            ) {
                Text(text = if (expanded) "Hide" else "Show")
            }
        }
        if (expanded) {
            section.items.forEach { item ->
                TodayCompletedTaskRow(item = item)
            }
        }
    }
}

@Composable
private fun TodaySectionHeader(
    section: TodaySectionUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Text(
            text = section.countLabel,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.supportingText,
            modifier = Modifier.padding(start = tokens.spacing.compactGap),
        )
    }
}

@Composable
private fun TodayTaskRow(
    section: TodaySection,
    item: TodayTaskUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val rowColor = when (section) {
        TodaySection.NOW -> MaterialTheme.colorScheme.primaryContainer
        TodaySection.OVERDUE -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = rowColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.contentGap),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TodayTimelineDot(section = section)
            Checkbox(
                checked = false,
                onCheckedChange = { checked ->
                    if (checked) {
                        onCompleteTask(item.taskId)
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription = item.completeActionLabel
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = section.supportingLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.supportingText,
                )
            }
            TodayScheduleChip(label = section.chipLabel())
        }
    }
}

@Composable
private fun TodayCompletedTaskRow(
    item: TodayTaskUiState,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.contentGap),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TodayTimelineDot(section = TodaySection.COMPLETED)
            Checkbox(
                checked = true,
                enabled = false,
                onCheckedChange = null,
                modifier = Modifier.semantics {
                    contentDescription = item.completedLabel
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = TodaySection.COMPLETED.supportingLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.supportingText,
                )
            }
            TodayScheduleChip(label = TodaySection.COMPLETED.chipLabel())
        }
    }
}

@Composable
private fun TodayTimelineDot(
    section: TodaySection,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val dotColor = when (section) {
        TodaySection.OVERDUE -> tokens.colors.warning
        TodaySection.NOW -> MaterialTheme.colorScheme.primary
        TodaySection.NEXT -> MaterialTheme.colorScheme.secondary
        TodaySection.ANYTIME -> tokens.colors.progressTrack
        TodaySection.COMPLETED -> tokens.colors.supportingText.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier.width(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

@Composable
private fun TodayScheduleChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun TodayBottomDock(
    uiState: TodayUiState,
    onAddTask: () -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        Button(
            onClick = onAddTask,
            shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.colors.actionSurface,
                contentColor = tokens.colors.onActionSurface,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .semantics { contentDescription = "Add task" },
        ) {
            Text(
                text = uiState.addTaskActionLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DayLoopBottomNav(
            items = TodayBottomNavItems,
            selectedItemId = uiState.selectedBottomNavItemId,
            onItemSelected = onBottomNavItemSelected,
        )
    }
}

@Composable
private fun TodayIconButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
private fun TodayMenuGlyph(
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 2.dp.toPx()
        listOf(0.28f, 0.5f, 0.72f).forEach { y ->
            drawLine(
                color = color,
                start = Offset(size.width * 0.18f, size.height * y),
                end = Offset(size.width * 0.82f, size.height * y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TodayCalendarGlyph(
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 2.dp.toPx()
        val left = size.width * 0.2f
        val right = size.width * 0.8f
        val top = size.height * 0.25f
        val bottom = size.height * 0.78f
        drawLine(color, Offset(left, top), Offset(right, top), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, bottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right, bottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(right, bottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, size.height * 0.42f), Offset(right, size.height * 0.42f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.15f), Offset(size.width * 0.35f, size.height * 0.3f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.65f, size.height * 0.15f), Offset(size.width * 0.65f, size.height * 0.3f), strokeWidth, StrokeCap.Round)
    }
}

private fun TodaySection.supportingLabel(): String = when (this) {
    TodaySection.OVERDUE -> "Needs attention"
    TodaySection.NOW -> "In progress"
    TodaySection.NEXT -> "Coming up"
    TodaySection.ANYTIME -> "Flexible"
    TodaySection.COMPLETED -> "Finished"
}

private fun TodaySection.chipLabel(): String = when (this) {
    TodaySection.OVERDUE -> "Due"
    TodaySection.NOW -> "Now"
    TodaySection.NEXT -> "Next"
    TodaySection.ANYTIME -> "Anytime"
    TodaySection.COMPLETED -> "Done"
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    DayLoopTheme {
        TodayScreen(
            uiState = TodayUiState(
                snapshot = TodaySnapshot(
                    date = LocalDate.of(2026, 9, 2),
                    overdue = listOf(TodayItem(previewTask("overdue", "Review overdue deadline"))),
                    now = listOf(TodayItem(previewTask("now", "Focus block"))),
                    next = listOf(TodayItem(previewTask("next", "Plan afternoon loop"))),
                    anytime = listOf(TodayItem(previewTask("anytime", "Tidy inbox"))),
                    completed = listOf(TodayItem(previewTask("done", "Log morning medication").markCompleted())),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayEmptyScreenPreview() {
    DayLoopTheme {
        TodayScreen(uiState = TodayUiState.empty(date = LocalDate.of(2026, 9, 2)))
    }
}

private fun previewTask(
    id: String,
    title: String,
): Task = Task(
    id = TaskId(id),
    title = title,
)
