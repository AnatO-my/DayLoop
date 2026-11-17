package com.dayloop.app.feature.plans

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import com.dayloop.app.core.model.*
import com.dayloop.app.core.projects.GoalProgressCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.Project
import com.dayloop.app.core.model.WorkoutRoutine
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.workouts.WorkoutRoutineEditor
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.guides.GuideHost
import com.dayloop.app.ui.guides.GuideStep
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import java.time.ZoneId

@Composable
fun PlansRoute(
    onBottomNavItemSelected: (String) -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onAddTask: (ProjectId) -> Unit = {},
    onEditTask: (TaskId) -> Unit = {},
    onOpenSavedWork: () -> Unit = {},
    activeGuideStep: GuideStep? = null,
    guideNextAction: GuideAction? = null,
    guideBusy: Boolean = false,
    onGuideAction: (GuideAction) -> Unit = {}
) {
    val appGraph = LocalAppGraph.current
    val projects by appGraph.projectRepository.observeProjects().collectAsState(emptyList())
    val goals by appGraph.goalRepository.observeGoals().collectAsState(emptyList())
    val tasks by appGraph.taskRepository.observeTasks().collectAsState(emptyList())
    val routines by appGraph.workoutRoutineRepository.observeRoutines().collectAsState(emptyList())
    val preferences by appGraph.settingsRepository.settings.collectAsState(AppSettings())
    val scope = rememberCoroutineScope()
    var projectEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionIsGoal by rememberSaveable { mutableStateOf(false) }
    var workoutEditor by rememberSaveable { mutableStateOf<String?>(null) }
    fun save(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block(); projectEditor = null; goalEditor = null; actionId = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not save your changes. Try again." }
            finally { busy = false }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(PlansTab.PROJECTS) }

    GuideHost(activeStep = activeGuideStep, onAction = onGuideAction, nextAction = guideNextAction, busy = guideBusy) {
        PlansScreen(
            uiState = PlansUiState(
                projects = projects,
                goals = goals,
                selectedTab = selectedTab,
            ),
            onTabSelected = { selectedTab = it },
            onBottomNavItemSelected = { if (!busy) onBottomNavItemSelected(it) },
            onOpenNotes = onOpenNotes,
            onOpenSavedWork = { if (!busy) onOpenSavedWork() },
            tasks = tasks,
            workoutRoutines = routines,
            iconSelections = preferences.planningIcons,
            onAdd = { if (!busy) { when (selectedTab) {
                PlansTab.PROJECTS -> projectEditor = "new"
                PlansTab.GOALS -> goalEditor = "new"
                PlansTab.WORKOUTS -> workoutEditor = "new"
            } } },
            onProjectSelected = { if (!busy) projectEditor = it.id.value },
            onGoalSelected = { if (!busy) goalEditor = it.id.value },
            onProjectOptions = { if (!busy) { actionIsGoal = false; actionId = it.id.value } },
            onGoalOptions = { if (!busy) { actionIsGoal = true; actionId = it.id.value } },
            onWorkoutSelected = { if (!busy) workoutEditor = it.id.value },
        )
    }
    actionId?.let { id ->
        val project = if (!actionIsGoal) projects.firstOrNull { it.id.value == id } else null
        val goal = if (actionIsGoal) goals.firstOrNull { it.id.value == id } else null
        val archived = project?.status == ProjectStatus.ARCHIVED || goal?.status == GoalStatus.ARCHIVED
        if (project != null || goal != null) key(actionIsGoal, id) {
            PlanningActionsDialog(
                title = project?.title ?: goal!!.title, isGoal = actionIsGoal, archived = archived, busy = busy,
                onDismiss = { if (!busy) actionId = null },
                onArchive = { save {
                    if (actionIsGoal) {
                        val current = appGraph.goalRepository.getGoal(GoalId(id)) ?: error("Goal no longer exists")
                        appGraph.goalRepository.saveGoal(current.copy(status = if (archived) GoalStatus.ACTIVE else GoalStatus.ARCHIVED))
                    } else {
                        val current = appGraph.projectRepository.getProject(ProjectId(id)) ?: error("Project no longer exists")
                        appGraph.projectRepository.saveProject(current.copy(status = if (archived) ProjectStatus.ACTIVE else ProjectStatus.ARCHIVED))
                    }
                } },
                onDelete = { save {
                    if (actionIsGoal) appGraph.goalRepository.deleteGoal(GoalId(id))
                    else appGraph.projectRepository.deleteProject(ProjectId(id))
                } },
            )
        }
    }
    projectEditor?.let { id -> if (id == "new" || projects.any { it.id.value == id }) key(id) {
        ProjectEditorDialog(
            today = appGraph.timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate(),
            project = projects.firstOrNull { it.id.value == id }, goals = goals,
            tasks = tasks.filter { it.projectId?.value == id }, busy = busy,
            onDismiss = { if (!busy) projectEditor = null },
            initialIcon = preferences.planningIcons["project:$id"] ?: PlanningIcon.CALENDAR.name,
            onSave = { project, icon -> save {
                appGraph.settingsRepository.setPlanningIcon("project:${project.id.value}", icon)
                appGraph.projectRepository.saveProject(project)
            } },
            onAddTask = { projectEditor = null; onAddTask(it) },
            onEditTask = { projectEditor = null; onEditTask(it) },
        )
    } }
    goalEditor?.let { id -> if (id == "new" || goals.any { it.id.value == id }) key(id) {
        GoalEditorDialog(goal = goals.firstOrNull { it.id.value == id }, busy = busy,
            onDismiss = { if (!busy) goalEditor = null },
            initialIcon = preferences.planningIcons["goal:$id"] ?: PlanningIcon.FLAG.name,
            onSave = { goal, icon -> save {
                appGraph.settingsRepository.setPlanningIcon("goal:${goal.id.value}", icon)
                appGraph.goalRepository.saveGoal(goal)
            } })
    } }
    workoutEditor?.let { id -> if (id == "new" || routines.any { it.id.value == id }) key(id) {
        val routine = routines.firstOrNull { it.id.value == id }
        WorkoutRoutineEditor(
            routine = routine,
            busy = busy,
            onDismiss = { if (!busy) workoutEditor = null },
            onSave = { updated -> save { appGraph.workoutRoutineRepository.save(updated) } },
            onDelete = routine?.let { { save { appGraph.workoutRoutineRepository.delete(it.id) } } },
            onAddToToday = routine?.let { savedRoutine -> {
                save {
                    val exerciseSummary = savedRoutine.exercises.joinToString("; ") { exercise ->
                        "${exercise.name}: ${exercise.sets} × ${exercise.repetitions}" +
                            exercise.note.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    }
                    appGraph.alarmCoordinator.saveTask(
                        Task(
                            id = TaskId.generate(),
                            title = savedRoutine.title,
                            description = exerciseSummary,
                            schedule = TaskSchedule.AllDay(
                                appGraph.timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate(),
                            ),
                        ),
                    )
                }
            } },
        )
    } }
    error?.let { text -> AlertDialog(onDismissRequest = { error = null }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@Composable
fun PlansScreen(
    uiState: PlansUiState,
    onTabSelected: (PlansTab) -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    onOpenNotes: () -> Unit = {},
    tasks: List<Task> = emptyList(),
    workoutRoutines: List<WorkoutRoutine> = emptyList(),
    onAdd: () -> Unit = {},
    onProjectSelected: (Project) -> Unit = {},
    onGoalSelected: (Goal) -> Unit = {},
    onProjectOptions: (Project) -> Unit = {},
    onGoalOptions: (Goal) -> Unit = {},
    onWorkoutSelected: (WorkoutRoutine) -> Unit = {},
    iconSelections: Map<String, String> = emptyMap(),
    onOpenSavedWork: () -> Unit = {},
) {
    val tokens = MaterialTheme.dayLoopTokens
    var showArchived by rememberSaveable(uiState.selectedTab) { mutableStateOf(false) }
    val visibleProjects = uiState.projects.filter { showArchived || it.status != ProjectStatus.ARCHIVED }
    val visibleGoals = uiState.goals.filter { showArchived || it.status != GoalStatus.ARCHIVED }

    Scaffold(
        bottomBar = {
            DayLoopMainNav(
                selected = "plan",
                onSelected = onBottomNavItemSelected,
                onAdd = onAdd
            )
        }
    ) { contentPadding ->
        DayLoopScreen(modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)) {
                // Header
                DayLoopPageHeader("P&Gs", { onBottomNavItemSelected("more") })

                TextButton(onClick = onOpenSavedWork, contentPadding = PaddingValues(0.dp)) {
                    Text("Saved work (${tasks.count { it.status == TaskStatus.SOMEDAY || it.status == TaskStatus.SKIPPED }})")
                }

                DayLoopPillTabRow(
                    options = listOf(DayLoopPillOption("PROJECTS", "Projects"), DayLoopPillOption("GOALS", "Goals"), DayLoopPillOption("WORKOUTS", "Workouts")),
                    selectedOptionId = uiState.selectedTab.name,
                    onOptionSelected = { onTabSelected(PlansTab.valueOf(it)) },
                    modifier = Modifier.guideTarget(GuideTargets.PLANS_TABS),
                    selectedContainerColor = MaterialTheme.dayLoopTokens.colors.accent,
                    selectedLabelColor = MaterialTheme.dayLoopTokens.colors.accentText
                )
                if (uiState.selectedTab != PlansTab.WORKOUTS) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Show archived", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Switch(showArchived, onCheckedChange = { showArchived = it })
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)
                ) {
                    if (uiState.selectedTab == PlansTab.PROJECTS) {
                        items(visibleProjects, key = { it.id.value }) { project ->
                            val projectTasks = tasks.filter { it.projectId == project.id }
                            val completed = projectTasks.count { it.status == TaskStatus.COMPLETED }
                            val progress = if (projectTasks.isEmpty()) 0f else completed.toFloat() / projectTasks.size

                            ProjectCard(
                                name = project.title,
                                progress = progress,
                                effortLabel = androidx.compose.ui.res.pluralStringResource(
                                    com.dayloop.app.R.plurals.project_task_progress, projectTasks.size, completed, projectTasks.size),
                                icon = {
                                    DayLoopIcon(
                                        resource = PlanningIcon.resolve(iconSelections["project:${project.id.value}"], PlanningIcon.CALENDAR).drawable,
                                        tint = tokens.colors.accent
                                    )
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                                    .combinedClickable(
                                        onClick = { onProjectSelected(project) },
                                        onLongClick = { onProjectOptions(project) }
                                    )
                            )
                        }
                    } else if (uiState.selectedTab == PlansTab.GOALS) {
                        items(visibleGoals, key = { it.id.value }) { goal ->
                            val related = uiState.projects.filter { it.goalId == goal.id }
                            val ids = related.map { it.id }.toSet()
                            val progress = GoalProgressCalculator().calculate(goal, tasks.filter { it.projectId in ids }, related)
                            
                            GoalItemRow(
                                goal = goal,
                                progress = progress,
                                icon = PlanningIcon.resolve(iconSelections["goal:${goal.id.value}"], PlanningIcon.FLAG),
                                onClick = { onGoalSelected(goal) },
                                onLongClick = { onGoalOptions(goal) }
                            )
                        }
                    } else {
                        if (workoutRoutines.isEmpty()) item {
                            Text("No routines yet. Use + to create a workout routine.", style = MaterialTheme.typography.bodyMedium)
                        }
                        items(workoutRoutines, key = { it.id.value }) { routine ->
                            DayLoopHeroCard(modifier = Modifier
                                .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                                .clickable { onWorkoutSelected(routine) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(routine.title, style = MaterialTheme.typography.titleLarge)
                                    Text(routine.exercises.joinToString { "${it.name}: ${it.sets} × ${it.repetitions}" }, style = MaterialTheme.typography.bodyMedium)
                                    Text(routine.scheduledDays.sortedBy { it.value }.joinToString { it.name.lowercase().replaceFirstChar(Char::uppercase) }, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalItemRow(
    goal: Goal,
    progress: Double,
    icon: PlanningIcon,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens
    DayLoopHeroCard(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.cardPadding)
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(tokens.colors.accentSubtle), contentAlignment = Alignment.Center) {
                DayLoopIcon(icon.drawable, tint = tokens.colors.accentText)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                        color = tokens.colors.accent,
                        trackColor = tokens.colors.progressTrack,
                        strokeCap = StrokeCap.Round
                    )
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600)
                }
            }
        }
    }
}
