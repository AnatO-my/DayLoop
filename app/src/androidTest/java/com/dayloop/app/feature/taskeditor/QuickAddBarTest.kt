package com.dayloop.app.feature.taskeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickAddBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankSubmitShowsValidationError() {
        composeRule.setContent {
            DayLoopTheme {
                var draft by mutableStateOf(QuickAddDraft())
                QuickAddBar(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskCreated = {},
                    createTaskId = { TaskId("unused") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add task").performClick()

        composeRule.onNodeWithText("Enter a task title").assertExists()
    }

    @Test
    fun validSubmitEmitsTaskAndClearsDraft() {
        var createdTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by mutableStateOf(QuickAddDraft())
                QuickAddBar(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskCreated = { task -> createdTask = task },
                    createTaskId = { TaskId("quick-1") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Quick add task title").performTextInput("  Plan today  ")
        composeRule.onNodeWithContentDescription("Add task").performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("quick-1"), createdTask?.id)
            assertEquals("Plan today", createdTask?.title)
        }
        composeRule.onNodeWithContentDescription("Quick add task title").assertTextEquals("")
    }
}
