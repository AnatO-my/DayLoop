package com.dayloop.app.feature.taskeditor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayloop.app.R
import com.dayloop.app.core.model.*
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.settings.*
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.guides.GuideHost
import com.dayloop.app.ui.guides.GuideStep
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.ui.theme.dayLoopTokens
import androidx.compose.ui.tooling.preview.Preview
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(draft: TaskEditorDraft, onDraftChange: (TaskEditorDraft) -> Unit, onTaskSaved: (Task) -> Unit,
    modifier: Modifier = Modifier, existingTask: Task? = null, onCancel: () -> Unit = {},
    createTaskId: () -> TaskId = { TaskId.generate() }, saving: Boolean = false,
    projects: List<Project> = emptyList(), selectedProjectId: String? = null, onProjectSelected: (String?) -> Unit = {},
    onDelete: (() -> Unit)? = null, onReopen: (() -> Unit)? = null, onEditSeries: ((TaskId) -> Unit)? = null,
    today: LocalDate? = null,
    activeGuideStep: GuideStep? = null,
    guideNextAction: GuideAction? = null,
    guideBusy: Boolean = false,
    onGuideAction: (GuideAction) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null) {
    val tokens = MaterialTheme.dayLoopTokens
    var showOptions by remember { mutableStateOf(false) }

    fun submit() {
        if (saving) return
        when (val result = if (existingTask == null) draft.createTask(createTaskId) else draft.applyTo(existingTask)) {
            is TaskEditorSubmitResult.Invalid -> { onDraftChange(result.draft); showOptions = true }
            is TaskEditorSubmitResult.Saved -> onTaskSaved(result.task)
        }
    }

    GuideHost(activeStep = activeGuideStep, onAction = onGuideAction, nextAction = guideNextAction, busy = guideBusy) {
        Surface(
            modifier = modifier.then(if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "row-${existingTask?.id?.value ?: "new"}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else Modifier),
            color = tokens.colors.background
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayLoopIconButton(resource = R.drawable.ic_action_back, label = "Back", onClick = onCancel, tint = tokens.colors.text)
                    Text("Task Details", style = MaterialTheme.typography.labelLarge, color = tokens.colors.secondaryText)
                    DayLoopMascot(modifier = Modifier.size(36.dp), mood = MascotMood.NEUTRAL)
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = tokens.spacing.pageMargin),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
                ) {
                    // Title
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BasicTextField(
                            value = draft.title,
                            onValueChange = { onDraftChange(draft.onTitleChanged(it)) },
                            textStyle = MaterialTheme.typography.displayMedium.copy(color = tokens.colors.text, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().guideTarget(GuideTargets.EDITOR_TITLE),
                            enabled = !saving,
                            cursorBrush = SolidColor(tokens.colors.text),
                            decorationBox = { innerTextField ->
                                if (draft.title.isEmpty()) Text("Task Title", style = MaterialTheme.typography.displayMedium, color = tokens.colors.mutedText)
                                innerTextField()
                            }
                        )

                        // Due Date Row
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_nav_plan), contentDescription = null, tint = tokens.colors.secondaryText, modifier = Modifier.size(20.dp))
                            val dueText = if (draft.deadlineEnabled) "Due Date: ${draft.deadlineDate}" else "Planning: ${draft.allDayDate}"
                            Text(dueText, style = MaterialTheme.typography.bodyMedium, color = tokens.colors.secondaryText)
                        }
                    }

                    // Description card uses the shared editorial surface treatment.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                        color = tokens.colors.raised,
                        contentColor = tokens.colors.text
                    ) {
                        Column(Modifier.padding(tokens.spacing.cardPadding), verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)) {
                            // Handle bar (dash)
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(32.dp, 4.dp).clip(CircleShape).background(tokens.colors.decorativeBorder))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Description", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                DayLoopIconButton(resource = R.drawable.ic_nav_more, label = "Task settings", onClick = { showOptions = true }, tint = tokens.colors.text,
                                    modifier = Modifier.guideTarget(GuideTargets.EDITOR_SETTINGS))
                            }
                            
                            BasicTextField(
                                value = draft.notes ?: "",
                                onValueChange = { onDraftChange(draft.copy(notes = it)) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                modifier = Modifier.fillMaxWidth().guideTarget(GuideTargets.EDITOR_DESCRIPTION),
                                enabled = !saving,
                                decorationBox = { innerTextField ->
                                    if ((draft.notes ?: "").isEmpty()) Text("No description added...", color = tokens.colors.mutedText)
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Sub Tasks Section
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.guideTarget(GuideTargets.EDITOR_SUBTASKS)) {
                        Text("Sub Tasks", style = MaterialTheme.typography.headlineSmall, color = tokens.colors.text, fontWeight = FontWeight.Bold)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            draft.subtasks.forEachIndexed { index, subtask ->
                                SubtaskRow(
                                    title = subtask.title,
                                    isCompleted = subtask.isCompleted,
                                    onToggle = { checked ->
                                        onDraftChange(draft.onSubtasksChanged(draft.subtasks.toMutableList().apply { set(index, subtask.copy(isCompleted = checked)) }))
                                    },
                                    onRemove = {
                                        onDraftChange(draft.onSubtasksChanged(draft.subtasks.toMutableList().apply { removeAt(index) }))
                                    },
                                    enabled = !saving
                                )
                            }
                            
                            // Add task to list button (bottom matching image)
                            var newSubtaskTitle by remember { mutableStateOf("") }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                               OutlinedTextField(
                                   value = newSubtaskTitle,
                                   onValueChange = { newSubtaskTitle = it },
                                   modifier = Modifier.weight(1f),
                                   placeholder = { Text("New subtask...") },
                                   enabled = !saving,
                                   singleLine = true,
                                   shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
                                   colors = OutlinedTextFieldDefaults.colors(
                                       focusedTextColor = tokens.colors.text,
                                       unfocusedTextColor = tokens.colors.text,
                                       focusedContainerColor = tokens.colors.surface,
                                       unfocusedContainerColor = tokens.colors.surface,
                                       focusedBorderColor = tokens.colors.accent,
                                       unfocusedBorderColor = tokens.colors.controlBorder
                                   )
                               )
                               DayLoopIconButton(
                                   resource = R.drawable.ic_action_add,
                                   label = "Add subtask",
                                   enabled = !saving && newSubtaskTitle.isNotBlank(),
                                   onClick = {
                                       onDraftChange(draft.onSubtasksChanged(draft.subtasks + Subtask(UUID.randomUUID().toString(), newSubtaskTitle.trim())))
                                       newSubtaskTitle = ""
                                   },
                                   tint = tokens.colors.accent
                               )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(100.dp)) // Clearance
                }
                
                listOfNotNull(draft.errorMessage, draft.deadlineErrorMessage, draft.timeBlockErrorMessage,
                    draft.intervalErrorMessage, draft.recurrenceErrorMessage, draft.reminderPolicyErrorMessage)
                    .forEach { Text(it, color = tokens.colors.error, modifier = Modifier.padding(horizontal = tokens.spacing.pageMargin)) }
                // Floating Action Button at Bottom (Matching image "Add task to list")
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(tokens.spacing.pageMargin).height(48.dp).clip(RoundedCornerShape(tokens.shapeScale.inputCorner)).clickable(enabled = !saving && draft.canSubmit) { submit() }
                        .guideTarget(GuideTargets.EDITOR_SAVE),
                    color = tokens.colors.accent,
                    contentColor = tokens.colors.onAccent
                ) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                       DayLoopIcon(R.drawable.ic_action_add, tint = tokens.colors.onAccent)
                       Spacer(Modifier.width(8.dp))
                       Text("Save task changes", style = MaterialTheme.typography.labelLarge, color = tokens.colors.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showOptions) {
        ModalBottomSheet(onDismissRequest = { showOptions = false }) {
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(tokens.spacing.pageMargin), verticalArrangement = Arrangement.spacedBy(tokens.spacing.cardPadding)) {
                Text("Task Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                TaskSettingsFields(draft, !saving, onDraftChange, projects, selectedProjectId, onProjectSelected)
                listOfNotNull(draft.errorMessage, draft.deadlineErrorMessage, draft.timeBlockErrorMessage,
                    draft.intervalErrorMessage, draft.recurrenceErrorMessage, draft.reminderPolicyErrorMessage)
                    .forEach { Text(it, color = tokens.colors.error) }
                
                if (existingTask?.provenance?.originType == "RECURRENCE_RULE" && onEditSeries != null) {
                    TextButton(onClick = { onEditSeries(TaskId(existingTask.provenance.ruleId)) }) {
                        Text("Edit series (master task)")
                    }
                }

                TextButton(onClick = ::submit, enabled = !saving && draft.canSubmit) { Text(if (existingTask?.provenance != null) "Save this occurrence" else "Save task") }
                if (onReopen != null) TextButton(onClick = onReopen, enabled = !saving) { Text("Reopen task") }
                
                if (onDelete != null) TextButton(enabled = !saving, onClick = { onDelete(); showOptions = false }, colors = ButtonDefaults.textButtonColors(contentColor = tokens.colors.error)) {
                    DayLoopIcon(R.drawable.ic_action_delete, tint = tokens.colors.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Task")
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
internal fun SubtaskRow(title: String, isCompleted: Boolean, onToggle: (Boolean) -> Unit, onRemove: () -> Unit, enabled: Boolean) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
        color = tokens.colors.raised
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) tokens.colors.accent else Color.Transparent)
                    .border(2.dp, tokens.colors.controlBorder, CircleShape)
                    .clickable(enabled = enabled) { onToggle(!isCompleted) },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) DayLoopIcon(R.drawable.ic_action_check, tint = tokens.colors.onAccent, modifier = Modifier.size(14.dp))
            }
            
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.text,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            DayLoopIconButton(resource = R.drawable.ic_action_close, label = "Remove", onClick = onRemove, tint = tokens.colors.mutedText, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ToggleField(label: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked, change, enabled = enabled)
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskEditorScreenNewTaskPreview() {
    DayLoopTheme(darkTheme = true) {
        TaskEditorScreen(
            draft = TaskEditorDraft(
                title = "",
                notes = "",
                deadlineEnabled = false,
                allDayDate = LocalDate.now().toString()
            ),
            onDraftChange = {},
            onTaskSaved = {},
            onCancel = {},
            projects = listOf(
                Project(ProjectId("1"), title = "Work"),
                Project(ProjectId("2"), title = "Personal")
            ),
            today = LocalDate.now()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskEditorScreenExistingTaskPreview() {
    DayLoopTheme(darkTheme = true) {
        TaskEditorScreen(
            draft = TaskEditorDraft(
                title = "Finish project proposal",
                notes = "Review the budget and timeline sections.",
                subtasks = listOf(
                    Subtask("1", "Review budget", true),
                    Subtask("2", "Update timeline", false),
                    Subtask("3", "Send to manager", false)
                ),
                deadlineEnabled = true,
                deadlineDate = LocalDate.now().plusDays(1).toString()
            ),
            onDraftChange = {},
            onTaskSaved = {},
            onCancel = {},
            existingTask = Task(
                id = TaskId.generate(),
                title = "Finish project proposal",
                status = TaskStatus.ACTIVE
            ),
            projects = listOf(
                Project(ProjectId("1"), title = "Work"),
                Project(ProjectId("2"), title = "Personal")
            ),
            selectedProjectId = "1",
            today = LocalDate.now()
        )
    }
}
