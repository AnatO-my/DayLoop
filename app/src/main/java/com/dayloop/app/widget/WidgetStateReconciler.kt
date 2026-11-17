package com.dayloop.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.GoalStatus
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.projects.GoalProgressCalculator
import com.dayloop.app.feature.focus.FocusStore
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.today.*
import com.dayloop.app.time.TimeProvider
import com.dayloop.app.settings.SettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.ZoneId

class WidgetStateReconciler(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) {
    suspend fun startObserving() {
        val focusStore = FocusStore.get(context)
        val app = context.applicationContext as DayLoopApplication
        
        combine(
            taskRepository.observeTasks(),
            settingsRepository.settings,
            focusStore.state,
            app.appGraph.goalRepository.observeGoals(),
            app.appGraph.projectRepository.observeProjects()
        ) { tasks, settings, focus, goals, projects ->
            val moment = TodayMoment(timeProvider.now(), ZoneId.systemDefault())
            val snapshot = TodaySnapshotBuilder.build(tasks, moment,
                com.dayloop.app.core.projects.ProjectOccurrenceGenerator().generateFor(projects, moment))
            
            // Derive mood for widget
            val suggestions: List<AssistantSuggestion> = if (settings.aiAssistantEnabled) {
                app.appGraph.todayAssistant.generateSuggestions(snapshot, moment.localDateTime)
            } else emptyList()

            val isFocusActive = focus.endsAt != null
            val dayStatus = snapshot.dayStatus
            
            val mascotMood = when {
                suggestions.isNotEmpty() -> "SUGGESTING"
                isFocusActive -> "FOCUSED"
                snapshot.itemsIn(TodaySection.OVERDUE).isNotEmpty() -> "OVERWHELMED"
                dayStatus == TodayDayStatus.COMPLETE -> "ACCOMPLISHED"
                moment.localDateTime.hour >= 23 || moment.localDateTime.hour < 6 -> "SLEEPY"
                else -> "NEUTRAL"
            }

            updateTodayWidget(snapshot, settings.widgetPrivacyEnabled, mascotMood)
            updateGoalWidget(goals, tasks)
            FocusWidget().updateAll(context)
        }.collectLatest { }
    }

    private suspend fun updateTodayWidget(snapshot: TodaySnapshot, privacyEnabled: Boolean, mascotMood: String) {
        val widgetState = snapshot.toWidgetState()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(TodayWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[TodayWidget.UnresolvedCountKey] = widgetState.unresolvedCount
                prefs[TodayWidget.NowTaskTitleKey] = if (privacyEnabled) "" else (widgetState.nowTask?.title ?: "")
                prefs[TodayWidget.NextTaskTitleKey] = if (privacyEnabled) "" else (widgetState.nextTask?.title ?: "")
                prefs[TodayWidget.NowTaskIdKey] = widgetState.nowTask?.id?.value ?: ""
                prefs[TodayWidget.DateLabelKey] = widgetState.dateLabel
                prefs[TodayWidget.MascotMoodKey] = mascotMood
                prefs[TodayWidget.PrivacyEnabledKey] = privacyEnabled
            }
            TodayWidget().update(context, glanceId)
        }
    }

    private suspend fun updateGoalWidget(goals: List<Goal>, tasks: List<Task>) {
        val activeGoal = goals.filter { it.status == GoalStatus.ACTIVE }.firstOrNull()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(GoalWidget::class.java)
        
        if (glanceIds.isEmpty()) return

        val progress = if (activeGoal != null) {
            val calculator = GoalProgressCalculator()
            val projects = (context.applicationContext as DayLoopApplication).appGraph.projectRepository.observeProjects().first()
            val relatedTasks = tasks.filter { task -> 
                val project = projects.firstOrNull { it.id == task.projectId }
                project?.goalId == activeGoal.id 
            }
            calculator.calculate(activeGoal, relatedTasks, projects.filter { it.goalId == activeGoal.id })
        } else 0.0

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[GoalWidget.GoalTitleKey] = activeGoal?.title ?: "No active goal"
                prefs[GoalWidget.GoalProgressKey] = "${(progress * 100).toInt()}%"
                prefs[GoalWidget.GoalProgressValueKey] = (progress * 100).toInt().toString()
            }
            GoalWidget().update(context, glanceId)
        }
    }
}
