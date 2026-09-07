package com.dayloop.app.feature.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.today.TodayItem
import com.dayloop.app.today.TodaySection
import com.dayloop.app.today.TodaySnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {
    private val date = LocalDate.of(2026, 9, 2)

    @Test
    fun emptyStateUsesSnapshotDateAndEmptyFlag() {
        val state = TodayUiState.empty(date = date)

        assertEquals("Today", state.title)
        assertEquals("Wed, Sep 2", state.dateLabel)
        assertTrue(state.isEmpty)
        assertEquals(0, state.remainingCount)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.totalCount)
        assertEquals(0f, state.progressFraction)
        assertEquals(0, state.progressPercent)
        assertEquals("0%", state.progressPercentLabel)
        assertEquals("0", state.taskCountValue)
        assertEquals("Tasks", state.taskCountMetricLabel)
        assertEquals("0m", state.focusTimeValue)
        assertEquals("Focus", state.focusTimeMetricLabel)
        assertEquals("0", state.streakValue)
        assertEquals("Streak", state.streakMetricLabel)
        assertEquals("0", state.completedMetricValue)
        assertEquals("Done", state.completedMetricLabel)
        assertEquals("--", state.focusScoreValue)
        assertEquals("Score", state.focusScoreMetricLabel)
        assertEquals("+ Add task", state.addTaskActionLabel)
        assertEquals("today", state.selectedBottomNavItemId)
        assertEquals("Open day", state.dayStatusLabel)
        assertEquals(TodayExperienceState.EMPTY, state.experienceState)
        assertTrue(state.shouldShowDayMessage)
        assertEquals("Nothing planned", state.dayMessageTitle)
        assertEquals("Today is clear.", state.dayMessageBody)
        assertEquals("Nothing planned, Today is clear.", state.dayMessageAccessibilityLabel)
        assertEquals("0 of 0 complete", state.progressSummary)
        assertEquals("No tasks planned", state.remainingLabel)
        assertEquals("Open day, 0 of 0 complete, No tasks planned", state.progressAccessibilityLabel)
    }

    @Test
    fun stateDerivesRemainingAndCompletedCountsFromSnapshot() {
        val active = task(id = "active")
        val completed = task(id = "completed").markCompleted()
        val state = TodayUiState(
            snapshot = TodaySnapshot(
                date = date,
                anytime = listOf(TodayItem(active)),
                completed = listOf(TodayItem(completed)),
            ),
        )

        assertFalse(state.isEmpty)
        assertEquals(1, state.remainingCount)
        assertEquals(1, state.completedCount)
        assertEquals(2, state.totalCount)
        assertEquals(0.5f, state.progressFraction)
        assertEquals(50, state.progressPercent)
        assertEquals("50%", state.progressPercentLabel)
        assertEquals("2", state.taskCountValue)
        assertEquals("Tasks", state.taskCountMetricLabel)
        assertEquals("1", state.completedMetricValue)
        assertEquals("Open day", state.dayStatusLabel)
        assertEquals(TodayExperienceState.OPEN, state.experienceState)
        assertFalse(state.shouldShowDayMessage)
        assertEquals("", state.dayMessageTitle)
        assertEquals("", state.dayMessageBody)
        assertEquals("", state.dayMessageAccessibilityLabel)
        assertEquals("1 of 2 complete", state.progressSummary)
        assertEquals("1 task remaining", state.remainingLabel)
        assertEquals("Open day, 1 of 2 complete, 1 task remaining", state.progressAccessibilityLabel)
    }

    @Test
    fun completeStateReportsDayCompleteAndFullProgress() {
        val completed = task(id = "completed").markCompleted()
        val state = TodayUiState(
            snapshot = TodaySnapshot(
                date = date,
                completed = listOf(TodayItem(completed)),
            ),
        )

        assertFalse(state.isEmpty)
        assertEquals(0, state.remainingCount)
        assertEquals(1, state.completedCount)
        assertEquals(1, state.totalCount)
        assertEquals(1f, state.progressFraction)
        assertEquals(100, state.progressPercent)
        assertEquals("100%", state.progressPercentLabel)
        assertEquals("1", state.taskCountValue)
        assertEquals("Task", state.taskCountMetricLabel)
        assertEquals("1", state.completedMetricValue)
        assertEquals("Day complete", state.dayStatusLabel)
        assertEquals(TodayExperienceState.COMPLETE, state.experienceState)
        assertTrue(state.shouldShowDayMessage)
        assertEquals("All done today", state.dayMessageTitle)
        assertEquals("Everything scheduled for today is complete.", state.dayMessageBody)
        assertEquals("All done today, Everything scheduled for today is complete.", state.dayMessageAccessibilityLabel)
        assertEquals("1 of 1 complete", state.progressSummary)
        assertEquals("All tasks complete", state.remainingLabel)
        assertEquals("Day complete, 1 of 1 complete, All tasks complete", state.progressAccessibilityLabel)
    }

    @Test
    fun unresolvedSectionsPreserveTodayDisplayOrderAndTaskTitles() {
        val overdue = task(id = "overdue", title = "Past deadline")
        val now = task(id = "now", title = "Current block")
        val next = task(id = "next", title = "Upcoming deadline")
        val anytime = task(id = "anytime", title = "Anytime chore")
        val state = TodayUiState(
            snapshot = TodaySnapshot(
                date = date,
                overdue = listOf(TodayItem(overdue)),
                now = listOf(TodayItem(now)),
                next = listOf(TodayItem(next)),
                anytime = listOf(TodayItem(anytime)),
            ),
        )

        assertEquals(
            listOf(TodaySection.OVERDUE, TodaySection.NOW, TodaySection.NEXT, TodaySection.ANYTIME),
            state.sections.map { section -> section.section },
        )
        assertEquals(listOf("Overdue", "Now", "Next", "Anytime"), state.visibleSections.map { section -> section.title })
        assertEquals(listOf("Past deadline", "Current block", "Upcoming deadline", "Anytime chore"), state.visibleSections.map { section -> section.items.single().title })
        assertEquals(listOf("Complete Past deadline", "Complete Current block", "Complete Upcoming deadline", "Complete Anytime chore"), state.visibleSections.map { section -> section.items.single().completeActionLabel })
        assertEquals(listOf("1 task", "1 task", "1 task", "1 task"), state.visibleSections.map { section -> section.countLabel })
    }

    @Test
    fun emptySectionsAreAvailableForStateButHiddenFromVisibleSections() {
        val anytime = task(id = "anytime", title = "Anytime chore")
        val state = TodayUiState(
            snapshot = TodaySnapshot(
                date = date,
                anytime = listOf(TodayItem(anytime)),
            ),
        )

        assertEquals(4, state.sections.size)
        assertTrue(state.sections.first { section -> section.section == TodaySection.OVERDUE }.isEmpty)
        assertEquals(listOf(TodaySection.ANYTIME), state.visibleSections.map { section -> section.section })
        assertEquals("Anytime, 1 task", state.visibleSections.single().accessibilityLabel)
    }

    @Test
    fun completedSectionIsDerivedSeparatelyFromVisibleUnresolvedSections() {
        val active = task(id = "active", title = "Write brief")
        val completed = task(id = "completed", title = "Send update").markCompleted()
        val state = TodayUiState(
            snapshot = TodaySnapshot(
                date = date,
                anytime = listOf(TodayItem(active)),
                completed = listOf(TodayItem(completed)),
            ),
        )

        assertEquals(listOf(TodaySection.ANYTIME), state.visibleSections.map { section -> section.section })
        assertTrue(state.hasCompletedSection)
        assertEquals(TodaySection.COMPLETED, state.completedSection.section)
        assertEquals("Completed", state.completedSection.title)
        assertEquals("1 task", state.completedSection.countLabel)
        assertEquals("Completed, 1 task", state.completedSection.accessibilityLabel)
        assertEquals("Show completed tasks", state.completedSection.expandActionLabel)
        assertEquals("Hide completed tasks", state.completedSection.collapseActionLabel)
        assertEquals("Send update", state.completedSection.items.single().title)
        assertTrue(state.completedSection.items.single().isCompleted)
        assertEquals("Completed Send update", state.completedSection.items.single().completedLabel)
    }

    private fun task(
        id: String,
        title: String = "Task $id",
    ): Task = Task(
        id = TaskId(id),
        title = title,
    )
}
