package com.dayloop.app.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.datastore.preferences.core.*
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.projects.ProjectOccurrenceGenerator
import com.dayloop.app.today.*
import kotlinx.coroutines.flow.first
import java.time.ZoneId

class TodayWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(170.dp, 180.dp), DpSize(280.dp, 320.dp)))
    override val previewSizeMode = sizeMode

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val colors = previewWidgetColors(context)
        provideContent { Content(context, WidgetPreviewSamples.today, false, colors, preview = true) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = (context.applicationContext as DayLoopApplication).appGraph
        val initialSettings = graph.settingsRepository.settings.first()
        val tasks = graph.taskRepository.observeTasks().first()
        val projects = graph.projectRepository.observeProjects().first()
        provideContent {
            val liveTasks by remember { graph.taskRepository.observeTasks() }.collectAsState(tasks)
            val liveProjects by remember { graph.projectRepository.observeProjects() }.collectAsState(projects)
            val settings by graph.settingsRepository.settings.collectAsState(initialSettings)
            val currentMoment = TodayMoment(graph.timeProvider.now(), ZoneId.systemDefault())
            val state = TodaySnapshotBuilder.build(liveTasks, currentMoment,
                ProjectOccurrenceGenerator().generateFor(liveProjects, currentMoment)).toWidgetState()
            val colors = widgetColors(context, settings.themeMode, settings.font)
            Content(context, state, settings.widgetPrivacyEnabled, colors)
        }
    }

    @Composable
    private fun Content(context: Context, state: TodayWidgetState, hideTitles: Boolean,
        colors: WidgetColors, preview: Boolean = false) {
            val agenda = LocalSize.current.width >= 280.dp && LocalSize.current.height >= 320.dp
            Column(GlanceModifier.fillMaxSize().background(colors.background).cornerRadius(4.dp).padding(16.dp)) {
                Text("Today", if (preview) GlanceModifier else GlanceModifier.clickable(widgetOpen(context, "today")),
                    style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                if (agenda) Text(state.dateLabel, style = TextStyle(fontFamily = colors.family, color = colors.muted, fontSize = 12.sp))
                Spacer(GlanceModifier.height(8.dp))
                Text("${state.unresolvedCount} remaining", style = TextStyle(fontFamily = colors.family, color = colors.text,
                    fontSize = if (agenda) 24.sp else 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text(when {
                    state.completedCount > 0 -> "${state.completedCount} completed"
                    state.unresolvedCount == 0 -> "Nothing planned"
                    else -> "Ready for today"
                }, style = TextStyle(fontFamily = colors.family, color = colors.muted, fontSize = 12.sp), maxLines = 1)
                if (agenda) {
                    listOfNotNull(state.nowTask, state.nextTask).forEach { task ->
                        Spacer(GlanceModifier.height(8.dp))
                        AgendaRow(context, task, hideTitles, colors, preview)
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WidgetAction("+", if (preview) null else widgetOpen(context, "add"), colors, GlanceModifier.width(48.dp))
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetAction("Open", if (preview) null else widgetOpen(context, "today"), colors, GlanceModifier.defaultWeight())
                }
            }
    }

    @Composable
    private fun AgendaRow(context: Context, task: WidgetTask, hideTitles: Boolean, colors: WidgetColors,
        preview: Boolean = false) {
        Row(GlanceModifier.fillMaxWidth().background(colors.task).cornerRadius(4.dp).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().let {
                if (preview) it else it.clickable(widgetOpen(context, "task", task.id.value))
            }) {
                Text(task.section, style = TextStyle(fontFamily = colors.family, color = colors.onTask, fontSize = 11.sp), maxLines = 1)
                Text(if (hideTitles) "Task details hidden" else task.title,
                    style = TextStyle(fontFamily = colors.family, color = colors.onTask, fontSize = 14.sp), maxLines = 1)
            }
            if (!hideTitles) WidgetAction("Done", if (preview) null else actionRunCallback<CompleteTaskCallback>(
                actionParametersOf(TaskIdParam to task.id.value)), colors)
        }
    }

    // Retain cached keys for older widget instances and the existing refresh coordinator.
    companion object {
        val UnresolvedCountKey = intPreferencesKey("unresolved_count")
        val NowTaskTitleKey = stringPreferencesKey("now_task_title")
        val NextTaskTitleKey = stringPreferencesKey("next_task_title")
        val NowTaskIdKey = stringPreferencesKey("now_task_id")
        val DateLabelKey = stringPreferencesKey("date_label")
        val MascotMoodKey = stringPreferencesKey("mascot_mood")
        val PrivacyEnabledKey = booleanPreferencesKey("privacy_enabled")
        val TaskIdParam = ActionParameters.Key<String>("taskId")
    }
}

class CompleteTaskCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TodayWidget.TaskIdParam] ?: return
        val graph = (context.applicationContext as DayLoopApplication).appGraph
        // Privacy is rechecked at execution time, not only when drawing the button.
        if (graph.settingsRepository.settings.first().widgetPrivacyEnabled) return
        val moment = TodayMoment(graph.timeProvider.now(), ZoneId.systemDefault())
        val generated = ProjectOccurrenceGenerator().generateFor(graph.projectRepository.observeProjects().first(), moment)
        val recurring = com.dayloop.app.core.recurrence.RecurrenceOccurrenceGenerator()
            .generateFor(graph.taskRepository.observeTasks().first(), moment)
        graph.alarmCoordinator.completeTask(TaskId(taskId), (generated + recurring).firstOrNull { it.id.value == taskId })
        TodayWidget().updateAll(context)
        GoalWidget().updateAll(context)
    }
}
