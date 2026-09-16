package com.dayloop.app.feature.today

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.R
import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.projects.ProjectOccurrenceGenerator
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.settings.MascotGift
import com.dayloop.app.today.*
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import com.dayloop.app.ui.designsystem.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val TodayBottomNavItems = listOf(
    DayLoopBottomNavItem("today", "Today"),
    DayLoopBottomNavItem("plan", "Plan"),
    DayLoopBottomNavItem("focus", "Focus"),
    DayLoopBottomNavItem("goals", "Goals"),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TodayRoute(
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit = {},
    onAddTask: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onBottomNavItemSelected: (String) -> Unit = {},
    onEditTask: (TaskId) -> Unit = {},
    moment: TodayMoment? = null,
    refreshKey: Int = 0,
    focusMinutes: Long = 0,
    focusStreak: Int = 0,
    isFocusActive: Boolean = false,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit = { _, _ -> },
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit = { _, _, _ -> },
    onApplySuggestion: (AssistantSuggestion) -> Unit = {},
    onDismissSuggestion: (String) -> Unit = {},
    onNaturalIntent: (NaturalIntent) -> Unit = {},
    actionsEnabled: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val appGraph = LocalAppGraph.current
    val todayMoment = moment ?: remember(appGraph.timeProvider, refreshKey) {
        TodayMoment(
            instant = appGraph.timeProvider.now(),
            zoneId = ZoneId.systemDefault(),
        )
    }
    
    val tasks by appGraph.taskRepository.observeTasks().collectAsState(emptyList())
    val projects by appGraph.projectRepository.observeProjects().collectAsState(emptyList())
    val settings by appGraph.settingsRepository.settings.collectAsState(AppSettings())
    
    var dismissedSuggestions by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showAssistantModal by rememberSaveable { mutableStateOf(false) }
    var celebrating by rememberSaveable { mutableStateOf(false) }

    val uiState = remember(tasks, projects, todayMoment, focusMinutes, focusStreak, isFocusActive, settings.aiAssistantEnabled, dismissedSuggestions) {
        val projectTitles = projects.associate { it.id to it.title }
        val generatedTasks = ProjectOccurrenceGenerator()
            .generateFor(projects, todayMoment)
        
        val snapshot = TodaySnapshotBuilder.build(
            tasks = tasks,
            moment = todayMoment,
            generatedTasks = generatedTasks
        )
        
        val suggestions = if (settings.aiAssistantEnabled) {
            appGraph.todayAssistant.generateSuggestions(snapshot, todayMoment.localDateTime)
                .filter { it.id !in dismissedSuggestions }
        } else emptyList()

        TodayUiState(snapshot, projectTitles, focusMinutes, focusStreak, isFocusActive, suggestions, todayMoment.localDateTime)
    }

    // Trigger celebration when day becomes complete
    LaunchedEffect(uiState.dayStatus) {
        if (uiState.dayStatus == TodayDayStatus.COMPLETE && uiState.totalCount > 0) {
            celebrating = true
        }
    }

    val capabilities = remember(refreshKey) { appGraph.reminderNotifications.capabilities() }
    val needsRepair = !capabilities.exactAlarmsAllowed || !capabilities.notificationsAllowed

    Box {
        TodayScreen(
            uiState = uiState,
            modifier = modifier,
            onCompleteTask = onCompleteTask,
            onEditTask = onEditTask,
            onTaskActions = onTaskActions,
            onSubtaskToggle = onSubtaskToggle,
            onOpenAssistant = { showAssistantModal = true },
            onNaturalIntent = onNaturalIntent,
            onApplySuggestion = onApplySuggestion,
            onDismissSuggestion = { dismissedSuggestions = dismissedSuggestions + it },
            actionsEnabled = actionsEnabled,
            onAddTask = onAddTask,
            onOpenSettings = onOpenSettings,
            onOpenNotes = onOpenNotes,
            onBottomNavItemSelected = onBottomNavItemSelected,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            warningBanner = if (needsRepair) {
                {
                    DayLoopWarningBanner(
                        title = "Reminders need attention",
                        body = "Some permissions are missing.",
                        actionText = "Fix",
                        onAction = { onBottomNavItemSelected("more") }
                    )
                }
            } else null
        )

        CelebrationOverlay(active = celebrating, onFinished = { celebrating = false })
    }

    if (showAssistantModal) {
        val modalScope = rememberCoroutineScope()
        MascotAssistantModal(
            suggestion = uiState.assistantSuggestions.firstOrNull(),
            gifts = settings.gifts.filter { !it.isRead },
            parser = appGraph.naturalIntentParser,
            onNaturalIntent = { 
                onNaturalIntent(it)
                showAssistantModal = false
            },
            onApply = { 
                onApplySuggestion(it)
                showAssistantModal = false 
            },
            onDismiss = { 
                dismissedSuggestions = dismissedSuggestions + it.id
                showAssistantModal = false 
            },
            onMarkGiftRead = { id ->
                modalScope.launch { appGraph.settingsRepository.markGiftRead(id) }
            },
            onClose = { showAssistantModal = false }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit = {},
    onAddTask: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onBottomNavItemSelected: (String) -> Unit = {},
    warningBanner: @Composable (() -> Unit)? = null,
    onEditTask: (TaskId) -> Unit = {},
    onTaskActions: (TaskId, TaskActionMenu) -> Unit = { _, _ -> },
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit = { _, _, _ -> },
    onApplySuggestion: (AssistantSuggestion) -> Unit = {},
    onDismissSuggestion: (String) -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onNaturalIntent: (NaturalIntent) -> Unit = {},
    actionsEnabled: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    DayLoopScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DayLoopMainNav(
                selected = "today",
                onSelected = onBottomNavItemSelected,
                onAdd = onAddTask,
                onOpenSettings = onOpenSettings,
                addLabel = "Add task",
                enabled = actionsEnabled)
        },
    ) { contentPadding ->
        TodayScaffoldContent(
            uiState = uiState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            onCompleteTask = onCompleteTask,
            onEditTask = onEditTask,
            onTaskActions = onTaskActions,
            onSubtaskToggle = onSubtaskToggle,
            onApplySuggestion = onApplySuggestion,
            onDismissSuggestion = onDismissSuggestion,
            onOpenAssistant = onOpenAssistant,
            onNaturalIntent = onNaturalIntent,
            actionsEnabled = actionsEnabled,
            onAddTask = onAddTask,
            onBottomNavItemSelected = onBottomNavItemSelected,
            warningBanner = warningBanner,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TodayScaffoldContent(
    uiState: TodayUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit,
    onApplySuggestion: (AssistantSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    onOpenAssistant: () -> Unit,
    onNaturalIntent: (NaturalIntent) -> Unit,
    actionsEnabled: Boolean,
    onAddTask: () -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    warningBanner: @Composable (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val colors = LocalDayLoopColors.current

    DayLoopBox(
        modifier = modifier
            .padding(contentPadding),
        backgroundColor = colors.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                TodayTopBar(uiState = uiState, onMascotClick = onOpenAssistant)
            }
            warningBanner?.let {
                item { it() }
            }

            item {
                TodayHeroDashboard(uiState = uiState)
            }
            items(
                items = uiState.visibleSections,
                key = { section -> section.section.name },
            ) { section ->
                TodaySectionBlock(
                    section = section,
                    onCompleteTask = onCompleteTask,
                    onEditTask = onEditTask,
                    onTaskActions = onTaskActions,
                    onSubtaskToggle = onSubtaskToggle,
                    actionsEnabled = actionsEnabled,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
            if (uiState.hasCompletedSection) {
                item(key = uiState.completedSection.section.name) {
                    TodayCompletedSection(section = uiState.completedSection, onEditTask = onEditTask,
                        onTaskActions = onTaskActions, actionsEnabled = actionsEnabled)
                }
            }
        }
    }
}

@Composable
private fun TodayTopBar(
    uiState: TodayUiState,
    onMascotClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    heading()
                    contentDescription = "Today: ${uiState.dateLabel}"
                },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayLoopText(
                text = uiState.title,
                style = typography.display,
                color = colors.onBackground,
            )
            DayLoopText(
                text = uiState.dateLabel,
                style = typography.body,
                color = colors.supportingText,
            )
        }

        Box(modifier = Modifier.clickable(onClick = onMascotClick)) {
            DayLoopMascot(
                modifier = Modifier.size(48.dp),
                mood = uiState.mascotMood
            )
        }

        // Streak counter (Ref 2 style - prominent pill)
        DayLoopBox(
            backgroundColor = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DayLoopText("🔥", style = typography.body)
                DayLoopText(
                    text = uiState.streakValue,
                    style = typography.headline,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
private fun TodayHeroDashboard(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    DayLoopBox(
        modifier = modifier.fillMaxWidth(),
        shape = DayLoopShapes.MatchaCard,
        backgroundColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top section: Text details on the left, Progress Ring on the right
            Row(
                modifier = Modifier.fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = uiState.progressAccessibilityLabel
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayLoopText(
                        text = uiState.dayStatusLabel,
                        style = typography.body,
                        color = colors.primary
                    )
                    DayLoopText(
                        text = uiState.progressSummary,
                        style = typography.display,
                        color = colors.onSurface,
                    )
                    DayLoopText(
                        text = uiState.remainingLabel,
                        style = typography.body,
                        color = colors.supportingText,
                    )
                }
                TodayHeroProgressRing(uiState = uiState)
            }

            // Bottom section: Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TodayHeroStat(
                    value = uiState.taskCountValue,
                    label = uiState.taskCountMetricLabel,
                    accessibilityLabel = "${uiState.taskCountValue} ${uiState.taskCountMetricLabel}"
                )
                TodayHeroStat(
                    value = uiState.focusTimeValue,
                    label = uiState.focusTimeMetricLabel,
                    accessibilityLabel = "${uiState.focusTimeValue} of focus time"
                )
            }
        }
    }
}

@Composable
private fun TodayHeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    DayLoopBox(
        modifier = modifier
            .defaultMinSize(minWidth = 68.dp)
            .semantics(mergeDescendants = true) {
                accessibilityLabel?.let { contentDescription = it }
            },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = colors.background.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val targetVal = targetState.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    val initialVal = initialState.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    if (targetVal > initialVal) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "statRoll"
            ) { targetValue ->
                DayLoopText(
                    text = targetValue,
                    style = typography.headline,
                    color = colors.onSurface
                )
            }
            DayLoopText(
                text = label,
                style = typography.label,
                color = colors.supportingText
            )
        }
    }
}

@Composable
private fun TodayHeroProgressRing(
    uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current
    val progress = uiState.progressFraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "heroProgressFill"
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .semantics {
                contentDescription = uiState.progressAccessibilityLabel
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = animatedProgress,
                    range = 0f..1f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = colors.primary,
            trackColor = colors.onSurface.copy(alpha = 0.1f),
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round
        )
        AnimatedContent(
            targetState = uiState.progressPercentLabel,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            label = "percentRoll"
        ) { targetValue ->
            DayLoopText(
                text = targetValue,
                style = typography.headline,
                color = colors.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}



@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TodaySectionBlock(
    section: TodaySectionUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit,
    actionsEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = section.accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TodaySectionHeader(section = section)
        section.items.forEach { item ->
            key(item.taskId) {
            TodayTaskRow(
                section = section.section,
                item = item,
                onCompleteTask = onCompleteTask,
                onEditTask = onEditTask,
                onTaskActions = onTaskActions,
                onSubtaskToggle = onSubtaskToggle,
                actionsEnabled = actionsEnabled,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            }
        }
    }
}

@Composable
private fun TodayCompletedSection(
    section: TodaySectionUiState,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = section.accessibilityLabel },
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            DayLoopTextButton(
                text = if (expanded) "Hide" else "Show",
                onClick = { expanded = !expanded }
            )
        }
        if (expanded) {
            section.items.forEach { item ->
                key(item.taskId) {
                    TodayCompletedTaskRow(item = item, onEditTask = onEditTask,
                        onTaskActions = onTaskActions, actionsEnabled = actionsEnabled)
                }
            }
        }
    }
}

@Composable
private fun TodaySectionHeader(
    section: TodaySectionUiState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayLoopText(
            text = section.title,
            style = typography.headline,
            color = colors.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        DayLoopText(
            text = section.countLabel,
            style = typography.label,
            color = colors.supportingText,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun StrikethroughBox(
    completed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalDayLoopColors.current
    val progress by animateFloatAsState(if (completed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "strikeProgress")
    val color = colors.onSurface.copy(alpha = 0.4f)
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        content()
        Canvas(Modifier.matchParentSize().padding(top = 2.dp)) {
            val y = size.height / 2f
            if (progress > 0f) {
                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(size.width * progress, y),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TodayTaskRow(
    section: TodaySection,
    item: TodayTaskUiState,
    modifier: Modifier = Modifier,
    onCompleteTask: (TaskId) -> Unit,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit,
    actionsEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current
    
    val rowColor = when (section) {
        TodaySection.NOW -> colors.primary.copy(alpha = 0.15f)
        TodaySection.OVERDUE -> colors.error.copy(alpha = 0.15f)
        else -> colors.surface
    }

    var expanded by rememberSaveable(item.taskId.value) { mutableStateOf(false) }

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TodayTimelineDot(section = section)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    StrikethroughBox(completed = false) {
                        DayLoopText(
                            text = item.title,
                            style = typography.body,
                            color = colors.onSurface,
                            modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "text-${item.taskId.value}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier
                        )
                    }
                    item.projectTitle?.let {
                        DayLoopText(
                            text = it,
                            style = typography.label,
                            color = colors.primary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayLoopText(
                            text = section.supportingLabel(),
                            style = typography.label,
                            color = colors.supportingText,
                        )
                        if (item.hasSubtasks) {
                            DayLoopText(
                                text = "•  " + item.subtaskProgressLabel,
                                style = typography.label,
                                color = if (item.completedSubtaskCount == item.totalSubtaskCount) colors.primary else colors.supportingText
                            )
                        }
                    }
                }
                if (item.hasSubtasks) {
                    Box(modifier = Modifier.size(24.dp).clickable { expanded = !expanded }, contentAlignment = Alignment.Center) {
                        DayLoopIcon(
                            resource = if (expanded) R.drawable.ic_action_close else R.drawable.ic_action_add,
                            description = if (expanded) "Collapse subtasks" else "Expand subtasks",
                            tint = colors.supportingText,
                            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (expanded) 0f else 45f }
                        )
                    }
                }
                TodayScheduleChip(label = section.chipLabel())
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.subtasks.forEach { subtask ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onSubtaskToggle(item.taskId, subtask.id, !subtask.isCompleted) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Custom Checkbox
                            DayLoopBox(
                                modifier = Modifier.size(20.dp),
                                shape = RoundedCornerShape(6.dp),
                                backgroundColor = if (subtask.isCompleted) colors.primary else Color.Transparent,
                                contentAlignment = Alignment.Center
                            ) {
                                if (!subtask.isCompleted) {
                                    DayLoopBox(
                                        modifier = Modifier.size(18.dp),
                                        shape = RoundedCornerShape(5.dp),
                                        backgroundColor = colors.surface,
                                        content = {}
                                    )
                                } else {
                                    DayLoopIcon(R.drawable.ic_action_check, modifier = Modifier.size(14.dp), tint = colors.onPrimary)
                                }
                            }
                            DayLoopText(
                                text = subtask.title,
                                style = typography.body,
                                color = if (subtask.isCompleted) colors.supportingText else colors.onSurface,
                                modifier = Modifier.graphicsLayer { alpha = if (subtask.isCompleted) 0.6f else 1f }
                            )
                        }
                    }
                }
            }
        }
    }

    val rowModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier.sharedBounds(
                rememberSharedContentState(key = "row-${item.taskId.value}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else modifier

    TaskGestureRow(
        modifier = rowModifier, enabled = actionsEnabled, color = rowColor,
        onEdit = { onEditTask(item.taskId) },
        onComplete = { onCompleteTask(item.taskId) },
        onTiming = { onTaskActions(item.taskId, TaskActionMenu.TIMING) },
        onOptions = { onTaskActions(item.taskId, TaskActionMenu.ALL) },
    ) {
        content()
    }
}

@Composable
private fun TodayCompletedTaskRow(
    item: TodayTaskUiState,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    TaskGestureRow(
        modifier = modifier, enabled = actionsEnabled,
        onEdit = { onEditTask(item.taskId) }, onComplete = null, onTiming = null,
        onOptions = { onTaskActions(item.taskId, TaskActionMenu.ALL) },
        color = colors.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TodayTimelineDot(section = TodaySection.COMPLETED)
            DayLoopIcon(R.drawable.ic_action_check, tint = colors.primary, description = item.completedLabel)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                StrikethroughBox(completed = true) {
                    DayLoopText(
                        text = item.title,
                        style = typography.body,
                        color = colors.supportingText,
                    )
                }
                item.projectTitle?.let {
                    DayLoopText(
                        text = it,
                        style = typography.label,
                        color = colors.primary.copy(alpha = 0.6f),
                    )
                }
                DayLoopText(
                    text = TodaySection.COMPLETED.supportingLabel(),
                    style = typography.label,
                    color = colors.supportingText.copy(alpha = 0.6f),
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
    val colors = LocalDayLoopColors.current
    val dotColor = when (section) {
        TodaySection.OVERDUE -> colors.error
        TodaySection.NOW -> colors.primary
        TodaySection.NEXT -> colors.secondary
        TodaySection.ANYTIME -> colors.supportingText.copy(alpha = 0.3f)
        TodaySection.COMPLETED -> colors.supportingText.copy(alpha = 0.2f)
    }

    DayLoopBox(
        modifier = modifier
            .width(16.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        DayLoopBox(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            backgroundColor = dotColor
        ) {}
    }
}

@Composable
private fun TodayScheduleChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current

    DayLoopBox(
        modifier = modifier.semantics {
            contentDescription = "Scheduled as $label"
        },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = colors.background.copy(alpha = 0.5f)
    ) {
        DayLoopText(
            text = label,
            style = typography.label,
            color = colors.supportingText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MascotAssistantModal(
    suggestion: AssistantSuggestion?,
    gifts: List<MascotGift> = emptyList(),
    parser: NaturalIntentParser,
    onNaturalIntent: (NaturalIntent) -> Unit,
    onApply: (AssistantSuggestion) -> Unit,
    onDismiss: (AssistantSuggestion) -> Unit,
    onMarkGiftRead: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val colors = LocalDayLoopColors.current
    val typography = LocalDayLoopTypography.current
    var chatText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DayLoopBox(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = colors.background.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DayLoopIconButton(R.drawable.ic_action_close, "Close Assistant", onClick = onClose)
                }

                DayLoopMascot(
                    modifier = Modifier.size(200.dp),
                    mood = if (suggestion != null || gifts.isNotEmpty()) MascotMood.SUGGESTING else MascotMood.EXCITED,
                    lookingDown = suggestion != null || gifts.isNotEmpty()
                )

                if (gifts.isNotEmpty()) {
                    gifts.forEach { gift ->
                        DayLoopBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = colors.primary.copy(alpha = 0.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DayLoopText(gift.title, style = typography.headline, color = colors.primary)
                                DayLoopText(gift.message, style = typography.body)
                                DayLoopTextButton(text = "Thanks! ✨", onClick = { onMarkGiftRead(gift.id) })
                            }
                        }
                    }
                }

                if (suggestion != null) {
                    DayLoopBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DayLoopShapes.MatchaCard,
                        backgroundColor = colors.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DayLoopText(
                                text = suggestion.title,
                                style = typography.headline,
                                color = colors.onSurface
                            )
                            DayLoopText(
                                text = suggestion.description,
                                style = typography.body,
                                color = colors.supportingText
                            )
                            
                            Spacer(Modifier.height(8.dp))

                            DayLoopBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onApply(suggestion) }
                                    .background(colors.primary, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DayLoopText(
                                        text = if (suggestion.suggestionType == SuggestionType.OVERDUE) "Start Now" else "Schedule Now",
                                        style = typography.headline,
                                        color = colors.onPrimary
                                    )
                                    DayLoopIcon(R.drawable.ic_action_check, tint = colors.onPrimary)
                                }
                            }

                            DayLoopTextButton(
                                text = "Dismiss suggestion",
                                color = colors.error,
                                onClick = { onDismiss(suggestion) }
                            )
                        }
                    }
                }
                
                if (suggestion == null && gifts.isEmpty()) {
                    DayLoopBox(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = colors.surface
                    ) {
                        DayLoopText(
                            text = "There are no suggestions at the moment. But I'm here to help! What's on your mind?",
                            modifier = Modifier.padding(24.dp),
                            style = typography.body,
                            color = colors.supportingText
                        )
                    }
                }
                
                // Natural Planning Chat Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DayLoopTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = "Ask me anything...",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val intent = parser.parse(chatText)
                                if (intent != null) {
                                    onNaturalIntent(intent)
                                }
                            }
                        )
                    )
                    DayLoopBox(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable(enabled = chatText.isNotBlank()) {
                                val intent = parser.parse(chatText)
                                if (intent != null) {
                                    onNaturalIntent(intent)
                                }
                            }
                            .background(if (chatText.isNotBlank()) colors.primary else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        DayLoopIcon(
                            resource = R.drawable.ic_action_forward,
                            tint = if (chatText.isNotBlank()) colors.onPrimary else colors.supportingText
                        )
                    }
                }
            }
        }
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
    com.dayloop.app.ui.designsystem.DayLoopTheme {
        TodayScreen(
            uiState = TodayUiState(
                snapshot = TodaySnapshot(
                    date = LocalDate.of(2026, 9, 2),
                    overdue = listOf(TodayItem(previewTask("overdue", "Review overdue deadline"))),
                    now = listOf(TodayItem(previewTask("now", "Focus block", projectId = "project-1"))),
                    next = listOf(TodayItem(previewTask("next", "Plan afternoon loop"))),
                    anytime = listOf(TodayItem(previewTask("anytime", "Tidy inbox"))),
                    completed = listOf(TodayItem(previewTask("done", "Log morning medication").markCompleted())),
                ),
                projectTitles = mapOf(ProjectId("project-1") to "Circuits Spaced Study")
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayEmptyScreenPreview() {
    com.dayloop.app.ui.designsystem.DayLoopTheme {
        TodayScreen(uiState = TodayUiState.empty(date = LocalDate.of(2026, 9, 2)))
    }
}

private fun previewTask(
    id: String,
    title: String,
    projectId: String? = null,
): Task = Task(
    id = TaskId(id),
    title = title,
    projectId = projectId?.let { ProjectId(it) }
)
