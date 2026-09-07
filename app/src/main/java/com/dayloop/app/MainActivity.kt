package com.dayloop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.today.TodayRoute
import com.dayloop.app.feature.today.TodayScreen
import com.dayloop.app.feature.today.TodayUiState
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appGraph = (application as DayLoopApplication).appGraph

        setContent {
            CompositionLocalProvider(LocalAppGraph provides appGraph) {
                DayLoopTheme {
                    DayLoopApp()
                }
            }
        }
    }
}

@Composable
fun DayLoopApp() {
    TodayRoute()
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