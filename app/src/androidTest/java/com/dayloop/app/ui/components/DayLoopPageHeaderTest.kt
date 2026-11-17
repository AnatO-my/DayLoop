package com.dayloop.app.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DayLoopPageHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mascotTapAndHoldOpenMore() {
        var opens = 0
        compose.setContent { DayLoopTheme { DayLoopPageHeader("Today", { opens++ }) } }
        compose.onNodeWithContentDescription("More settings").performClick()
        compose.onNodeWithContentDescription("More settings").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(2, opens) }
    }
}
