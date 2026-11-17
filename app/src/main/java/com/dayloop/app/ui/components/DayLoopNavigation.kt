package com.dayloop.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.dayloop.app.ui.theme.dayLoopTokens
import com.dayloop.app.R

@Composable
fun DayLoopMainNav(
    selected: String,
    onSelected: (String) -> Unit,
    onAdd: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DayLoopBottomNavItem("today", "Today"),
        DayLoopBottomNavItem("plan", "P&Gs"),
        DayLoopBottomNavItem("focus", "Focus"),
    )

    AnimatedDayLoopNavigation(
        items = items,
        selectedId = selected,
        onSelected = { id ->
            if (enabled) onSelected(id)
        },
        onAddClick = onAdd?.takeIf { selected == "today" || selected == "plan" },
        enabled = enabled,
        modifier = modifier,
        icon = { item, selected ->
            DayLoopNavigationIcon(item.id, selected)
        }
    )
}

@Composable
private fun DayLoopNavigationIcon(id: String, selected: Boolean) {
    val resource = when (id) {
        "today" -> R.drawable.ic_nav_today
        "plan" -> R.drawable.ic_nav_target
        "focus" -> R.drawable.ic_nav_focus
        "goals" -> R.drawable.ic_nav_goals
        else -> R.drawable.ic_action_menu
    }
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = if (selected) MaterialTheme.dayLoopTokens.colors.accentText else MaterialTheme.dayLoopTokens.colors.dockInactive,
    )
}
