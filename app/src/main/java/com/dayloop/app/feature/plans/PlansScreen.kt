package com.dayloop.app.feature.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.model.Goal
import com.dayloop.app.core.model.Project
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopHeroCard
import com.dayloop.app.ui.components.DayLoopPillOption
import com.dayloop.app.ui.components.DayLoopPillTabRow
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun PlansRoute(
    initialTab: PlansTab = PlansTab.PROJECTS,
) {
    val appGraph = LocalAppGraph.current
    val projects by appGraph.projectRepository.observeProjects().collectAsState(emptyList())
    val goals by appGraph.goalRepository.observeGoals().collectAsState(emptyList())
    
    var selectedTab by remember { mutableStateOf(initialTab) }

    PlansScreen(
        uiState = PlansUiState(
            projects = projects,
            goals = goals,
            selectedTab = selectedTab
        ),
        onTabSelected = { selectedTab = it }
    )
}

@Composable
fun PlansScreen(
    uiState: PlansUiState,
    onTabSelected: (PlansTab) -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens

    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Plans",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = tokens.spacing.contentGap)
            )

            DayLoopPillTabRow(
                options = listOf(
                    DayLoopPillOption(PlansTab.PROJECTS.name, "Projects"),
                    DayLoopPillOption(PlansTab.GOALS.name, "Goals")
                ),
                selectedOptionId = uiState.selectedTab.name,
                onOptionSelected = { onTabSelected(PlansTab.valueOf(it)) }
            )

            Spacer(Modifier.height(tokens.spacing.contentGap))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap)
            ) {
                if (uiState.selectedTab == PlansTab.PROJECTS) {
                    items(uiState.projects) { project ->
                        ProjectCard(project)
                    }
                    if (uiState.projects.isEmpty()) {
                        item { EmptyState("No projects yet.") }
                    }
                } else {
                    items(uiState.goals) { goal ->
                        GoalCard(goal)
                    }
                    if (uiState.goals.isEmpty()) {
                        item { EmptyState("No goals yet.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project) {
    val tokens = MaterialTheme.dayLoopTokens
    DayLoopHeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap)) {
            Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(project.status.name, style = MaterialTheme.typography.labelSmall, color = tokens.colors.onActionSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun GoalCard(goal: Goal) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(tokens.spacing.contentGap), verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap)) {
            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            goal.targetDate?.let {
                Text("Target: $it", style = MaterialTheme.typography.bodySmall, color = tokens.colors.supportingText)
            }
            Text("Policy: ${goal.progressPolicy}", style = MaterialTheme.typography.labelSmall, color = tokens.colors.supportingText)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(message, color = MaterialTheme.dayLoopTokens.colors.supportingText, modifier = Modifier.padding(16.dp))
}
