package com.dayloop.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.settings.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Fictional fixtures only: the picker must never publish private task/goal/timer data.
internal object WidgetPreviewSamples {
    val today = TodayWidgetState(
        unresolvedCount = 3, completedCount = 2, dateLabel = "Mon, Sep 21",
        nowTask = WidgetTask(TaskId("preview-now"), "Read a chapter", "NOW"),
        nextTask = WidgetTask(TaskId("preview-next"), "Plan tomorrow", "NEXT"),
    )
    const val goalTitle = "Read 10 books"
    const val goalProgress = 0.4f
    const val focusTime = "25:00"
    const val focusMinutes = 45L
}

internal suspend fun previewWidgetColors(context: Context): WidgetColors {
    val settings = (context.applicationContext as DayLoopApplication).appGraph.settingsRepository.settings.first()
    return widgetColors(context, settings.themeMode, settings.font)
}

/** Best-effort picker publishing, separate from live widget updates and all timer actions. */
internal object WidgetPreviews {
    private const val REVISION = 1
    private val mutex = Mutex()
    private val attempted = mutableSetOf<String>()

    suspend fun publishIfNeeded(context: Context, themeMode: ThemeMode) {
        if (Build.VERSION.SDK_INT < 35) return
        mutex.withLock {
            val dark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM &&
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            val signature = "$REVISION:$dark"
            val prefs = context.getSharedPreferences("widget_preview_publication", Context.MODE_PRIVATE)
            val manager = GlanceAppWidgetManager(context)
            try {
                val installed = AppWidgetManager.getInstance(context).installedProviders.associateBy { it.provider }
                for (receiver in listOf(TodayWidgetReceiver::class, GoalWidgetReceiver::class, FocusWidgetReceiver::class)) {
                    val name = receiver.java.name
                    val info = installed[ComponentName(context, receiver.java)] ?: continue
                    val published = info.generatedPreviewCategories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0
                    if (published && prefs.getString(name, null) == signature) continue
                    // Rate-limited or failed requests retry on a later process launch, never in a loop.
                    if (!attempted.add("$name:$signature")) continue
                    try {
                        val result = manager.setWidgetPreviews(receiver)
                        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                            prefs.edit().putString(name, signature).apply()
                            attempted.remove("$name:$signature")
                        }
                    } catch (cancelled: CancellationException) {
                        attempted.remove("$name:$signature")
                        throw cancelled
                    } catch (failure: Exception) {
                        Log.w("WidgetPreviews", "Using static preview for $name", failure)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w("WidgetPreviews", "Picker previews unavailable; static resources remain available", failure)
            }
        }
    }
}
