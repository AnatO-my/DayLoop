package com.dayloop.app.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.feature.focus.FocusStore
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class FocusWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(200.dp, 220.dp), DpSize(280.dp, 220.dp)))
    override val previewSizeMode = sizeMode

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val colors = previewWidgetColors(context)
        provideContent {
            Content(context, "Pomodoro", WidgetPreviewSamples.focusTime,
                WidgetPreviewSamples.focusMinutes, "Start", "focus_start", colors, preview = true)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = (context.applicationContext as DayLoopApplication).appGraph
        val initialSettings = graph.settingsRepository.settings.first()
        val initialSession = FocusStore.get(context).state.first()
        provideContent {
            val session by remember { FocusStore.get(context).state }.collectAsState(initialSession)
            val settings by graph.settingsRepository.settings.collectAsState(initialSettings)
            val colors = widgetColors(context, settings.themeMode, settings.font)
            val now = graph.timeProvider.now()
            val running = session.endsAt != null
            val seconds = session.secondsAt(now)
            val time = if (running) session.endsAt!!.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                else String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
            Content(context, (session.activeMode ?: session.mode).label, if (running) "Until $time" else time,
                session.minutesOn(now.atZone(ZoneId.systemDefault()).toLocalDate()),
                if (running) "Pause" else if (session.activeMode != null) "Resume" else "Start",
                if (running) "focus_pause" else if (session.activeMode != null) "focus_resume" else "focus_start", colors)
        }
    }

    @Composable
    private fun Content(context: Context, mode: String, time: String, minutes: Long, label: String,
        route: String, colors: WidgetColors, preview: Boolean = false) {
            Column(GlanceModifier.fillMaxSize().background(colors.background).cornerRadius(4.dp).padding(16.dp)) {
                Text("Focus", style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text(mode,
                    style = TextStyle(fontFamily = colors.family, color = colors.muted, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.height(8.dp))
                Text(time,
                    style = TextStyle(fontFamily = colors.family, color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Text("$minutes min today",
                    style = TextStyle(fontFamily = colors.family, color = colors.muted, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.defaultWeight())
                WidgetAction(label, if (preview) null else widgetOpen(context, route),
                    colors, GlanceModifier.fillMaxWidth())
            }
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}
