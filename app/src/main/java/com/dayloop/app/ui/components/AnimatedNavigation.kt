package com.dayloop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.ui.theme.DayLoopTokens
import com.dayloop.app.ui.theme.LocalReducedMotion
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
internal fun AnimatedDayLoopNavigation(
    items: List<DayLoopBottomNavItem>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    onAddLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    icon: @Composable (DayLoopBottomNavItem, Boolean) -> Unit
) {
    val reduced = LocalReducedMotion.current
    val tokens = MaterialTheme.dayLoopTokens

    Column(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onAddClick != null) {
            FilledIconButton(
                onClick = onAddClick, enabled = enabled,
                modifier = Modifier.size(56.dp).let {
                    when (selectedId) {
                        "today" -> it.guideTarget(GuideTargets.TODAY_ADD_TASK)
                        "plan" -> it.guideTarget(GuideTargets.PLANS_ADD)
                        else -> it
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = tokens.colors.accent, contentColor = tokens.colors.onAccent)
            ) {
                DayLoopIcon(R.drawable.ic_action_add, "Add", modifier = Modifier.size(24.dp), tint = tokens.colors.onAccent)
            }
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = tokens.colors.dock,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                NavigationItem(item, selectedId == item.id, reduced, tokens, onSelected, enabled, icon,
                    modifier = if (item.id == "today") Modifier.guideTarget(GuideTargets.TODAY_NAVIGATION) else Modifier)
            }
        }
    }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RowScope.NavigationItem(
    item: DayLoopBottomNavItem,
    active: Boolean,
    reduced: Boolean,
    tokens: DayLoopTokens,
    onSelected: (String) -> Unit,
    enabled: Boolean,
    icon: @Composable (DayLoopBottomNavItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colour by animateColorAsState(
        targetValue = if (active) tokens.colors.accentText else tokens.colors.dockInactive,
        animationSpec = if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tabStroke"
    )
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active, reduced) {
        if (active && !reduced) {
            scale.animateTo(1.1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else scale.snapTo(1f)
    }

    // Row weight belongs to a direct layout child, not TooltipBox's internal layout.
    Box(modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(item.label) } },
        state = rememberTooltipState(),
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .selectable(active, enabled = enabled, role = Role.Tab, onClick = { onSelected(item.id) })
            .semantics { contentDescription = item.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            contentAlignment = Alignment.Center
        ) {
            icon(item, active)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(24.dp).height(2.dp).background(if (active) colour else Color.Transparent))
    }
    }
    }
}
