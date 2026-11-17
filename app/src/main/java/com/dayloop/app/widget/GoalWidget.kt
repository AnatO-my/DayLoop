package com.dayloop.app.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.core.model.GoalStatus
import com.dayloop.app.core.projects.GoalProgressCalculator
import kotlinx.coroutines.flow.first

class GoalWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(170.dp, 220.dp), DpSize(280.dp, 220.dp)))
    override val previewSizeMode = sizeMode

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val colors = previewWidgetColors(context)
        provideContent { Content(context, WidgetPreviewSamples.goalTitle, WidgetPreviewSamples.goalProgress, colors, preview = true) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = (context.applicationContext as DayLoopApplication).appGraph
        val initialSettings = graph.settingsRepository.settings.first()
        val initialGoals = graph.goalRepository.observeGoals().first()
        val initialProjects = graph.projectRepository.observeProjects().first()
        val initialTasks = graph.taskRepository.observeTasks().first()
        provideContent {
            val liveGoals by remember { graph.goalRepository.observeGoals() }.collectAsState(initialGoals)
            val liveProjects by remember { graph.projectRepository.observeProjects() }.collectAsState(initialProjects)
            val liveTasks by remember { graph.taskRepository.observeTasks() }.collectAsState(initialTasks)
            val settings by graph.settingsRepository.settings.collectAsState(initialSettings)
            val goal = liveGoals.firstOrNull { it.status == GoalStatus.ACTIVE }
            val projects = liveProjects.filter { goal != null && it.goalId == goal.id }
            val projectIds = projects.map { it.id }.toSet()
            val progress = goal?.let { GoalProgressCalculator().calculate(it,
                liveTasks.filter { task -> task.projectId in projectIds }, projects) }?.toFloat()?.coerceIn(0f, 1f) ?: 0f
            val colors = widgetColors(context, settings.themeMode, settings.font)
            Content(context,
                if (goal == null) "No active goal" else if (settings.widgetPrivacyEnabled) "Goal details hidden" else goal.title,
                progress, colors)
        }
    }

    @Composable
    private fun Content(context: Context, title: String, progress: Float, colors: WidgetColors,
        preview: Boolean = false) {
            Column(GlanceModifier.fillMaxSize().background(colors.background).cornerRadius(4.dp).padding(16.dp)) {
                Text("Goal", style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(8.dp))
                Text(title,
                    style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 16.sp), maxLines = 2)
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(progress = progress, modifier = GlanceModifier.fillMaxWidth(),
                    color = colors.accent, backgroundColor = colors.muted)
                Text("${(progress * 100).toInt()}%", style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 16.sp))
                Spacer(GlanceModifier.defaultWeight())
                WidgetAction("P&Gs", if (preview) null else widgetOpen(context, "plan"), colors, GlanceModifier.fillMaxWidth())
            }
    }

    companion object {
        val GoalTitleKey = stringPreferencesKey("goal_title")
        val GoalProgressKey = stringPreferencesKey("goal_progress")
        val GoalProgressValueKey = stringPreferencesKey("goal_progress_value")
    }
}
