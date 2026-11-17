package com.dayloop.app.feature.today

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayItem
import com.dayloop.app.ui.theme.DayLoopTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MissedTaskReviewScreenTest {
    @get:Rule val compose = createComposeRule()
    private val task = Task(TaskId("missed"), "Study")

    @Test fun reviewActionsEmitTheSelectedTaskId() {
        val actions = mutableListOf<Pair<String, TaskId>>()
        compose.setContent {
            DayLoopTheme {
                MissedTaskReviewScreen(listOf(TodayItem(task)),
                    onKeepForToday = { actions += "keep" to it },
                    onReschedule = { actions += "reschedule" to it },
                    onSomeday = { actions += "someday" to it },
                    onDismiss = { actions += "dismiss" to it }, onBack = {})
            }
        }
        listOf("Keep for today", "Reschedule", "Someday", "Dismiss").forEach {
            compose.onNodeWithText(it).performClick()
        }
        assertEquals(listOf("keep", "reschedule", "someday", "dismiss").map { it to task.id }, actions)
    }

    @Test fun busyReviewDisablesAllMutations() {
        compose.setContent {
            DayLoopTheme {
                MissedTaskReviewScreen(listOf(TodayItem(task)), {}, {}, {}, {}, {}, busy = true)
            }
        }
        listOf("Keep for today", "Reschedule", "Someday", "Dismiss").forEach {
            compose.onNodeWithText(it).assertIsNotEnabled()
        }
    }

    @Test fun savedWorkCanBeRestoredAndDismissedTasksRemainAccessible() {
        var restored: TaskId? = null
        compose.setContent {
            DayLoopTheme {
                SavedWorkScreen(listOf(task.moveToSomeday(), Task(TaskId("skip"), "Skipped task").skip()), false,
                    onRestore = { restored = it }, onReschedule = {}, onBack = {})
            }
        }
        compose.onNodeWithText("Study").assertIsDisplayed()
        compose.onNodeWithText("Bring to today").performClick()
        assertEquals(task.id, restored)
        compose.onNodeWithText("Dismissed (1)").performClick()
        compose.onNodeWithText("Skipped task").assertIsDisplayed()
        compose.onNodeWithText("Study").assertDoesNotExist()
    }
}
