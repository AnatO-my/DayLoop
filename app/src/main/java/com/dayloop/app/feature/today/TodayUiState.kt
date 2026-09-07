package com.dayloop.app.feature.today

import com.dayloop.app.core.model.TaskId
import com.dayloop.app.today.TodayDayStatus
import com.dayloop.app.today.TodayItem
import com.dayloop.app.today.TodaySection
import com.dayloop.app.today.TodaySnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TodayDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
private val TodayUnresolvedSections = listOf(
    TodaySection.OVERDUE,
    TodaySection.NOW,
    TodaySection.NEXT,
    TodaySection.ANYTIME,
)

data class TodayUiState(
    val snapshot: TodaySnapshot,
) {
    val title: String = "Today"
    val dateLabel: String = snapshot.date.format(TodayDateFormatter)
    val isEmpty: Boolean = snapshot.isEmpty
    val dayStatus: TodayDayStatus = snapshot.dayStatus
    val remainingCount: Int = snapshot.progress.remainingCount
    val completedCount: Int = snapshot.completedCount
    val totalCount: Int = snapshot.totalCount
    val progressFraction: Float = snapshot.progress.completionRatio.toFloat()
    val progressPercent: Int = (progressFraction.coerceIn(0f, 1f) * 100).toInt()
    val progressPercentLabel: String = "$progressPercent%"
    val taskCountValue: String = totalCount.toString()
    val taskCountMetricLabel: String = if (totalCount == 1) "Task" else "Tasks"
    val focusTimeValue: String = "0m"
    val focusTimeMetricLabel: String = "Focus"
    val streakValue: String = "0"
    val streakMetricLabel: String = "Streak"
    val completedMetricValue: String = completedCount.toString()
    val completedMetricLabel: String = "Done"
    val focusScoreValue: String = "--"
    val focusScoreMetricLabel: String = "Score"
    val addTaskActionLabel: String = "+ Add task"
    val selectedBottomNavItemId: String = "today"
    val dayStatusLabel: String = when (dayStatus) {
        TodayDayStatus.OPEN -> "Open day"
        TodayDayStatus.COMPLETE -> "Day complete"
    }
    val experienceState: TodayExperienceState = when {
        isEmpty -> TodayExperienceState.EMPTY
        dayStatus == TodayDayStatus.COMPLETE -> TodayExperienceState.COMPLETE
        else -> TodayExperienceState.OPEN
    }
    val shouldShowDayMessage: Boolean = experienceState != TodayExperienceState.OPEN
    val dayMessageTitle: String = when (experienceState) {
        TodayExperienceState.EMPTY -> "Nothing planned"
        TodayExperienceState.COMPLETE -> "All done today"
        TodayExperienceState.OPEN -> ""
    }
    val dayMessageBody: String = when (experienceState) {
        TodayExperienceState.EMPTY -> "Today is clear."
        TodayExperienceState.COMPLETE -> "Everything scheduled for today is complete."
        TodayExperienceState.OPEN -> ""
    }
    val dayMessageAccessibilityLabel: String = when (experienceState) {
        TodayExperienceState.EMPTY -> "$dayMessageTitle, $dayMessageBody"
        TodayExperienceState.COMPLETE -> "$dayMessageTitle, $dayMessageBody"
        TodayExperienceState.OPEN -> ""
    }
    val progressSummary: String = "$completedCount of $totalCount complete"
    val remainingLabel: String = when {
        isEmpty -> "No tasks planned"
        remainingCount == 0 -> "All tasks complete"
        remainingCount == 1 -> "1 task remaining"
        else -> "$remainingCount tasks remaining"
    }
    val progressAccessibilityLabel: String = "$dayStatusLabel, $progressSummary, $remainingLabel"
    val sections: List<TodaySectionUiState> = TodayUnresolvedSections.map { section ->
        val sectionItems = snapshot.itemsIn(section).map { item -> item.toUiState(isCompleted = false) }
        TodaySectionUiState(
            section = section,
            title = section.displayTitle(),
            items = sectionItems,
        )
    }
    val visibleSections: List<TodaySectionUiState> = sections.filterNot { section -> section.isEmpty }
    val completedSection: TodaySectionUiState = TodaySectionUiState(
        section = TodaySection.COMPLETED,
        title = TodaySection.COMPLETED.displayTitle(),
        items = snapshot.completed.map { item -> item.toUiState(isCompleted = true) },
    )
    val hasCompletedSection: Boolean = !completedSection.isEmpty

    companion object {
        fun empty(date: LocalDate): TodayUiState = TodayUiState(
            snapshot = TodaySnapshot(date = date),
        )
    }
}

enum class TodayExperienceState {
    EMPTY,
    OPEN,
    COMPLETE,
}

data class TodaySectionUiState(
    val section: TodaySection,
    val title: String,
    val items: List<TodayTaskUiState>,
) {
    val count: Int = items.size
    val isEmpty: Boolean = items.isEmpty()
    val countLabel: String = when (count) {
        0 -> "No tasks"
        1 -> "1 task"
        else -> "$count tasks"
    }
    val accessibilityLabel: String = "$title, $countLabel"
    private val actionTitle: String = title.replaceFirstChar { character -> character.lowercase() }
    val expandActionLabel: String = "Show $actionTitle tasks"
    val collapseActionLabel: String = "Hide $actionTitle tasks"
}

data class TodayTaskUiState(
    val taskId: TaskId,
    val title: String,
    val isCompleted: Boolean,
) {
    val completeActionLabel: String = "Complete $title"
    val completedLabel: String = "Completed $title"
}

private fun TodayItem.toUiState(isCompleted: Boolean): TodayTaskUiState = TodayTaskUiState(
    taskId = taskId,
    title = task.title,
    isCompleted = isCompleted,
)

private fun TodaySection.displayTitle(): String = when (this) {
    TodaySection.OVERDUE -> "Overdue"
    TodaySection.NOW -> "Now"
    TodaySection.NEXT -> "Next"
    TodaySection.ANYTIME -> "Anytime"
    TodaySection.COMPLETED -> "Completed"
}
