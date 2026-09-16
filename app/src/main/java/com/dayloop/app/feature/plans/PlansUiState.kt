package com.dayloop.app.feature.plans

import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.Project

data class PlansUiState(
    val projects: List<Project> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val selectedTab: PlansTab = PlansTab.PROJECTS,
) {
    val isEmpty: Boolean = projects.isEmpty() && goals.isEmpty()
}

enum class PlansTab {
    PROJECTS,
    GOALS,
}
