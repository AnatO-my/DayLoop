package com.dayloop.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayloop.app.feature.diagnostics.DiagnosticsScreen
import com.dayloop.app.feature.diagnostics.DiagnosticsUiState
import com.dayloop.app.settings.ThemeMode
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DayLoopAppearanceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun appearanceSelectionRecomposesSemanticColors() {
        var background = Color.Unspecified
        var foreground = Color.Unspecified
        var container = Color.Unspecified
        var onContainer = Color.Unspecified
        composeRule.setContent {
            var mode by remember { mutableStateOf(ThemeMode.LIGHT) }
            DayLoopTheme(themeMode = mode) {
                background = MaterialTheme.colorScheme.background
                foreground = MaterialTheme.colorScheme.onBackground
                container = MaterialTheme.colorScheme.surfaceContainer
                onContainer = MaterialTheme.colorScheme.onSurface
                DiagnosticsScreen(
                    uiState = DiagnosticsUiState(),
                    onBack = {}, onRepair = {}, onFixCapability = {}, onClearHistory = {},
                    themeMode = mode, onThemeModeSelected = { mode = it },
                )
            }
        }
        composeRule.runOnIdle { assertTrue(background.luminance() > 0.8f) }
        composeRule.onNodeWithText("Dark").performClick().assertIsSelected()
        composeRule.runOnIdle {
            assertTrue(background.luminance() < 0.03f)
            assertTrue(contrast(background, foreground) >= 4.5f)
            assertTrue(contrast(container, onContainer) >= 4.5f)
        }
        composeRule.onNodeWithText("Light").performClick().assertIsSelected()
        composeRule.runOnIdle { assertTrue(background.luminance() > 0.8f) }
    }

    @Test fun heroForegroundHasReadableContrastInBothModes() {
        var dark by mutableStateOf(false)
        var surface = Color.Unspecified
        var foreground = Color.Unspecified
        composeRule.setContent {
            DayLoopTheme(darkTheme = dark) {
                surface = MaterialTheme.dayLoopTokens.colors.actionSurface
                foreground = MaterialTheme.dayLoopTokens.colors.onActionSurface
                DayLoopHeroCard { DayLoopProgressRing(progress = 0.5f, label = "50%") }
            }
        }
        assertRingLabelIsLight()
        composeRule.runOnIdle {
            assertTrue(contrast(surface, foreground) >= 4.5f)
            dark = true
        }
        assertRingLabelIsLight()
        composeRule.runOnIdle { assertTrue(contrast(surface, foreground) >= 4.5f) }
    }

    private fun assertRingLabelIsLight() {
        val pixels = composeRule.onNodeWithText("50%").captureToImage().toPixelMap()
        var brightPixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y].luminance() > 0.6f) brightPixels++
            }
        }
        assertTrue("Progress label should be light on the dark hero", brightPixels > 10)
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) /
            (minOf(a.luminance(), b.luminance()) + 0.05f)
}
