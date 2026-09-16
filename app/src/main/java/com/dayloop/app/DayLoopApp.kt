@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.dayloop.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.dayloop.app.core.model.*
import com.dayloop.app.core.alarms.*
import com.dayloop.app.core.projects.ProjectOccurrenceGenerator
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.diagnostics.DiagnosticsRoute
import com.dayloop.app.feature.focus.FocusRoute
import com.dayloop.app.feature.focus.FocusStore
import com.dayloop.app.feature.focus.FocusService
import com.dayloop.app.feature.focus.FocusMode
import com.dayloop.app.feature.idle.IdleCalendarHost
import com.dayloop.app.feature.plans.PlansRoute
import com.dayloop.app.feature.plans.PlansTab
import com.dayloop.app.feature.taskeditor.TaskEditorDraft
import com.dayloop.app.feature.taskeditor.TaskEditorScreen
import com.dayloop.app.feature.today.TodayRoute
import com.dayloop.app.feature.today.TaskActionMenu
import com.dayloop.app.feature.today.TaskActionsDialog
import com.dayloop.app.feature.settings.SettingsPullHost
import com.dayloop.app.feature.settings.AdvancedSettingsScreen
import com.dayloop.app.feature.settings.NotesScreen
import com.dayloop.app.today.TodayMoment
import com.dayloop.app.today.NaturalIntent
import com.dayloop.app.today.TodayDayStatus
import com.dayloop.app.today.TodaySnapshotBuilder
import com.dayloop.app.ui.components.DayLoopScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun DayLoopApp(
    settings: com.dayloop.app.settings.AppSettings = com.dayloop.app.settings.AppSettings(onboardingCompleted = true),
    requestedTaskId: String? = null,
    requestedOccurrenceId: String? = null,
    requestedGeneration: Long = 0L,
    onEditRequestConsumed: () -> Unit = {},
    foreground: Boolean = true,
    resumeVersion: Int = 0,
) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val tasks by graph.taskRepository.observeTasks().collectAsState(initial = null)
    val occurrences by graph.scheduledOccurrenceRepository.observeOccurrences().collectAsState(emptyList())
    val projects by graph.projectRepository.observeProjects().collectAsState(emptyList())
    val focusStore = remember { FocusStore.get(graph.applicationContext) }
    val focus by focusStore.state.collectAsState(initial = null)
    var mutationVersion by remember { mutableIntStateOf(0) }
    val now by produceState(graph.timeProvider.now(), foreground, resumeVersion, focus?.endsAt, mutationVersion) {
        value = graph.timeProvider.now()
        while (foreground) {
            delay(if (focus?.endsAt != null) 1000L else 30_000L)
            value = graph.timeProvider.now()
        }
    }
    val moment = TodayMoment(now, ZoneId.systemDefault())
    val generated = remember(projects, moment.date) { ProjectOccurrenceGenerator().generateFor(projects, moment) }
    var route by rememberSaveable { mutableStateOf(if (settings.onboardingCompleted) "today" else "welcome") }
    var returnRoute by rememberSaveable { mutableStateOf("today") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionMenu by rememberSaveable { mutableStateOf(TaskActionMenu.ALL) }
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        graph.settingsRepository.recordAppOpen(graph.timeProvider.now().toEpochMilli())
    }

    LaunchedEffect(foreground, focus?.endsAt != null) {
        try { 
            if (foreground && focus?.endsAt != null) {
                FocusService.start(graph.applicationContext)
            }
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: java.io.IOException) { message = "The focus session could not be saved. Please try again." }
        catch (_: IllegalStateException) { message = "Open Focus to resume the background timer." }
        catch (_: SecurityException) { message = "The background timer could not start. Check app permissions." }
    }

    fun openEditor(id: String? = null, project: String? = null) {
        returnRoute = if (route == "editor") returnRoute else if (route == "welcome") "today" else route
        editId = id
        projectId = project
        route = "editor"
    }
    fun write(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { block(); mutationVersion++ }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "The change could not be completed. Please try again." }
            finally { busy = false }
        }
    }
    val navigate: (String) -> Unit = { destination ->
        if (!busy) {
            if (destination in listOf("today", "plan", "focus")) route = destination
            else if (destination == "more") showAdvancedSettings = true
        }
    }
    
    LaunchedEffect(requestedTaskId, requestedOccurrenceId, requestedGeneration) {
        if (requestedTaskId != null) {
            try {
                if (requestedOccurrenceId == null) openEditor(requestedTaskId)
                else if (requestedOccurrenceId.isNotBlank() && requestedGeneration > 0) {
                    val result = graph.alarmCoordinator.action(AlarmNotificationCommand(
                        AlarmNotificationAction.RESCHEDULE,
                        AlarmDelivery(ScheduledOccurrenceId(requestedOccurrenceId), requestedGeneration),
                    ))
                    if (result is AlarmNotificationActionResult.RescheduleRequested) openEditor(result.taskId.value)
                    else message = "This reminder has changed. Open the task from Today."
                }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { message = "Could not open this reminder. Please try again." }
            finally { onEditRequestConsumed() }
        }
    }
    
    // Mascot Gifts System - Milestone Checks
    LaunchedEffect(tasks, moment.date) {
        if (tasks != null && tasks!!.isNotEmpty()) {
            val snapshot = TodaySnapshotBuilder.build(tasks!!, moment)
            if (snapshot.dayStatus == TodayDayStatus.COMPLETE) {
                val currentSettings = graph.settingsRepository.settings.first()
                val todayStart = moment.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (currentSettings.gifts.none { it.title == "Perfect Day!" && it.timestamp > todayStart }) {
                    graph.settingsRepository.addGift("Perfect Day!", "You completed everything planned for today. Here is a little sparkle from me! ✨")
                }
            }
        }
    }

    BackHandler(enabled = (route != "today" || showAdvancedSettings || showNotes) && (route != "welcome" || settings.onboardingCompleted)) {
        if (!busy) {
            if (showAdvancedSettings) {
                showAdvancedSettings = false
            } else if (showNotes) {
                showNotes = false
            } else {
                route = if (route == "editor") returnRoute else "today"
            }
        }
    }

    IdleCalendarHost(moment.date, tasks.orEmpty(), enabled = route != "welcome" && !busy) {
        SettingsPullHost(onOpenSettings = { showAdvancedSettings = true }) {
            @OptIn(ExperimentalSharedTransitionApi::class)
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = route,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    },
                    label = "routeChange"
                ) { targetRoute ->
                    when (targetRoute) {
                        "welcome" -> com.dayloop.app.feature.onboarding.WelcomeScreen(
                            saving = busy,
                            onStart = { write { graph.settingsRepository.completeOnboarding(); route = "today" } },
                            onClose = if (settings.onboardingCompleted) ({ if (!busy) route = "today" }) else null,
                        )
                        "today" -> TodayRoute(
                            actionsEnabled = !busy,
                            onTaskActions = { id, menu -> if (!busy) { actionTaskId = id.value; actionMenu = menu } },
                            moment = moment,
                            refreshKey = resumeVersion,
                            focusMinutes = focus?.minutesOn(moment.date) ?: 0,
                            focusStreak = focus?.streakOn(moment.date) ?: 0,
                            isFocusActive = focus?.endsAt != null,
                            onCompleteTask = { id -> write {
                                graph.alarmCoordinator.completeTask(id, generated.firstOrNull { it.id == id })
                            } },
                            onAddTask = { if (!busy) openEditor() },
                            onOpenSettings = { showAdvancedSettings = true },
                            onOpenNotes = { showNotes = true },
                            onEditTask = { openEditor(it.value) },
                            onSubtaskToggle = { id, subId, completed -> write {
                                tasks?.firstOrNull { it.id == id }?.let { task ->
                                    val updated = task.subtasks.map { if (it.id == subId) it.copy(isCompleted = completed) else it }
                                    graph.taskRepository.saveTask(task.copy(subtasks = updated))
                                }
                            } },
                            onApplySuggestion = { suggestion -> write {
                                val task = suggestion.task
                                val nowDateTime = moment.localDateTime
                                val updatedTask = task.copy(
                                    schedule = TaskSchedule.TimeBlock(
                                        startsAt = nowDateTime,
                                        endsAt = nowDateTime.plusMinutes(25)
                                    )
                                )
                                graph.alarmCoordinator.saveTask(updatedTask)
                            } },
                            onNaturalIntent = { intent -> write {
                                when (intent) {
                                    is NaturalIntent.StartFocus -> {
                                        intent.minutes?.let { focusStore.setDuration(FocusMode.WORK, it) }
                                        focusStore.start(now)
                                        FocusService.start(graph.applicationContext)
                                        route = "focus"
                                    }
                                    is NaturalIntent.StartBreak -> {
                                        val mode = if ((focus?.sets ?: 0) % (focus?.setsUntilLongBreak ?: 4) == 0) 
                                            FocusMode.LONG_BREAK else FocusMode.SHORT_BREAK
                                        intent.minutes?.let { focusStore.setDuration(mode, it) }
                                        // Manual selection for natural intent
                                        focusStore.selectMode(mode)
                                        focusStore.start(now)
                                        FocusService.start(graph.applicationContext)
                                        route = "focus"
                                    }
                                    is NaturalIntent.AddTask -> {
                                        val task = Task(TaskId.generate(), intent.title)
                                        graph.alarmCoordinator.saveTask(task)
                                        message = "Added task: ${intent.title}"
                                    }
                                }
                            } },
                            onBottomNavItemSelected = navigate,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                        "plan" -> PlansRoute(
                            onBottomNavItemSelected = navigate,
                            onOpenSettings = { showAdvancedSettings = true },
                            onOpenNotes = { showNotes = true },
                            onAddTask = { openEditor(project = it.value) },
                            onEditTask = { openEditor(it.value) },
                        )
                        "focus" -> FocusRoute(focusStore, now, navigate, 
                            onOpenSettings = { showAdvancedSettings = true },
                            onOpenNotes = { showNotes = true })
                        "editor" -> {
                            val projected = generated.firstOrNull { it.id.value == editId }
                            val existing = tasks?.firstOrNull { it.id.value == editId }
                                ?.takeIf { projected == null || it.provenance == projected.provenance } ?: projected
                            when {
                                editId != null && tasks == null -> DayLoopScreen { CircularProgressIndicator() }
                                editId != null && existing == null -> DayLoopScreen {
                                    Column {
                                        Text("This task is no longer available.")
                                        TextButton(onClick = { route = returnRoute }) { Text("Back") }
                                    }
                                }
                                else -> key(editId, projectId) {
                                    var draft by rememberSaveable { mutableStateOf(existing?.let { TaskEditorDraft.fromTask(it) }
                                        ?: TaskEditorDraft(allDayDate = moment.date.toString())) }
                                    var chosenProject by rememberSaveable { mutableStateOf(existing?.projectId?.value ?: projectId) }
                                    val draftTaskId = rememberSaveable { TaskId.generate().value }
                                    TaskEditorScreen(
                                        today = moment.date,
                                        draft = draft,
                                        onDraftChange = { if (!busy) draft = it },
                                        existingTask = existing,
                                        createTaskId = { TaskId(draftTaskId) },
                                        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
                                        onCancel = { if (!busy) route = returnRoute },
                                        saving = busy,
                                        projects = projects,
                                        selectedProjectId = chosenProject,
                                        onProjectSelected = { chosenProject = it },
                                        onDelete = if (existing != null && existing.provenance == null) ({ confirmDelete = true }) else null,
                                        onReopen = if (existing?.status == TaskStatus.COMPLETED) ({ write {
                                            message = graph.alarmCoordinator.saveTask(existing.copy(status = TaskStatus.ACTIVE))
                                            route = returnRoute
                                        } }) else null,
                                        onTaskSaved = { task -> write {
                                            message = graph.alarmCoordinator.saveTask(task.copy(projectId = chosenProject?.let(::ProjectId)))
                                            route = returnRoute
                                        } },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    AnimatedVisibility(
        visible = showAdvancedSettings,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing))
    ) {
        AdvancedSettingsScreen(
            onDismiss = { showAdvancedSettings = false },
            refreshKey = resumeVersion
        )
    }
    AnimatedVisibility(
        visible = showNotes,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing))
    ) {
        NotesScreen(
            notes = settings.notes,
            onAddNote = { write { graph.settingsRepository.addNote(it) } },
            onDeleteNote = { write { graph.settingsRepository.deleteNote(it) } },
            onBack = { showNotes = false }
        )
    }
    actionTaskId?.let { id ->
        val projected = generated.firstOrNull { it.id.value == id }
        val task = tasks?.firstOrNull { it.id.value == id }
            ?.takeIf { projected == null || it.provenance == projected.provenance } ?: projected
        if (task != null) {
            val occurrence = occurrences.filter { it.taskId == task.id && it.status != ScheduledOccurrenceStatus.CANCELLED }
                .maxByOrNull { it.updatedAt }
            TaskActionsDialog(task, actionMenu, busy,
                canSnooze = task.reminderPolicy.snoozeAllowed && occurrence != null,
                onDismiss = { actionTaskId = null },
                onEdit = { actionTaskId = null; openEditor(task.id.value) },
                onComplete = { write {
                    graph.alarmCoordinator.completeTask(task.id, projected)
                    actionTaskId = null
                } },
                onReopen = { write {
                    message = graph.alarmCoordinator.saveTask(task.copy(status = TaskStatus.ACTIVE))
                    actionTaskId = null
                } },
                onSnooze = { write {
                    if (occurrence != null) {
                        message = when (val result = graph.alarmCoordinator.action(AlarmNotificationCommand(
                            AlarmNotificationAction.SNOOZE, AlarmDelivery(occurrence.id, occurrence.generation)))) {
                            is AlarmNotificationActionResult.Snoozed -> if (result.scheduledExact) null
                                else "Reminder snoozed. Delivery may be approximate."
                            is AlarmNotificationActionResult.SnoozeRejected -> "Snooze needs attention. Open More to repair reminders."
                            else -> "This reminder changed or cannot be snoozed. Open the task to reschedule."
                        }
                    }
                    actionTaskId = null
                } },
                onDelete = {
                    editId = task.id.value; returnRoute = route
                    actionTaskId = null; confirmDelete = true
                },
            )
        } else if (tasks != null) {
            AlertDialog(onDismissRequest = { actionTaskId = null }, text = { Text("This task is no longer available.") },
                confirmButton = { TextButton(onClick = { actionTaskId = null }) { Text("Close") } })
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { if (!busy) confirmDelete = false },
        title = { Text("Delete task?") },
        text = { Text("Its reminders will also be removed.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { write {
            editId?.let { graph.alarmCoordinator.deleteTask(TaskId(it)) }
            confirmDelete = false
            route = returnRoute
        } }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
    message?.let { text -> AlertDialog(
        onDismissRequest = { message = null }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
    ) }
}
