package com.dayloop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.LocalReducedMotion
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
internal fun AnimatedDayLoopNavigation(items: List<DayLoopBottomNavItem>, selectedId: String,
    onSelected: (String) -> Unit, modifier: Modifier,
    icon: @Composable (DayLoopBottomNavItem, Boolean) -> Unit) {
    val tabs = items.filter { it.id != "more" }
    if (tabs.isEmpty()) return
    val reduced = LocalReducedMotion.current
    val colors = MaterialTheme.dayLoopTokens.colors
    val height = 72.dp + (12f * (LocalDensity.current.fontScale - 1f).coerceIn(0f, 3f)).dp
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(36.dp),
        color = colors.actionSurface, contentColor = colors.onActionSurface) {
        Row(Modifier.fillMaxWidth().height(height).padding(horizontal = 6.dp).selectableGroup()) {
            tabs.forEach { item ->
                val active = item.id == selectedId
                val colour by animateColorAsState(if (active) colors.onActionSurface else colors.onActionSurface.copy(alpha = 0.66f),
                    if (reduced) snap() else tween(180), label = "tabStroke")
                val scale = remember { Animatable(1f) }
                LaunchedEffect(active, reduced) {
                    if (active && !reduced) {
                        scale.animateTo(1.08f, tween(90)); scale.animateTo(1f, tween(150))
                    } else scale.snapTo(1f)
                }
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                    .selectable(active, role = Role.Tab, onClick = { onSelected(item.id) }).padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                    CompositionLocalProvider(LocalContentColor provides colour) {
                        Box(Modifier.size(28.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                            contentAlignment = Alignment.Center) { icon(item, active) }
                    }
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = colour,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
