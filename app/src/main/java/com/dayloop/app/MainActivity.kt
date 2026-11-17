package com.dayloop.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.dayloop.app.settings.AppSettings
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.today.TodayRoute
import com.dayloop.app.feature.today.TodayScreen
import com.dayloop.app.feature.today.TodayUiState
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var requestedTaskId by mutableStateOf<String?>(null)
    private var widgetRequest by mutableStateOf<String?>(null)
    private var widgetRequestVersion by mutableStateOf(0)
    private var requestedOccurrenceId by mutableStateOf<String?>(null)
    private var requestedGeneration by mutableStateOf(0L)
    private var foreground by mutableStateOf(false)
    private var resumeVersion by mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        foreground = true
        resumeVersion++
    }

    override fun onPause() {
        foreground = false
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetRequest = intent.getStringExtra("widget_route")
        widgetRequestVersion++
        requestedTaskId = intent.getStringExtra("edit_task_id")?.takeIf { it.isNotBlank() }
        requestedOccurrenceId = intent.getStringExtra("reminder_occurrence_id")
        requestedGeneration = intent.getLongExtra("reminder_generation", 0L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetRequest = intent.getStringExtra("widget_route")
        requestedTaskId = intent.getStringExtra("edit_task_id")?.takeIf { it.isNotBlank() }
        requestedOccurrenceId = intent.getStringExtra("reminder_occurrence_id")
        requestedGeneration = intent.getLongExtra("reminder_generation", 0L)
        val appGraph = (application as DayLoopApplication).appGraph

        setContent {
            CompositionLocalProvider(LocalAppGraph provides appGraph) {
                val settings by appGraph.settingsRepository.settings.collectAsState(initial = null)
                val previewUiMode = androidx.compose.ui.platform.LocalConfiguration.current.uiMode
                androidx.compose.runtime.LaunchedEffect(settings?.themeMode, previewUiMode) {
                    settings?.let { com.dayloop.app.widget.WidgetPreviews.publishIfNeeded(applicationContext, it.themeMode) }
                }
                DayLoopTheme(settings = settings ?: AppSettings()) {
                    if (settings == null) {
                        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {}
                        return@DayLoopTheme
                    }
                    DayLoopApp(
                        settings = settings!!,
                        widgetRequest = widgetRequest,
                        widgetRequestVersion = widgetRequestVersion,
                        onWidgetRequestConsumed = { widgetRequest = null; intent.removeExtra("widget_route") },
                        requestedTaskId = requestedTaskId,
                        requestedOccurrenceId = requestedOccurrenceId,
                        requestedGeneration = requestedGeneration,
                        onEditRequestConsumed = {
                            requestedTaskId = null
                            requestedOccurrenceId = null
                            requestedGeneration = 0L
                            intent.removeExtra("edit_task_id")
                            intent.removeExtra("reminder_occurrence_id")
                            intent.removeExtra("reminder_generation")
                        },
                        foreground = foreground,
                        resumeVersion = resumeVersion,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DayLoopAppPreview() {
    DayLoopTheme {
        TodayScreen(
            uiState = TodayUiState.empty(date = LocalDate.of(2026, 9, 2)),
        )
    }
}
