package com.dayloop.app.feature.focus

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Rule
import org.junit.Test

class FocusSoundPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chooseSoundThenTurnItOff() {
        compose.setContent {
            var selected by remember { mutableStateOf(FocusSound.NONE) }
            DayLoopTheme { FocusSoundPicker(selected, { selected = it }) }
        }
        compose.onNodeWithText("Background sound").performClick()
        compose.onNodeWithText("Library").performScrollTo().performClick()
        compose.onNodeWithText("Library").assertIsDisplayed()
        compose.onNodeWithText("Background sound").performClick()
        compose.onNodeWithText("Off").performScrollTo().performClick()
        compose.onNodeWithText("Off").assertIsDisplayed()
    }

    @Test fun closeDoesNotChangeSelectionAndBusyDisablesOpening() {
        var enabled by mutableStateOf(true)
        compose.setContent { DayLoopTheme { FocusSoundPicker(FocusSound.RAIN, {}, enabled) } }
        compose.onNodeWithText("Background sound").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Rain").assertIsDisplayed()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithText("Background sound").assertIsNotEnabled()
    }
}
