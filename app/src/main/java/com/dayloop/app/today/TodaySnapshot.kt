package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate

data class TodaySnapshot(
    val date: LocalDate,
    val overdue: List<TodayItem> = emptyList(),
    val now: List<TodayItem> = emptyList(),
    val next: List<TodayItem> = emptyList(),
    val anytime: List<TodayItem> = emptyList(),
    val completed: List<TodayItem> = emptyList(),
    val missed: List<TodayItem> = emptyList(),
) {
    // Review overlaps overdue deadlines; count each task once across both views.
    val unresolved: List<TodayItem> = (overdue + now + next + anytime + missed).distinctBy { it.taskId }

    val hasMissedTasks: Boolean = missed.isNotEmpty()

    val unresolvedCount: Int = unresolved.size

    val completedCount: Int = completed.size

    val totalCount: Int = unresolvedCount + completedCount

    val progress: TodayProgress = TodayProgress(
        resolvedCount = completedCount,
        requiredCount = totalCount,
    )

    val dayStatus: TodayDayStatus = if (progress.isComplete) {
        TodayDayStatus.COMPLETE
    } else {
        TodayDayStatus.OPEN
    }

    val isEmpty: Boolean = totalCount == 0

    init {
        require(missed.all { it.task.status == TaskStatus.ACTIVE }) {
            "Only active tasks can need missed-work review."
        }
        require(unresolved.all { item -> item.task.status == TaskStatus.ACTIVE }) {
            "Only active tasks can appear in unresolved Today sections."
        }
        require(completed.all { item -> item.task.status == TaskStatus.COMPLETED }) {
            "Only completed tasks can appear in the completed Today section."
        }
        require(missed.map { it.taskId }.distinct().size == missed.size) {
            "A task can appear only once in missed-work review."
        }
        requireNoDuplicateTasks()
    }

    fun itemsIn(section: TodaySection): List<TodayItem> = when (section) {
        TodaySection.OVERDUE -> overdue
        TodaySection.NOW -> now
        TodaySection.NEXT -> next
        TodaySection.ANYTIME -> anytime
        TodaySection.COMPLETED -> completed
        TodaySection.MISSED -> missed.filterNot { item -> overdue.any { it.taskId == item.taskId } }
    }

    private fun requireNoDuplicateTasks() {
        val taskIds = sectionsInDisplayOrder.flatMap { section -> itemsIn(section).map { item -> item.task.id } }
        require(taskIds.distinct().size == taskIds.size) {
            "A task can appear in only one Today section."
        }
    }

    companion object {
        val sectionsInDisplayOrder: List<TodaySection> = listOf(
            TodaySection.OVERDUE,
            TodaySection.NOW,
            TodaySection.NEXT,
            TodaySection.ANYTIME,
            TodaySection.COMPLETED,
            TodaySection.MISSED,
        )
    }
}

data class TodayProgress(
    val resolvedCount: Int,
    val requiredCount: Int,
) {
    val remainingCount: Int = requiredCount - resolvedCount

    val completionRatio: Double = if (requiredCount == 0) {
        0.0
    } else {
        resolvedCount.toDouble() / requiredCount.toDouble()
    }

    val isComplete: Boolean = requiredCount > 0 && remainingCount == 0

    init {
        require(resolvedCount >= 0) { "Resolved count must not be negative." }
        require(requiredCount >= 0) { "Required count must not be negative." }
        require(resolvedCount <= requiredCount) { "Resolved count must not exceed required count." }
    }
}

data class TodayItem(
    val task: Task,
) {
    val taskId: TaskId = task.id
}

enum class TodayDayStatus {
    OPEN,
    COMPLETE,
}

enum class TodaySection {
    OVERDUE,
    NOW,
    NEXT,
    ANYTIME,
    COMPLETED,
    MISSED,
}
