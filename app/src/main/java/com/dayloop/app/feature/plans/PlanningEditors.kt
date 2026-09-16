package com.dayloop.app.feature.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.*
import com.dayloop.app.ui.components.TemporalPickerField
import com.dayloop.app.ui.components.TemporalKind
import com.dayloop.app.ui.designsystem.DayLoopChoice
import java.time.LocalDate

@Composable
internal fun ProjectEditorDialog(
    project: Project?, goals: List<Goal>, tasks: List<Task>, busy: Boolean,
    onDismiss: () -> Unit, onSave: (Project, String) -> Unit,
    onAddTask: (ProjectId) -> Unit, onEditTask: (TaskId) -> Unit,
    today: LocalDate? = null,
    initialIcon: String = PlanningIcon.CALENDAR.name,
) {
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    var title by rememberSaveable { mutableStateOf(project?.title.orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(project?.goalId?.value.orEmpty()) }
    var status by rememberSaveable { mutableStateOf(project?.status ?: ProjectStatus.ACTIVE) }
    val id = rememberSaveable { project?.id?.value ?: ProjectId.generate().value }
    val previousCycle = project?.protocol as? SimplePhaseCycle
    var cycleEnabled by rememberSaveable { mutableStateOf(previousCycle != null) }
    var anchor by rememberSaveable { mutableStateOf(previousCycle?.anchorDate?.toString() ?: today?.toString().orEmpty()) }
    var active by rememberSaveable { mutableStateOf(previousCycle?.activeDays?.toString() ?: "5") }
    var rest by rememberSaveable { mutableStateOf(previousCycle?.restDays?.toString() ?: "2") }
    var templateTitle by rememberSaveable { mutableStateOf(previousCycle?.taskTemplate?.title.orEmpty()) }
    var templatePriority by rememberSaveable { mutableStateOf(previousCycle?.taskTemplate?.priority ?: TaskPriority.NORMAL) }
    val anchorDate = runCatching { LocalDate.parse(anchor.trim()) }.getOrNull()
    val activeDays = active.toIntOrNull()
    val restDays = rest.toIntOrNull()
    val cycleValid = !cycleEnabled || (anchorDate != null && activeDays != null && activeDays in 1..3650 &&
        restDays != null && restDays in 0..3650 && templateTitle.isNotBlank())
    val canSave = title.isNotBlank() && cycleValid && !busy && project?.version != Long.MAX_VALUE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "Add project" else "Project") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Project name") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                PlanningIconPicker(icon, !busy) { icon = it }
                DayLoopChoice("Goal", listOf("" to "No goal") + goals.map { it.id.value to it.title }, goal, { goal = it }, !busy)
                DayLoopChoice("Status", ProjectStatus.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }, status.name,
                    { status = ProjectStatus.valueOf(it) }, !busy)
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Active / rest cycle", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(cycleEnabled, onCheckedChange = { cycleEnabled = it }, enabled = !busy)
                }
                if (cycleEnabled) {
                    TemporalPickerField("Cycle starts", anchor, TemporalKind.DATE, { anchor = it },
                        enabled = !busy, error = anchorDate == null)
                    OutlinedTextField(active, { active = it }, label = { Text("Active days") }, enabled = !busy,
                        isError = activeDays == null || activeDays !in 1..3650, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(rest, { rest = it }, label = { Text("Rest days") }, enabled = !busy,
                        isError = restDays == null || restDays !in 0..3650, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(templateTitle, { templateTitle = it }, label = { Text("Generated task title") },
                        enabled = !busy, isError = templateTitle.isBlank(), modifier = Modifier.fillMaxWidth())
                    DayLoopChoice("Task priority", TaskPriority.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) },
                        templatePriority.name, { templatePriority = TaskPriority.valueOf(it) }, !busy)
                    if (cycleValid) Text("$activeDays active / $restDays rest", style = MaterialTheme.typography.labelMedium)
                }
                if (previousCycle != null) Text("Cycle changes regenerate today's project task.", style = MaterialTheme.typography.labelSmall)
                if (project != null) {
                    Text("Tasks", style = MaterialTheme.typography.titleMedium)
                    tasks.forEach { task -> TextButton(enabled = !busy, onClick = { onEditTask(task.id) }) {
                        Text(task.title + if (task.status == TaskStatus.COMPLETED) " (completed)" else "")
                    } }
                    TextButton(enabled = !busy, onClick = { onAddTask(project.id) }) { Text("Add task") }
                }
            }
        },
        confirmButton = { TextButton(enabled = canSave, onClick = {
            val base = project ?: Project(ProjectId(id), title = title.trim())
            val protocol = if (cycleEnabled) SimplePhaseCycle(anchorDate!!, activeDays!!, restDays!!,
                (previousCycle?.taskTemplate ?: TaskTemplate(templateTitle.trim())).copy(
                    title = templateTitle.trim(), priority = templatePriority)) else null
            onSave(base.copy(title = title.trim(), goalId = goal.takeIf { it.isNotBlank() }?.let(::GoalId), status = status,
                protocol = protocol, version = if (base.protocol != protocol) base.version + 1 else base.version), icon)
        }) { Text(if (busy) "Saving..." else "Save") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun GoalEditorDialog(goal: Goal?, busy: Boolean, onDismiss: () -> Unit, onSave: (Goal, String) -> Unit,
    initialIcon: String = PlanningIcon.FLAG.name) {
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    var title by rememberSaveable { mutableStateOf(goal?.title.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(goal?.targetDate?.toString().orEmpty()) }
    var policy by rememberSaveable { mutableStateOf(goal?.progressPolicy ?: GoalProgressPolicy.COUNT) }
    var status by rememberSaveable { mutableStateOf(goal?.status ?: GoalStatus.ACTIVE) }
    var manual by rememberSaveable { mutableStateOf(goal?.manualProgress?.toFloat() ?: 0f) }
    val id = rememberSaveable { goal?.id?.value ?: GoalId.generate().value }
    val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    val validDate = date.isBlank() || parsedDate != null
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Add goal" else "Goal") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Goal name") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                PlanningIconPicker(icon, !busy) { icon = it }
                TemporalPickerField("Target date", date, TemporalKind.DATE, { date = it },
                    error = !validDate, enabled = !busy, optional = true)
                DayLoopChoice("Progress", listOf("COUNT" to "Linked work", "BINARY" to "Done or not done", "MANUAL" to "Manual"),
                    policy.name, { policy = GoalProgressPolicy.valueOf(it) }, !busy)
                DayLoopChoice("Status", GoalStatus.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }, status.name,
                    { status = GoalStatus.valueOf(it) }, !busy)
                if (policy == GoalProgressPolicy.MANUAL) {
                    Text("${(manual * 100).toInt()}% complete")
                    Slider(value = manual, onValueChange = { manual = it }, enabled = !busy)
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && validDate && !busy, onClick = {
            val base = goal ?: Goal(GoalId(id), title = title.trim())
            onSave(base.copy(title = title.trim(), targetDate = parsedDate, progressPolicy = policy, status = status, manualProgress = manual.toDouble()), icon)
        }) { Text(if (busy) "Saving..." else "Save") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}
