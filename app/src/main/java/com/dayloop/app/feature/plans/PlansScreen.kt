package com.dayloop.app.feature.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.Project
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun PlansRoute(
    onBottomNavItemSelected: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onAddTask: (ProjectId) -> Unit = {},
    onEditTask: (TaskId) -> Unit = {},
) {
    val appGraph = LocalAppGraph.current
    val projects by appGraph.projectRepository.observeProjects().collectAsState(emptyList())
    val goals by appGraph.goalRepository.observeGoals().collectAsState(emptyList())
    val tasks by appGraph.taskRepository.observeTasks().collectAsState(emptyList())
    val preferences by appGraph.settingsRepository.settings.collectAsState(com.dayloop.app.settings.AppSettings())
    val scope = rememberCoroutineScope()
    var projectEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionIsGoal by rememberSaveable { mutableStateOf(false) }
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

    PlansScreen(
        uiState = PlansUiState(
            projects = projects,
            goals = goals,
            selectedTab = selectedTab
        ),
        onTabSelected = { selectedTab = it },
        onBottomNavItemSelected = { if (!busy) onBottomNavItemSelected(it) },
        onOpenSettings = onOpenSettings,
        onOpenNotes = onOpenNotes,
        tasks = tasks,
        iconSelections = preferences.planningIcons,
        onAdd = { if (!busy) { if (selectedTab == PlansTab.PROJECTS) projectEditor = "new" else goalEditor = "new" } },
        onProjectSelected = { if (!busy) projectEditor = it.id.value },
        onGoalSelected = { if (!busy) goalEditor = it.id.value },
        onProjectOptions = { if (!busy) { actionIsGoal = false; actionId = it.id.value } },
        onGoalOptions = { if (!busy) { actionIsGoal = true; actionId = it.id.value } },
    )
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
            today = appGraph.timeProvider.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
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
    error?.let { text -> AlertDialog(onDismissRequest = { error = null }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@Composable
fun PlansScreen(
    uiState: PlansUiState,
    onTabSelected: (PlansTab) -> Unit,
    onBottomNavItemSelected: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    tasks: List<Task> = emptyList(),
    onAdd: () -> Unit = {},
    onProjectSelected: (Project) -> Unit = {},
    onGoalSelected: (Goal) -> Unit = {},
    onProjectOptions: (Project) -> Unit = {},
    onGoalOptions: (Goal) -> Unit = {},
    iconSelections: Map<String, String> = emptyMap(),
) {
    val tokens = MaterialTheme.dayLoopTokens
    var showArchived by rememberSaveable(uiState.selectedTab) { mutableStateOf(false) }
    val visibleProjects = uiState.projects.filter { showArchived || it.status != ProjectStatus.ARCHIVED }
    val visibleGoals = uiState.goals.filter { showArchived || it.status != GoalStatus.ARCHIVED }

    Scaffold(
        bottomBar = {
            com.dayloop.app.ui.components.DayLoopMainNav(
                selected = "plan",
                onSelected = onBottomNavItemSelected,
                onAdd = onAdd,
                onHoldAdd = onOpenNotes,
                onOpenSettings = onOpenSettings,
                addLabel = if (uiState.selectedTab == PlansTab.PROJECTS) "Add project" else "Add goal"
            )
        }
    ) { contentPadding ->
        DayLoopScreen(modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)) {
            Column(Modifier.fillMaxSize()) {
                // Arranged with SpaceBetween so Projects stays on the left and Goals shifts to the top right edge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.clickable { onTabSelected(PlansTab.PROJECTS) }) {
                        Text("Projects",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedTab == PlansTab.PROJECTS) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        )
                    }
                    
                    DayLoopMascot(modifier = Modifier.size(44.dp), mood = MascotMood.NEUTRAL)

                    Box(Modifier.clickable { onTabSelected(PlansTab.GOALS) }) {
                        Text("Goals",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedTab == PlansTab.GOALS) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        )
                    }
                }

                Spacer(Modifier.height(tokens.spacing.contentGap))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Show archived", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Switch(showArchived, onCheckedChange = { showArchived = it })
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap)
                ) {
                    if (uiState.selectedTab == PlansTab.PROJECTS) {
                        items(visibleProjects, key = { it.id.value }) { project ->
                            ProjectCard(project, tasks.filter { it.projectId == project.id },
                                icon = PlanningIcon.resolve(iconSelections["project:${project.id.value}"], PlanningIcon.CALENDAR),
                                onLongClick = { onProjectOptions(project) }, onClick = { onProjectSelected(project) })
                        }
                        if (visibleProjects.isEmpty()) {
                            item { EmptyState("No projects yet.") }
                        }
                    } else {
                        items(visibleGoals, key = { it.id.value }) { goal ->
                            val related = uiState.projects.filter { it.goalId == goal.id }
                            val ids = related.map { it.id }.toSet()
                            val progress = GoalProgressCalculator().calculate(goal, tasks.filter { it.projectId in ids }, related)
                            GoalCard(goal, progress,
                                icon = PlanningIcon.resolve(iconSelections["goal:${goal.id.value}"], PlanningIcon.FLAG),
                                onLongClick = { onGoalOptions(goal) }, onClick = { onGoalSelected(goal) })
                        }
                        if (visibleGoals.isEmpty()) {
                            item { EmptyState("No goals yet.") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, tasks: List<Task>, icon: PlanningIcon, onLongClick: () -> Unit, onClick: () -> Unit) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        modifier = Modifier.combinedClickable(onClickLabel = "Edit project", onClick = onClick,
            onLongClickLabel = "Project options", onLongClick = onLongClick).semantics(mergeDescendants = true) {
            contentDescription = "Project: ${project.title}, Status: ${project.status.name}"
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                com.dayloop.app.ui.components.DayLoopIcon(icon.drawable)
                com.dayloop.app.ui.components.DayLoopIcon(com.dayloop.app.R.drawable.ic_action_forward)
            }
            Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(project.status.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall)
            val completed = tasks.count { it.status == TaskStatus.COMPLETED }
            Text("$completed of ${tasks.size} tasks complete", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, progress: Double, icon: PlanningIcon, onLongClick: () -> Unit, onClick: () -> Unit) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClickLabel = "Edit goal", onClick = onClick,
                onLongClickLabel = "Goal options", onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "Goal: ${goal.title}, Progress: ${goal.progressPolicy.name}"
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    com.dayloop.app.ui.components.DayLoopIcon(icon.drawable)
                }
            }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (goal.status == GoalStatus.ARCHIVED) Text("Archived", style = MaterialTheme.typography.labelSmall)
            goal.targetDate?.let {
                Text("Target: $it", style = MaterialTheme.typography.bodySmall, color = tokens.colors.supportingText)
            }
            Text("${(progress * 100).toInt()}% complete", style = MaterialTheme.typography.labelSmall, color = tokens.colors.supportingText)
            LinearProgressIndicator(progress = { progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
        }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
        DayLoopMascot(modifier = Modifier.size(100.dp), mood = MascotMood.NEUTRAL)
        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.dayLoopTokens.colors.supportingText)
    }
}
