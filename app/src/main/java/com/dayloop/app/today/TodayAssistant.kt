package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskSchedule
import java.time.LocalDateTime
import java.time.Duration

data class AssistantSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val task: Task,
    val suggestionType: SuggestionType
)

enum class SuggestionType {
    OVERDUE,
    GAP_FILL,
    PRIORITY_UPGRADE
}

class TodayAssistant {
    fun generateSuggestions(
        snapshot: TodaySnapshot,
        now: LocalDateTime
    ): List<AssistantSuggestion> {
        val suggestions = mutableListOf<AssistantSuggestion>()

        // 1. Overdue Priority
        if (snapshot.overdue.isNotEmpty()) {
            val mostUrgent = snapshot.overdue.first().task
            suggestions.add(
                AssistantSuggestion(
                    id = "overdue-${mostUrgent.id.value}",
                    title = "Handle Overdue Task",
                    description = "Take care of \"${mostUrgent.title}\" now to clear your backlog.",
                    task = mostUrgent,
                    suggestionType = SuggestionType.OVERDUE
                )
            )
        }

        // 2. Gap Detection & High Priority Anytime
        val activeNow = snapshot.now.isNotEmpty()
        if (!activeNow) {
            val nextTask = snapshot.next.firstOrNull()?.task
            val nextStart = (nextTask?.schedule as? TaskSchedule.TimeBlock)?.startsAt
            
            val gapMinutes = if (nextStart != null) {
                Duration.between(now, nextStart).toMinutes()
            } else {
                Long.MAX_VALUE
            }

            if (gapMinutes > 15) {
                val bestAnytime = snapshot.anytime
                    .map { it.task }
                    .sortedByDescending { it.priority.rank }
                    .firstOrNull()

                if (bestAnytime != null) {
                    val label = if (bestAnytime.priority == TaskPriority.HIGH) "High Priority" else "Next Step"
                    suggestions.add(
                        AssistantSuggestion(
                            id = "gap-${bestAnytime.id.value}",
                            title = "$label Suggestion",
                            description = "You have a gap in your schedule. Start \"${bestAnytime.title}\" to stay productive.",
                            task = bestAnytime,
                            suggestionType = if (bestAnytime.priority == TaskPriority.HIGH) SuggestionType.PRIORITY_UPGRADE else SuggestionType.GAP_FILL
                        )
                    )
                }
            }
        }

        return suggestions.take(1) // Only show the most relevant suggestion for now
    }
}
