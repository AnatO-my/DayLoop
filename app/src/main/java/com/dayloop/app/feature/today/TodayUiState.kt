package com.dayloop.app.feature.today

import com.dayloop.app.core.model.ProjectId
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.Subtask
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.today.*
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val projectTitles: Map<ProjectId, String> = emptyMap(),
    val focusMinutes: Long = 0,
    val focusStreak: Int = 0,
    val isFocusActive: Boolean = false,
    val assistantSuggestions: List<AssistantSuggestion> = emptyList(),
    val now: LocalDateTime = LocalDateTime.now(),
) {
    val title: String = "Today"
    val dateLabel: String = snapshot.date.format(TodayDateFormatter)
    val isEmpty: Boolean = snapshot.isEmpty
    val dayStatus: TodayDayStatus = snapshot.dayStatus
    
    val mascotMood: MascotMood = when {
        assistantSuggestions.isNotEmpty() -> MascotMood.SUGGESTING
        isFocusActive -> MascotMood.FOCUSED
        snapshot.itemsIn(TodaySection.OVERDUE).isNotEmpty() -> MascotMood.OVERWHELMED
        dayStatus == TodayDayStatus.COMPLETE -> MascotMood.ACCOMPLISHED
        now.hour >= 23 || now.hour < 6 -> MascotMood.SLEEPY
        focusStreak >= 5 || (now.hour in 6..9) -> MascotMood.ENERGETIC
        else -> MascotMood.NEUTRAL
    }

    val remainingCount: Int = snapshot.progress.remainingCount
    val completedCount: Int = snapshot.completedCount
    val totalCount: Int = snapshot.totalCount
    val progressFraction: Float = snapshot.progress.completionRatio.toFloat()
    val progressPercent: Int = (progressFraction.coerceIn(0f, 1f) * 100).toInt()
    val progressPercentLabel: String = "$progressPercent%"
    val taskCountValue: String = totalCount.toString()
    val taskCountMetricLabel: String = if (totalCount == 1) "Task" else "Tasks"
    val focusTimeValue: String = "${focusMinutes}m"
    val focusTimeMetricLabel: String = "Focus"
    val streakValue: String = focusStreak.toString()
    val streakMetricLabel: String = "Focus streak"
    val completedMetricValue: String = completedCount.toString()
    val completedMetricLabel: String = "Done"
    val focusScoreValue: String = progressPercentLabel
    val focusScoreMetricLabel: String = "Progress"
    val addTaskActionLabel: String = "+ Add task"
    val selectedBottomNavItemId: String = "today"
    val dayStatusLabel: String = when (dayStatus) {
        TodayDayStatus.OPEN -> "Open day"
        TodayDayStatus.COMPLETE -> "Day complete"
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
        val sectionItems = snapshot.itemsIn(section).map { item -> item.toUiState(isCompleted = false, projectTitles) }
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
        items = snapshot.completed.map { item -> item.toUiState(isCompleted = true, projectTitles) },
    )
    val hasCompletedSection: Boolean = !completedSection.isEmpty

    companion object {
        fun empty(date: LocalDate): TodayUiState = TodayUiState(
            snapshot = TodaySnapshot(date = date),
        )
    }
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
    val projectTitle: String? = null,
    val subtasks: List<Subtask> = emptyList(),
) {
    val completeActionLabel: String = "Complete $title"
    val completedLabel: String = "Completed $title"
    val completedSubtaskCount: Int = subtasks.count { it.isCompleted }
    val totalSubtaskCount: Int = subtasks.size
    val hasSubtasks: Boolean = subtasks.isNotEmpty()
    val subtaskProgressLabel: String = if (hasSubtasks) "$completedSubtaskCount/$totalSubtaskCount" else ""
}

private fun TodayItem.toUiState(
    isCompleted: Boolean,
    projectTitles: Map<ProjectId, String>
): TodayTaskUiState = TodayTaskUiState(
    taskId = taskId,
    title = task.title,
    isCompleted = isCompleted,
    projectTitle = task.projectId?.let { projectTitles[it] },
    subtasks = task.subtasks,
)

private fun TodaySection.displayTitle(): String = when (this) {
    TodaySection.OVERDUE -> "Overdue"
    TodaySection.NOW -> "Now"
    TodaySection.NEXT -> "Next"
    TodaySection.ANYTIME -> "Anytime"
    TodaySection.COMPLETED -> "Completed"
}
