package com.dayloop.app.feature.diagnostics

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IntroductionReplayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun historyOnlyClearsAfterConfirmation() {
        var clears = 0
        val entry = com.dayloop.app.core.alarms.AlarmDiagnosticEntry(
            occurrenceId = null, taskId = null,
            event = com.dayloop.app.core.alarms.AlarmDiagnosticEvent.RECOVERY_COMPLETED,
            createdAt = java.time.Instant.EPOCH)
        compose.setContent {
            DayLoopTheme {
                DiagnosticsScreen(DiagnosticsUiState(entries = listOf(entry)), onBack = {}, onRepair = {},
                    onFixCapability = {}, onClearHistory = { clears++ })
            }
        }
        compose.onNodeWithText("Clear").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithText("Clear").performClick()
        compose.onNode(hasText("Clear") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(1, clears) }
    }

    @Test fun replayInvokesCallbackAndDisablesWhileBusy() {
        var enabled by mutableStateOf(true)
        var replayCount = 0
        compose.setContent {
            DayLoopTheme {
                DiagnosticsScreen(DiagnosticsUiState(), onBack = {}, onRepair = {},
                    onFixCapability = {}, onClearHistory = {},
                    onReplayIntroduction = { replayCount++ }, replayEnabled = enabled)
            }
        }
        compose.onNodeWithContentDescription("Replay introduction").performClick()
        compose.runOnIdle { assertEquals(1, replayCount); enabled = false }
        compose.onNodeWithContentDescription("Replay introduction").assertIsNotEnabled()
    }
}
