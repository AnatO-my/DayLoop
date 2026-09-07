package com.dayloop.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsTodayScaffold() {
        composeRule.onNodeWithContentDescription("Open menu").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open calendar").assertIsDisplayed()
        composeRule.onNodeWithText("Open day").assertIsDisplayed()
        composeRule.onNodeWithText("0 of 0 complete").assertIsDisplayed()
        composeRule.onNodeWithText("No tasks planned").assertIsDisplayed()
        composeRule.onNodeWithText("0%").assertIsDisplayed()
        composeRule.onNodeWithText("+ Add task").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing planned").assertIsDisplayed()
        composeRule.onNodeWithText("Today is clear.").assertIsDisplayed()
    }
}
