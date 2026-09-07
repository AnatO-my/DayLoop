package com.dayloop.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayLoopComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomActionBarEmitsClick() {
        var clicks = 0

        composeRule.setContent {
            DayLoopTheme {
                DayLoopBottomActionBar(
                    text = "Start the day",
                    onClick = { clicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Start the day").performClick()

        composeRule.runOnIdle {
            assertEquals(1, clicks)
        }
    }

    @Test
    fun scheduleSelectorDisplaysOptionsAndEmitsSelection() {
        var selected by mutableStateOf("now")

        composeRule.setContent {
            DayLoopTheme {
                DayLoopScheduleSelector(
                    options = listOf(
                        DayLoopScheduleOption("now", "Now", "Do it soon"),
                        DayLoopScheduleOption("next", "Next", "In order"),
                    ),
                    selectedOptionId = selected,
                    onOptionSelected = { id -> selected = id },
                )
            }
        }

        composeRule.onNodeWithText("Now").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next schedule").performClick()

        composeRule.runOnIdle {
            assertEquals("next", selected)
        }
    }

    @Test
    fun bottomNavDisplaysTabsAndEmitsSelection() {
        var selected by mutableStateOf("today")

        composeRule.setContent {
            DayLoopTheme {
                DayLoopBottomNav(
                    items = listOf(
                        DayLoopBottomNavItem("today", "Today"),
                        DayLoopBottomNavItem("focus", "Focus"),
                    ),
                    selectedItemId = selected,
                    onItemSelected = { id -> selected = id },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Focus").performClick()

        composeRule.runOnIdle {
            assertEquals("focus", selected)
        }
    }

    @Test
    fun warningBannerDisplaysMessageAndEmitsAction() {
        var reviewed = false

        composeRule.setContent {
            DayLoopTheme {
                DayLoopWarningBanner(
                    title = "3 reminders need attention",
                    body = "These may be late or missed.",
                    actionText = "Review",
                    onAction = { reviewed = true },
                )
            }
        }

        composeRule.onNodeWithText("3 reminders need attention").assertIsDisplayed()
        composeRule.onNodeWithText("Review").performClick()

        composeRule.runOnIdle {
            assertEquals(true, reviewed)
        }
    }
}