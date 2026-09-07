package com.dayloop.app.feature.today

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.today.TodayItem
import com.dayloop.app.today.TodaySnapshot
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completeCheckboxEmitsTaskId() {
        val task = Task(
            id = TaskId("brief"),
            title = "Write brief",
        )
        var completedTaskId: TaskId? = null

        composeRule.setContent {
            DayLoopTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        snapshot = TodaySnapshot(
                            date = LocalDate.of(2026, 9, 3),
                            anytime = listOf(TodayItem(task)),
                        ),
                    ),
                    onCompleteTask = { taskId -> completedTaskId = taskId },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Complete Write brief").performClick()

        composeRule.runOnIdle {
            assertEquals(task.id, completedTaskId)
        }
    }

    @Test
    fun emptyDayShowsEmptyDayMessage() {
        composeRule.setContent {
            DayLoopTheme {
                TodayScreen(
                    uiState = TodayUiState.empty(date = LocalDate.of(2026, 9, 3)),
                )
            }
        }

        composeRule.onNodeWithText("Nothing planned").assertExists()
        composeRule.onNodeWithText("Today is clear.").assertExists()
    }

    @Test
    fun todayDashboardVisualExperimentShowsShellAndActions() {
        var addClicks = 0
        var selectedTabId: String? = null

        composeRule.setContent {
            DayLoopTheme {
                TodayScreen(
                    uiState = TodayUiState.empty(date = LocalDate.of(2026, 9, 3)),
                    onAddTask = { addClicks += 1 },
                    onBottomNavItemSelected = { itemId -> selectedTabId = itemId },
                )
            }
        }

        composeRule.onNodeWithText("0%").assertExists()
        composeRule.onNodeWithText("Streak").assertExists()
        composeRule.onNodeWithText("Done").assertExists()
        composeRule.onNodeWithText("Score").assertExists()
        composeRule.onNodeWithText("+ Add task").performClick()
        composeRule.onNodeWithContentDescription("Plan").performClick()

        composeRule.runOnIdle {
            assertEquals(1, addClicks)
            assertEquals("plan", selectedTabId)
        }
    }

    @Test
    fun completedSectionExpandsToRevealCompletedTask() {
        val task = Task(
            id = TaskId("update"),
            title = "Send update",
        ).markCompleted()

        composeRule.setContent {
            DayLoopTheme {
                TodayScreen(
                    uiState = TodayUiState(
                        snapshot = TodaySnapshot(
                            date = LocalDate.of(2026, 9, 3),
                            completed = listOf(TodayItem(task)),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Completed").assertExists()
        composeRule.onNodeWithText("1 task").assertExists()
        composeRule.onNodeWithText("All done today").assertExists()
        composeRule.onNodeWithText("Everything scheduled for today is complete.").assertExists()
        composeRule.onNodeWithContentDescription("Show completed tasks").assertExists()
        composeRule.onNodeWithText("Show").performClick()

        composeRule.onNodeWithText("Send update").assertExists()
        composeRule.onNodeWithContentDescription("Hide completed tasks").assertExists()
        composeRule.onNodeWithContentDescription("Completed Send update").assertExists()
    }
}
