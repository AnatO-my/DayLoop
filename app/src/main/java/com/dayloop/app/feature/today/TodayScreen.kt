package com.dayloop.app.feature.today

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import androidx.compose.material3.ExperimentalMaterial3Api
import com.dayloop.app.core.projects.ProjectOccurrenceGenerator
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.today.*
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import com.dayloop.app.core.database.OutfitWithItems
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.outfit.OutfitSuggester
import com.dayloop.app.feature.outfit.OutfitDetailDialog
import com.dayloop.app.feature.wardrobe.WardrobeScreen
import com.dayloop.app.repository.toDomain
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.settings.GuideProgress
import com.dayloop.app.settings.GuideStatus
import com.dayloop.app.ui.guides.GuideDefinitions
import com.dayloop.app.ui.guides.GuideHost
import com.dayloop.app.ui.guides.GuideStep
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.R
import com.dayloop.app.feature.taskeditor.QuickAddBar
import com.dayloop.app.feature.taskeditor.QuickAddDraft
import com.dayloop.app.feature.taskeditor.TaskEditorDraft
import com.dayloop.app.feature.taskeditor.TaskEditorScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.dayloop.app.settings.HomePresentation
import com.dayloop.app.core.notifications.ReminderCapabilities
import android.content.Intent

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayRoute(
    moment: TodayMoment,
    refreshKey: Int,
    focusMinutes: Long,
    focusStreak: Int,
    isFocusActive: Boolean,
    onCompleteTask: (TaskId) -> Unit,
    onAddTask: () -> Unit,
    onOpenNotes: () -> Unit,
    onEditTask: (TaskId) -> Unit,
    onTaskActions: (TaskId, TaskActionMenu) -> Unit,
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit,
    onApplySuggestion: (AssistantSuggestion) -> Unit,
    onNaturalIntent: (NaturalIntent) -> Unit,
    onReviewMissed: () -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    homePresentation: HomePresentation = HomePresentation.EDITORIAL,
    actionsEnabled: Boolean,
    onImportWardrobe: () -> Unit = {},
    activeGuideStep: GuideStep? = null,
    guideNextAction: GuideAction? = null,
    guideBusy: Boolean = false,
    onGuideAction: (GuideAction) -> Unit = {},
    displayName: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val appGraph = LocalAppGraph.current
    val coroutineScope = rememberCoroutineScope()
    val tasks by appGraph.taskRepository.observeTasks().collectAsState(initial = null)
    val projects by appGraph.projectRepository.observeProjects().collectAsState(initial = emptyList())
    val projectTitles = remember(projects) { projects.associate { it.id to it.title } }
    val allOutfits by appGraph.outfitDao.observeAllOutfits().collectAsState(initial = null)
    val suggestibleItems by appGraph.outfitRepository.observeActiveWithMedia().collectAsState(initial = emptyList())
    val reminderCapabilities = remember(refreshKey) { appGraph.reminderNotifications.capabilities() }

    val todayOutfit = remember(allOutfits, moment.date) { allOutfits?.find { it.outfit.date == moment.date.toString() } }
    // Outfit ideas come from the explainable recommender; nothing is recorded until "Wear this".
    val outfitIdea = com.dayloop.app.feature.wardrobe.rememberTodayOutfitIdea(moment.date)
    val suggestionPreview = outfitIdea.current?.pieces?.map { it.item }
    var showWardrobe by remember { mutableStateOf(false) }
    var showOutfitDetail by remember { mutableStateOf(false) }
    var editingTaskId by remember { mutableStateOf<TaskId?>(null) }
    var isCreatingTask by remember { mutableStateOf(false) }

    if (showWardrobe) WardrobeScreen(onDismiss = { showWardrobe = false }, onImport = onImportWardrobe)
    if (showOutfitDetail) OutfitDetailDialog(date = moment.date, onDismiss = { showOutfitDetail = false })

    if ((editingTaskId != null || isCreatingTask) && tasks != null) {
        val existing = tasks?.firstOrNull { it.id == editingTaskId }
        var draft by remember(editingTaskId, isCreatingTask) {
            mutableStateOf(
                existing?.let { TaskEditorDraft.fromTask(it) }
                    ?: TaskEditorDraft(allDayDate = moment.date.toString())
            )
        }
        var chosenProject by remember(editingTaskId) { mutableStateOf(existing?.projectId?.value) }
        val draftTaskId = remember { TaskId.generate().value }

        Dialog(
            onDismissRequest = { editingTaskId = null; isCreatingTask = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            TaskEditorScreen(
                today = moment.date,
                draft = draft,
                onDraftChange = { draft = it },
                existingTask = existing,
                createTaskId = { TaskId(draftTaskId) },
                modifier = Modifier.fillMaxSize(),
                onCancel = { editingTaskId = null; isCreatingTask = false },
                saving = false,
                projects = projects,
                selectedProjectId = chosenProject,
                onProjectSelected = { chosenProject = it },
                onDelete = if (existing != null && existing.provenance == null) ({
                    coroutineScope.launch {
                        appGraph.taskRepository.deleteTask(existing.id)
                        editingTaskId = null
                    }
                }) else null,
                onTaskSaved = { task ->
                    coroutineScope.launch {
                        val updated = if (task.status in listOf(TaskStatus.SOMEDAY, TaskStatus.SKIPPED)) task.reopen() else task
                        appGraph.alarmCoordinator.saveTask(updated.copy(projectId = chosenProject?.let(::ProjectId)))
                        editingTaskId = null
                        isCreatingTask = false
                    }
                },
            )
        }
    }

    val generated = remember(projects, moment.date) { ProjectOccurrenceGenerator().generateFor(projects, moment) }
    
    val uiState = remember(tasks, generated, projectTitles, moment, focusMinutes, focusStreak, isFocusActive) {
        tasks?.let { t ->
            TodayUiState(
                snapshot = TodaySnapshotBuilder.build(t, moment, generated),
                projectTitles = projectTitles,
                focusMinutes = focusMinutes,
                focusStreak = focusStreak,
                isFocusActive = isFocusActive
            )
        }
    }

    uiState?.let { state ->
        TodayScreen(
            uiState = state,
            reminderCapabilities = reminderCapabilities,
            onFixReminderCapability = { capability ->
                val intent = when (capability) {
                    "notifications" -> appGraph.capabilityRepairFlow.notificationSettingsIntent()
                    "exact_alarms" -> appGraph.capabilityRepairFlow.exactAlarmSettingsIntent()
                    else -> null
                }
                intent?.let { appGraph.applicationContext.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            },
            homePresentation = homePresentation,
            displayName = displayName,
            todayOutfit = todayOutfit,
            outfitsLoaded = allOutfits != null,
            wardrobeAvailable = suggestibleItems.isNotEmpty(),
            suggestionPreview = suggestionPreview,
            outfitIdea = outfitIdea,
            activeGuideStep = activeGuideStep,
            guideNextAction = guideNextAction,
            guideBusy = guideBusy,
            onGuideAction = onGuideAction,
            onSuggestOutfit = { outfitIdea.start() },
            onWearSuggestion = { outfitIdea.wearCurrent() },
            onCancelSuggestion = { outfitIdea.cancel() },
            onRecordOutfit = { showOutfitDetail = true },
            onManageWardrobe = { showWardrobe = true },
            onImportWardrobe = onImportWardrobe,
            onCompleteTask = onCompleteTask,
            onAddTask = { isCreatingTask = true },
            onEditTask = { editingTaskId = it },
            onTaskActions = onTaskActions,
            onSubtaskToggle = onSubtaskToggle,
            onReviewMissed = onReviewMissed,
            onBottomNavItemSelected = onBottomNavItemSelected,
            actionsEnabled = actionsEnabled,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    } ?: TodaySkeletonScreen()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    reminderCapabilities: ReminderCapabilities? = null,
    onFixReminderCapability: (String) -> Unit = {},
    homePresentation: HomePresentation = HomePresentation.EDITORIAL,
    todayOutfit: OutfitWithItems? = null,
    outfitsLoaded: Boolean = true,
    wardrobeAvailable: Boolean = false,
    suggestionPreview: List<ClothingItem>? = null,
    outfitIdea: com.dayloop.app.feature.wardrobe.TodayOutfitIdeaState? = null,
    activeGuideStep: GuideStep? = null,
    guideBusy: Boolean = false,
    guideNextAction: GuideAction? = null,
    onGuideAction: (GuideAction) -> Unit = {},
    onSuggestOutfit: () -> Unit = {},
    onWearSuggestion: () -> Unit = {},
    onCancelSuggestion: () -> Unit = {},
    onRecordOutfit: () -> Unit = {},
    onManageWardrobe: () -> Unit = {},
    onImportWardrobe: () -> Unit = {},
    onCompleteTask: (TaskId) -> Unit = {},
    onAddTask: () -> Unit = {},
    onQuickTaskCreated: (Task, Boolean) -> Unit = { _, _ -> },
    onEditTask: (TaskId) -> Unit = {},
    onTaskActions: (TaskId, TaskActionMenu) -> Unit = { _, _ -> },
    onSubtaskToggle: (TaskId, String, Boolean) -> Unit = { _, _, _ -> },
    onReviewMissed: () -> Unit = {},
    onBottomNavItemSelected: (String) -> Unit = {},
    actionsEnabled: Boolean = true,
    displayName: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    if (homePresentation != HomePresentation.EDITORIAL) {
        TodayAlternatePresentation(
            presentation = homePresentation,
            uiState = uiState,
            actionsEnabled = actionsEnabled,
            onCompleteTask = onCompleteTask,
            onEditTask = onEditTask,
            onAddTask = onAddTask,
            onBottomNavItemSelected = onBottomNavItemSelected,
            onReviewMissed = onReviewMissed,
            reminderCapabilities = reminderCapabilities,
            onFixReminderCapability = onFixReminderCapability,
        )
        return
    }
    val tokens = MaterialTheme.dayLoopTokens
    var metricSheet by rememberSaveable { mutableStateOf<String?>(null) }

    metricSheet?.let { selected ->
        TodayMetricSheet(uiState, completed = selected == "completed", enabled = actionsEnabled,
            onDismiss = { metricSheet = null }, onEdit = { id ->
                metricSheet = null
                onEditTask(id)
            })
    }

    GuideHost(activeStep = activeGuideStep.takeIf { metricSheet == null }, onAction = onGuideAction,
        nextAction = guideNextAction, busy = guideBusy) {
        Scaffold(
            topBar = {
                Surface(color = tokens.colors.background) {
                    Box(Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = tokens.spacing.pageMargin, vertical = 8.dp)
                        .testTag("today-header")) {
                        DayLoopPageHeader("Today", { onBottomNavItemSelected("more") }, actionsEnabled, uiState.mascotMood,
                            mascotModifier = Modifier.guideTarget(GuideTargets.TODAY_MORE))
                    }
                }
            },
            bottomBar = {
                DayLoopMainNav(
                    selected = "today",
                    onSelected = onBottomNavItemSelected,
                    onAdd = onAddTask,
                    enabled = actionsEnabled,
                    modifier = Modifier.guideTarget(GuideTargets.TODAY_NAVIGATION)
                )
            }
        ) { contentPadding ->
            DayLoopScreen(
                modifier = Modifier.padding(top = contentPadding.calculateTopPadding()).consumeWindowInsets(contentPadding),
                contentPadding = PaddingValues(0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("today-list"),
                    contentPadding = PaddingValues(start = tokens.spacing.pageMargin, end = tokens.spacing.pageMargin,
                        top = tokens.spacing.pageMargin, bottom = contentPadding.calculateBottomPadding() + tokens.spacing.pageMargin),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
                ) {
                    // Greeting (as seen in image)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap)) {
                            Text(
                                text = displayName.trim().takeIf { it.isNotEmpty() }?.let { "Hello, $it" } ?: "Hello",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    // Quick Add Bar removed from persistent Today layout; task creation uses FAB sheet.

                    if (reminderCapabilities?.notificationsAllowed == false) {
                        item {
                            DayLoopWarningBanner(
                                title = "Reminders are off",
                                body = "DayLoop cannot show task alerts until notifications are enabled.",
                                actionText = "Fix",
                                onAction = { onFixReminderCapability("notifications") },
                            )
                        }
                    } else if (reminderCapabilities?.exactAlarmsAllowed == false) {
                        item {
                            DayLoopWarningBanner(
                                title = "Reminders may be late",
                                body = "Allow alarms and reminders for more reliable timing.",
                                actionText = "Fix",
                                onAction = { onFixReminderCapability("exact_alarms") },
                            )
                        }
                    }

                    if (uiState.hasMissedTasks) {
                        item {
                            DayLoopWarningBanner(
                                title = "Unfinished work",
                                body = "${uiState.missedCount} ${if (uiState.missedCount == 1) "task needs" else "tasks need"} a new plan.",
                                actionText = "Review",
                                onAction = { if (actionsEnabled) onReviewMissed() }
                            )
                        }
                    }

                    if (outfitsLoaded && (todayOutfit == null || suggestionPreview != null)) item(key = "outfit-prompt") {
                        TodayOutfitCard(
                            outfit = todayOutfit,
                            wardrobeAvailable = wardrobeAvailable,
                            suggestionPreview = suggestionPreview,
                            idea = outfitIdea,
                            onSuggest = onSuggestOutfit,
                            onWear = onWearSuggestion,
                            onCancel = onCancelSuggestion,
                            onRecord = onRecordOutfit,
                            onManageWardrobe = onManageWardrobe,
                            onImportWardrobe = onImportWardrobe
                        )
                    } else if (outfitsLoaded && todayOutfit != null) item(key = "wardrobe-shortcut") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            DayLoopIconButton(R.drawable.ic_wardrobe, "Wardrobe", onClick = onManageWardrobe,
                                enabled = actionsEnabled, tint = tokens.colors.accentText)
                        }
                    }

                    // Compact Daily Progress
                    item {
                        TodayProgressCard(
                            completedCount = uiState.completedCount,
                            remainingCount = uiState.remainingCount,
                            enabled = actionsEnabled,
                            onClick = { metricSheet = "remaining" },
                            modifier = Modifier.guideTarget(GuideTargets.TODAY_SUMMARY),
                        )
                    }

                    uiState.visibleSections.forEach { section ->
                        item { SectionHeader(title = section.title, count = section.count) }
                        items(section.items, key = { it.taskId.value }) { task ->
                            val isFirstTask = uiState.visibleSections.firstOrNull { it.items.isNotEmpty() }
                                ?.items?.firstOrNull()?.taskId == task.taskId
                            TaskGestureRow(enabled = actionsEnabled,
                                color = tokens.colors.surface,
                                onEdit = { onEditTask(task.taskId) },
                                onComplete = { onCompleteTask(task.taskId) },
                                onTiming = { onTaskActions(task.taskId, TaskActionMenu.TIMING) },
                                onOptions = { onTaskActions(task.taskId, TaskActionMenu.ALL) },
                                modifier = Modifier
                                    .animateItem()
                                    .then(if (isFirstTask) Modifier.guideTarget(GuideTargets.TODAY_TASK_ROW) else Modifier)) {
                                Column(Modifier.fillMaxWidth().padding(tokens.spacing.cardPadding)) {
                                    Text(task.title, color = tokens.colors.text,
                                        style = MaterialTheme.typography.titleMedium)
                                    task.projectTitle?.let { Text(it, color = tokens.colors.secondaryText) }
                                    task.subtasks.forEach { subtask ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(subtask.isCompleted,
                                                { onSubtaskToggle(task.taskId, subtask.id, it) }, enabled = actionsEnabled)
                                            Text(subtask.title, color = tokens.colors.text,
                                                modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom clearance for dock
                    item {
                        Spacer(Modifier.height(tokens.spacing.maxGap))
                    }
                }
            }
        }
    }
}


@Composable
private fun TodayTopBar(
    uiState: TodayUiState,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )
        
        DayLoopMascot(
            modifier = Modifier.size(40.dp),
            mood = uiState.mascotMood
        )
    }
}

private fun TodayItem.toTodayTaskUiState(state: TodayUiState): TodayTaskUiState {
    // This is a bit of a hack to use the existing conversion logic in TodayUiState if possible
    return state.sections.flatMap { it.items }.find { it.taskId == taskId }
        ?: state.completedSection.items.find { it.taskId == taskId }
        ?: TodayTaskUiState(taskId, task.title, false)
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    DayLoopTheme {
        TodayScreen(
            uiState = TodayUiState.empty(LocalDate.now())
        )
    }
}

@Composable
fun TodayOutfitCard(
    outfit: OutfitWithItems?,
    wardrobeAvailable: Boolean = false,
    suggestionPreview: List<ClothingItem>? = null,
    idea: com.dayloop.app.feature.wardrobe.TodayOutfitIdeaState? = null,
    onSuggest: () -> Unit,
    onWear: () -> Unit,
    onCancel: () -> Unit,
    onRecord: () -> Unit,
    onManageWardrobe: () -> Unit,
    onImportWardrobe: () -> Unit = {}
) {
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    val isEmpty = !wardrobeAvailable

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accentText),
        colors = CardDefaults.cardColors(containerColor = colors.background)
    ) {
        Column(Modifier.padding(tokens.spacing.cardPadding)) {
            if (suggestionPreview != null && idea != null) {
                com.dayloop.app.feature.wardrobe.TodayOutfitIdeaContent(idea, onWear = onWear, onCancel = onCancel)
                return@Column
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (suggestionPreview != null) "Suggested Outfit" else "Today's Outfit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (suggestionPreview == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEmpty) {
                            DayLoopIconButton(R.drawable.ic_action_restore, "Import wardrobe details", onClick = onImportWardrobe)
                        }
                        DayLoopIconButton(R.drawable.ic_wardrobe, "Wardrobe", onClick = onManageWardrobe,
                            tint = colors.accentText)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                
                Column(Modifier.weight(1f)) {
                    if (suggestionPreview != null) {
                        Text(
                            text = suggestionPreview.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (outfit != null && outfit.items.isNotEmpty()) {
                        Text(
                            text = outfit.items.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.accentText,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = if (isEmpty) "No items in wardrobe yet." else "No outfit recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            Spacer(Modifier.height(tokens.spacing.itemGap))

            if (suggestionPreview != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)
                ) {
                    DayLoopButton(
                        onClick = onWear,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Wear this")
                    }
                    DayLoopOutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (!isEmpty) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)
                ) {
                    DayLoopButton(
                        onClick = onSuggest,
                        modifier = Modifier.weight(1f),
                        enabled = !isEmpty
                    ) {
                        Text("Suggest")
                    }
                    DayLoopOutlinedButton(
                        onClick = onRecord,
                        modifier = Modifier.weight(1f),
                        enabled = !isEmpty
                    ) {
                        Text("Record")
                    }
                }
            }
        }
    }
}
