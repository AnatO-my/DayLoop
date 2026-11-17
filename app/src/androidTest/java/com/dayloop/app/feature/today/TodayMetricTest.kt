package com.dayloop.app.feature.today

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class TodayMetricTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothMetricsOpenTheirEmptyDetailSheets() {
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(LocalDate.of(2026, 9, 17))) }
        }
        compose.onNodeWithText("Tasks\nCompleted").performClick()
        compose.onNodeWithText("No completed tasks yet.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Tasks\nRemaining").performClick()
        compose.onNodeWithText("No tasks remaining today.").assertIsDisplayed()
    }

    @Test fun disabledMetricsCannotOpenSheets() {
        compose.setContent {
            DayLoopTheme { TodayScreen(TodayUiState.empty(LocalDate.of(2026, 9, 17)), actionsEnabled = false) }
        }
        compose.onNodeWithText("Tasks\nCompleted").assertIsNotEnabled()
        compose.onNodeWithText("Tasks\nRemaining").assertIsNotEnabled()
    }
}
