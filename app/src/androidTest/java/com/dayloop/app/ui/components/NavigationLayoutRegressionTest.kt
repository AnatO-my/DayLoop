package com.dayloop.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allDestinationsHaveSeparateVisibleBoundsAndRespond() {
        var selected by mutableStateOf("today")
        compose.setContent {
            DayLoopTheme {
                Box(Modifier.width(320.dp)) {
                    DayLoopMainNav(selected, { selected = it })
                }
            }
        }
        val today = compose.onNodeWithContentDescription("Today").assertIsDisplayed().getUnclippedBoundsInRoot()
        val plans = compose.onNodeWithContentDescription("P&Gs").assertIsDisplayed().getUnclippedBoundsInRoot()
        val focus = compose.onNodeWithContentDescription("Focus").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(today.right <= plans.left)
        assertTrue(plans.right <= focus.left)
        compose.onNodeWithContentDescription("P&Gs").performClick()
        compose.runOnIdle { assertEquals("plan", selected) }
        compose.onNodeWithContentDescription("Focus").performClick()
        compose.runOnIdle { assertEquals("focus", selected) }
        compose.onNodeWithContentDescription("Today").performClick()
        compose.runOnIdle { assertEquals("today", selected) }
    }
}
