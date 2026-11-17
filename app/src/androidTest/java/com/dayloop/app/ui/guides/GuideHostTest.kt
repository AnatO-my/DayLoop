package com.dayloop.app.ui.guides

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GuideHostTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nextAdvancesAndMissingTargetStillAllowsSkip() {
        val steps = GuideDefinitions.getSteps(GuideDefinitions.FirstLaunch)
        var action: GuideAction? = null
        compose.setContent {
            DayLoopTheme {
                GuideHost(steps.first(), { action = it }, GuideAction.Advance(steps[1].id)) {
                    Box(Modifier.size(48.dp).guideTarget(steps.first().targetId))
                }
            }
        }
        compose.onNodeWithText("Next").performClick()
        compose.runOnIdle { assertEquals(GuideAction.Advance("create_task"), action) }
        compose.onNodeWithText("Skip").performClick()
        compose.runOnIdle { assertEquals(GuideAction.Skip, action) }
    }

    @Test fun disposedTargetIsRemovedAndBusyDisablesGuideControls() {
        var targetVisible by mutableStateOf(true)
        var busy by mutableStateOf(false)
        val step = GuideDefinitions.getSteps(GuideDefinitions.FirstLaunch).first()
        compose.setContent {
            DayLoopTheme {
                GuideHost(step, {}, GuideAction.Advance("create_task"), busy) {
                    if (targetVisible) Box(Modifier.size(48.dp).guideTarget(step.targetId))
                }
            }
        }
        compose.onNodeWithText(step.title).assertIsDisplayed()
        compose.runOnIdle { targetVisible = false; busy = true }
        compose.onNodeWithText(step.title).assertIsDisplayed()
        compose.onNodeWithText("Skip").assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test fun targetModifierIsSafeOutsideHost() {
        var clicked = false
        compose.setContent {
            DayLoopTheme {
                Button(onClick = { clicked = true }, modifier = Modifier.guideTarget("preview")) { Text("Action") }
            }
        }
        compose.onNodeWithText("Action").performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }

    @Test fun nestedScreenHostRendersOnePanelAndUsesOuterActions() {
        var outerActions = 0
        var innerActions = 0
        val step = GuideDefinitions.getSteps(GuideDefinitions.FirstLaunch).first()
        compose.setContent {
            DayLoopTheme {
                GuideHost(step, { outerActions++ }, GuideAction.Advance("create_task")) {
                    GuideHost(step, { innerActions++ }, GuideAction.Advance("create_task")) {
                        Box(Modifier.size(48.dp).guideTarget(step.targetId))
                    }
                }
            }
        }
        compose.onAllNodesWithText(step.title).assertCountEquals(1)
        compose.onAllNodesWithText(step.message).assertCountEquals(1)
        compose.onAllNodesWithText("Next").assertCountEquals(1)
        compose.onAllNodesWithText("Skip").assertCountEquals(1)
        compose.onNodeWithText("Next").performClick()
        compose.runOnIdle {
            assertEquals(1, outerActions)
            assertEquals(0, innerActions)
        }
    }
}
