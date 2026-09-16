package com.dayloop.app.feature.taskeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.ui.theme.DayLoopTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankSaveShowsValidationError() {
        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = {},
                    createTaskId = { TaskId("unused") },
                )
            }
        }

        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Enter a task title").assertExists()
    }

    @Test
    fun validSaveEmitsTaskWithSelectedPriority() {
        var savedTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = { task -> savedTask = task },
                    createTaskId = { TaskId("editor-1") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Task title").performTextInput("  Plan today  ")
        composeRule.onNodeWithText("High").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("editor-1"), savedTask?.id)
            assertEquals("Plan today", savedTask?.title)
            assertEquals(TaskPriority.HIGH, savedTask?.priority)
        }
    }

    @Test
    fun validDeadlineSaveEmitsTaskWithDeadlineSchedule() {
        var savedTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = { task -> savedTask = task },
                    createTaskId = { TaskId("deadline-editor") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Task title").performTextInput("Submit report")
        composeRule.onNodeWithContentDescription("Add deadline").performClick()
        composeRule.onNodeWithContentDescription("Deadline date").performTextInput("2026-09-06")
        composeRule.onNodeWithContentDescription("Deadline time").performTextInput("17:30")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("deadline-editor"), savedTask?.id)
            assertEquals("Submit report", savedTask?.title)
            assertEquals(
                TaskSchedule.Deadline(dueAt = LocalDateTime.of(2026, 9, 6, 17, 30)),
                savedTask?.schedule,
            )
        }
    }

    @Test
    fun validTimeBlockSaveEmitsTaskWithTimeBlockSchedule() {
        var savedTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = { task -> savedTask = task },
                    createTaskId = { TaskId("time-block-editor") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Task title").performTextInput("Deep work")
        composeRule.onNodeWithContentDescription("Add time block").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Time block start date").performScrollTo().performTextInput("2026-09-06")
        composeRule.onNodeWithContentDescription("Time block start time").performScrollTo().performTextInput("09:00")
        composeRule.onNodeWithContentDescription("Time block end date").performScrollTo().performTextInput("2026-09-06")
        composeRule.onNodeWithContentDescription("Time block end time").performScrollTo().performTextInput("10:30")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("time-block-editor"), savedTask?.id)
            assertEquals("Deep work", savedTask?.title)
            assertEquals(
                TaskSchedule.TimeBlock(
                    startsAt = LocalDateTime.of(2026, 9, 6, 9, 0),
                    endsAt = LocalDateTime.of(2026, 9, 6, 10, 30),
                ),
                savedTask?.schedule,
            )
        }
    }

    @Test
    fun validIntervalSaveEmitsTaskWithIntervalSchedule() {
        var savedTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = { task -> savedTask = task },
                    createTaskId = { TaskId("interval-editor") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Task title").performTextInput("Drink water")
        composeRule.onNodeWithContentDescription("Add interval").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Interval start date").performScrollTo().performTextInput("2026-09-06")
        composeRule.onNodeWithContentDescription("Interval duration minutes").performScrollTo().performTextInput("90")
        composeRule.onNodeWithContentDescription("Interval window start time").performScrollTo().performTextInput("08:00")
        composeRule.onNodeWithContentDescription("Interval window end time").performScrollTo().performTextInput("18:00")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("interval-editor"), savedTask?.id)
            assertEquals("Drink water", savedTask?.title)
            assertEquals(
                TaskSchedule.Interval(
                    rule = IntervalRule(
                        every = Duration.ofMinutes(90),
                        activeWindow = LocalTimeRange(
                            startsAt = LocalTime.of(8, 0),
                            endsAt = LocalTime.of(18, 0),
                        ),
                        eligibleDays = DayOfWeek.entries.toSet(),
                        startsOn = LocalDate.of(2026, 9, 6),
                        anchor = IntervalAnchor.SCHEDULED_START,
                    ),
                ),
                savedTask?.schedule,
            )
        }
    }
    @Test
    fun validPersistentReminderSaveEmitsTaskWithReminderPolicy() {
        var savedTask: Task? = null

        composeRule.setContent {
            DayLoopTheme {
                var draft by remember { mutableStateOf(TaskEditorDraft()) }
                TaskEditorScreen(
                    draft = draft,
                    onDraftChange = { changedDraft -> draft = changedDraft },
                    onTaskSaved = { task -> savedTask = task },
                    createTaskId = { TaskId("persistent-reminder-editor") },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Task title").performTextInput("Drink water")
        composeRule.onNodeWithContentDescription("Reminder Persistent").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Reminder re-alert interval minutes")
            .performScrollTo()
            .performTextClearance()
        composeRule.onNodeWithContentDescription("Reminder re-alert interval minutes").performTextInput("20")
        composeRule.onNodeWithContentDescription("Reminder maximum re-alerts")
            .performScrollTo()
            .performTextClearance()
        composeRule.onNodeWithContentDescription("Reminder maximum re-alerts").performTextInput("4")
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(TaskId("persistent-reminder-editor"), savedTask?.id)
            assertEquals("Drink water", savedTask?.title)
            assertEquals(ReminderPolicy.persistent(Duration.ofMinutes(20), 4), savedTask?.reminderPolicy)
        }
    }
}

