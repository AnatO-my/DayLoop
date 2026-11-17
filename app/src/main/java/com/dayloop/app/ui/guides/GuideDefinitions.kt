package com.dayloop.app.ui.guides

import com.dayloop.app.settings.GuideKey
import com.dayloop.app.settings.GuideAction
import com.dayloop.app.settings.GuideProgress
import com.dayloop.app.settings.GuideStatus

object GuideDefinitions {
    val FirstLaunch = GuideKey("first_launch", version = 1)
    val Today = GuideKey("today", version = 1)
    val TaskEditor = GuideKey("task_editor", version = 1)
    val Scheduling = GuideKey("scheduling", version = 1)
    val Plans = GuideKey("plans", version = 1)
    val Focus = GuideKey("focus", version = 1)
    val Wardrobe = GuideKey("wardrobe", version = 1)
    val Calendar = GuideKey("calendar", version = 1)

    fun select(first: GuideProgress?, today: GuideProgress?, firstLaunch: Boolean): GuideKey? {
        if (first == null || today == null) return null
        return when {
            first.status == GuideStatus.IN_PROGRESS || (firstLaunch && first.status == GuideStatus.UNSEEN) -> FirstLaunch
            today.status == GuideStatus.UNSEEN || today.status == GuideStatus.IN_PROGRESS -> Today
            else -> null
        }
    }

    fun nextAction(key: GuideKey, stepId: String): GuideAction? {
        val steps = getSteps(key)
        val index = steps.indexOfFirst { it.id == stepId }
        if (index < 0) return null
        return steps.getOrNull(index + 1)?.let { GuideAction.Advance(it.id) } ?: GuideAction.Complete
    }

    fun getSteps(key: GuideKey): List<GuideStep> = when (key) {
        FirstLaunch -> listOf(
            GuideStep(
                id = "orientation",
                targetId = GuideTargets.TODAY_NAVIGATION,
                title = "Welcome to DayLoop",
                message = "DayLoop is organized into three areas: Today for daily work, P&Gs for planning, and Focus for deep work timers."
            ),
            GuideStep(
                id = "create_task",
                targetId = GuideTargets.TODAY_ADD_TASK,
                title = "Add a task",
                message = "Tap the plus button to create your first task for today.",
                nextActionLabel = "I'll do that later",
                isFinal = true
            )
        )
        Today -> listOf(
            GuideStep(
                id = "summaries",
                targetId = GuideTargets.TODAY_SUMMARY,
                title = "Daily Progress",
                message = "These cards show your completed and remaining tasks. Tap them to see the details."
            ),
            GuideStep(
                id = "gestures",
                targetId = GuideTargets.TODAY_TASK_ROW,
                title = "Task Actions",
                message = "Swipe left to complete a task, or right for timing options. Hold for even more actions."
            ),
            GuideStep(
                id = "more",
                targetId = GuideTargets.TODAY_MORE,
                title = "More Options",
                message = "Tap the mascot to access settings, notes, and diagnostics.",
                isFinal = true
            )
        )
        TaskEditor -> listOf(
            GuideStep(
                id = "title",
                targetId = GuideTargets.EDITOR_TITLE,
                title = "Task Title",
                message = "Give your task a clear name. You can also add a description for more detail."
            ),
            GuideStep(
                id = "subtasks",
                targetId = GuideTargets.EDITOR_SUBTASKS,
                title = "Subtasks",
                message = "Break large tasks into smaller, manageable steps."
            ),
            GuideStep(
                id = "settings",
                targetId = GuideTargets.EDITOR_SETTINGS,
                title = "Task Settings",
                message = "Tap here to configure scheduling, priority, and reminders.",
                isFinal = true
            )
        )
        Scheduling -> listOf(
            GuideStep(
                id = "schedule_mode",
                targetId = GuideTargets.SETTINGS_SCHEDULE,
                title = "Schedule",
                message = "Choose how you want to track this task: on a simple date, with a deadline, as a time block, or as a recurring interval/repeat rule.",
                isFinal = true
            )
        )
        Plans -> listOf(
            GuideStep(
                id = "tabs",
                targetId = GuideTargets.PLANS_TABS,
                title = "Projects & Goals",
                message = "Switch between Projects (short-term work) and Goals (long-term outcomes)."
            ),
            GuideStep(
                id = "add_planning",
                targetId = GuideTargets.PLANS_ADD,
                title = "Start Planning",
                message = "Tap the plus to create a new project or goal.",
                isFinal = true
            )
        )
        Focus -> listOf(
            GuideStep(
                id = "timer",
                targetId = GuideTargets.FOCUS_TIMER,
                title = "Deep Work",
                message = "Start a Pomodoro or break timer to maintain your focus and energy.",
                isFinal = true
            )
        )
        Wardrobe -> listOf(
            GuideStep(
                id = "add_clothes",
                targetId = GuideTargets.WARDROBE_ADD,
                title = "My Wardrobe",
                message = "Add your clothes here to get outfit suggestions and track what you wear.",
                isFinal = true
            )
        )
        Calendar -> listOf(
            GuideStep(
                id = "dots",
                targetId = GuideTargets.CALENDAR_DOTS,
                title = "Style History",
                message = "Tap a date to see what you wore. Dots indicate recorded outfits.",
                isFinal = true
            )
        )
        else -> emptyList()
    }
}
