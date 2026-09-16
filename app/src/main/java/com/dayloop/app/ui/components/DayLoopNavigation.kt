package com.dayloop.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.designsystem.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayLoopMainNav(
    selected: String,
    onSelected: (String) -> Unit,
    onAdd: (() -> Unit)? = null,
    onHoldAdd: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    addLabel: String = "Add",
    enabled: Boolean = true
) {
    val colors = LocalDayLoopColors.current

    DayLoopBox(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        backgroundColor = colors.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Today
            DayLoopNavItem(
                selected = selected == "today",
                onClick = { onSelected("today") },
                icon = R.drawable.ic_nav_today,
                label = "Today"
            )

            // P & G (Plans & Goals)
            DayLoopNavItem(
                selected = selected == "plan",
                onClick = { onSelected("plan") },
                icon = R.drawable.ic_nav_plan,
                label = "Plans"
            )

            // Central Floating Add Button (Ref 2 style)
            if (onAdd != null || onHoldAdd != null) {
                DayLoopBox(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-16).dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .combinedClickable(
                            enabled = enabled,
                            onClick = { onAdd?.invoke() },
                            onLongClick = onHoldAdd
                        )
                        .padding(4.dp),
                    shape = CircleShape,
                    backgroundColor = colors.primary,
                    contentAlignment = Alignment.Center
                ) {
                    DayLoopIcon(
                        resource = R.drawable.ic_action_add,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(32.dp),
                        description = addLabel
                    )
                }
            }

            // Focus
            DayLoopNavItem(
                selected = selected == "focus",
                onClick = { onSelected("focus") },
                icon = R.drawable.ic_nav_goals, // Using goals icon as placeholder for Focus
                label = "Focus"
            )

            // More / Settings
            DayLoopNavItem(
                selected = selected == "more",
                onClick = { onOpenSettings?.invoke() ?: onSelected("more") },
                icon = R.drawable.ic_action_menu,
                label = "More"
            )
        }
    }
}
