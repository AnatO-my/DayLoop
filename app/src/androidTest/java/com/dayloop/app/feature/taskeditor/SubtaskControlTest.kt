package com.dayloop.app.feature.taskeditor

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Rule
import org.junit.Test

class SubtaskControlTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completionHasAccessibleSizeStateAndDisabledBehavior() {
        var checked by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        compose.setContent {
            DayLoopTheme { SubtaskRow("Read", checked, { checked = it }, {}, enabled) }
        }
        val control = compose.onNodeWithContentDescription("Complete subtask: Read")
        control.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).assertIsOff().performClick()
        control.assertIsOn()
        compose.runOnIdle { enabled = false }
        control.assertIsNotEnabled()
        compose.onNodeWithContentDescription("Remove subtask").assertIsNotEnabled()
    }
}
