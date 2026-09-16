package com.dayloop.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.dayloop.app.repository.TaskRepository
import com.dayloop.app.today.TodayMoment
import com.dayloop.app.today.TodaySnapshot
import com.dayloop.app.today.TodaySnapshotBuilder
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.collectLatest
import java.time.ZoneId

class WidgetStateReconciler(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    suspend fun startObserving() {
        taskRepository.observeTasks().collectLatest { tasks ->
            val moment = TodayMoment(timeProvider.now(), ZoneId.systemDefault())
            val snapshot = TodaySnapshotBuilder.build(tasks, moment)
            updateWidget(snapshot)
        }
    }

    private suspend fun updateWidget(snapshot: TodaySnapshot) {
        val widgetState = snapshot.toWidgetState()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(TodayWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[TodayWidget.UnresolvedCountKey] = widgetState.unresolvedCount
                prefs[TodayWidget.NowTaskTitleKey] = widgetState.nowTask?.title ?: ""
                prefs[TodayWidget.NextTaskTitleKey] = widgetState.nextTask?.title ?: ""
                prefs[TodayWidget.DateLabelKey] = widgetState.dateLabel
            }
            TodayWidget().update(context, glanceId)
        }
    }
}
