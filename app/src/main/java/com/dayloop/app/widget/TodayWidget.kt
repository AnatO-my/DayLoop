package com.dayloop.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.action.Action
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.MainActivity
import com.dayloop.app.R
import com.dayloop.app.feature.focus.FocusService
import com.dayloop.app.feature.focus.FocusStore
import java.time.Instant

class TodayWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                TodayWidgetContent()
            }
        }
    }

    @Composable
    private fun TodayWidgetContent() {
        val prefs = currentState<Preferences>()
        val count = prefs[UnresolvedCountKey] ?: 0
        val nowTitle = prefs[NowTaskTitleKey] ?: ""
        val nextTitle = prefs[NextTaskTitleKey] ?: ""
        val nowId = prefs[NowTaskIdKey] ?: ""
        val dateLabel = prefs[DateLabelKey] ?: ""
        val privacy = prefs[PrivacyEnabledKey] ?: false

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.dayloop_dark))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_dayloop_foreground),
                        contentDescription = null,
                        modifier = GlanceModifier.width(16.dp).height(16.dp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = "DayLoop",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.dayloop_lime)
                        )
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    val mood = prefs[MascotMoodKey] ?: "NEUTRAL"
                    Text(
                        text = when (mood) {
                            "FOCUSED" -> "🧐"
                            "ACCOMPLISHED" -> "🥳"
                            "OVERWHELMED" -> "😨"
                            "SUGGESTING" -> "💡"
                            "EXCITED" -> "✨"
                            "SLEEPY" -> "😴"
                            "ENERGETIC" -> "⚡"
                            "STARGAZING" -> "🌌"
                            else -> "😐"
                        },
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
                Text(
                    text = dateLabel.ifBlank { "Today" },
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(R.color.white)
                    )
                )
            }
            
            Spacer(GlanceModifier.height(8.dp))
            
            Text(
                text = if (count == 0) "All done!" else "$count",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.white)
                )
            )
            Text(
                text = if (count == 1) "task remaining" else "tasks remaining",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(R.color.white)
                )
            )
            
            if (nowTitle.isNotBlank()) {
                Spacer(GlanceModifier.height(16.dp))
                WidgetTaskRow(
                    label = "NOW", 
                    title = if (privacy) "Active task" else nowTitle, 
                    labelColor = ColorProvider(R.color.dayloop_lime),
                    onComplete = if (!privacy && nowId.isNotBlank()) {
                        { actionRunCallback<CompleteTaskCallback>(actionParametersOf(TaskIdParam to nowId)) }
                    } else null
                )
            }
            
            if (nextTitle.isNotBlank()) {
                Spacer(GlanceModifier.height(10.dp))
                WidgetTaskRow(
                    label = "NEXT", 
                    title = if (privacy) "Upcoming task" else nextTitle, 
                    labelColor = ColorProvider(R.color.dayloop_emerald)
                )
            }
            
            if (nowTitle.isBlank() && nextTitle.isBlank() && count > 0) {
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    text = if (privacy) "Check the app for details" else "No specific tasks scheduled",
                    style = TextStyle(fontSize = 13.sp, color = ColorProvider(R.color.white))
                )
            }

            Spacer(GlanceModifier.defaultWeight())
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(R.color.dayloop_lime))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<StartFocusCallback>())
                ) {
                    Text(
                        text = "FOCUS",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.dayloop_dark)
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetTaskRow(
        label: String, 
        title: String, 
        labelColor: ColorProvider,
        onComplete: (() -> Action)? = null
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    )
                )
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(R.color.white)
                    ),
                    maxLines = 1
                )
            }
            onComplete?.let {
                Box(modifier = GlanceModifier.padding(start = 8.dp).clickable(it())) {
                    Text(
                        text = "DONE",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.dayloop_lime)
                        )
                    )
                }
            }
        }
    }

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

class StartFocusCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DayLoopApplication
        val store = FocusStore.get(app)
        store.start(Instant.now())
        FocusService.start(app)
    }
}

class CompleteTaskCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TodayWidget.TaskIdParam] ?: return
        val app = context.applicationContext as DayLoopApplication
        app.appGraph.alarmCoordinator.completeTask(TaskId(taskId))
    }
}
