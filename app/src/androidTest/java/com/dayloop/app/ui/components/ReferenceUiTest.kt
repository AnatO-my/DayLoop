package com.dayloop.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.settings.*
import com.dayloop.app.ui.theme.FunnelDisplayFontFamily
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReferenceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fixedPalettePreservesSelectedFunnelDisplayFont() {
        var accent = Color.Unspecified
        var inverse = Color.Unspecified
        var family: androidx.compose.ui.text.font.FontFamily? = null
        compose.setContent {
            DayLoopTheme(AppSettings(themeMode = ThemeMode.DARK,
                customColorHex = "#FF0000", font = AppFont.FUNNEL_DISPLAY)) {
                accent = MaterialTheme.colorScheme.primary
                inverse = MaterialTheme.colorScheme.inverseSurface
                family = MaterialTheme.typography.bodyMedium.fontFamily
            }
        }
        compose.runOnIdle {
            assertEquals(Color(0xFFDDF36B), accent)
            assertEquals(Color(0xFFF4F8F9), inverse)
            assertEquals(FunnelDisplayFontFamily, family)
        }
    }

    @Test fun creationReturnsOnPlansAndNeverOccupiesFocusDock() {
        compose.setContent {
            var selected by remember { mutableStateOf("today") }
            DayLoopTheme {
                DayLoopMainNav(selected, { selected = it }, onAdd = {})
            }
        }
        compose.onNodeWithContentDescription("Add").assertExists()
        compose.onNodeWithText("Focus").performClick()
        compose.onNodeWithContentDescription("Add").assertDoesNotExist()
        compose.onNodeWithText("P&Gs").performClick()
        compose.onNodeWithContentDescription("Add").assertExists()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithContentDescription("Add").assertExists()
    }

    @Test fun scheduleOptionsHaveFullLabelsAndSelection() {
        compose.setContent {
            var selected by remember { mutableStateOf("day") }
            DayLoopTheme {
                DayLoopOptionGrid(listOf(DayLoopPillOption("day", "Anytime"),
                    DayLoopPillOption("block", "Time block")), selected, { selected = it })
            }
        }
        compose.onNodeWithText("Time block").performClick().assertIsSelected()
        compose.onNodeWithText("Anytime").assertIsNotSelected()
    }
}
