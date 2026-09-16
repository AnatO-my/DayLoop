package com.dayloop.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.R
import com.dayloop.app.ui.theme.dayLoopTokens

@Immutable
data class DayLoopPillOption(
    val id: String,
    val label: String,
)

@Immutable
data class DayLoopScheduleOption(
    val id: String,
    val label: String,
    val supportingLabel: String = "",
)

@Immutable
data class DayLoopBottomNavItem(
    val id: String,
    val label: String,
)

@Composable
fun DayLoopScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.dayLoopTokens.spacing.screenPadding),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun DayLoopHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.expressiveCorner),
        color = tokens.colors.actionSurface,
        contentColor = tokens.colors.onActionSurface,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(tokens.spacing.contentGap)) {
            content()
        }
    }
}

@Composable
fun DayLoopMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Surface(
        modifier = modifier.height(88.dp + 40.dp * LocalDensity.current.fontScale),
        shape = RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        val targetVal = targetState.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        val initialVal = initialState.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        if (targetVal > initialVal) {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "metricRoll"
                ) { targetValue ->
                    BasicText(targetValue, maxLines = 1, modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium.copy(color = accentColor, textAlign = TextAlign.Center),
                        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 26.sp))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                BasicText(if (label == "Focus streak") "Focus\nstreak" else label, maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(color = tokens.colors.supportingText, textAlign = TextAlign.Center),
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 12.sp))
            }
        }
    }
}

@Composable
fun DayLoopBottomActionBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.colors.actionSurface,
            contentColor = tokens.colors.onActionSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = if (trailing == null) TextAlign.Center else TextAlign.Start,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(tokens.spacing.compactGap))
            trailing()
        }
    }
}

@Composable
fun DayLoopPillTabRow(
    options: List<DayLoopPillOption>,
    selectedOptionId: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.shapeScale.pillCorner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(tokens.spacing.microGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
    ) {
        options.forEach { option ->
            val selected = option.id == selectedOptionId
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(tokens.shapeScale.pillCorner))
                    .clickable { onOptionSelected(option.id) }
                    .semantics {
                        contentDescription = option.label
                        role = Role.Tab
                    },
                shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(Modifier.height(52.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    BasicText(option.label, modifier = Modifier.fillMaxWidth(), maxLines = 1,
                        style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp))
                }
            }
        }
    }
}

@Composable
fun DayLoopScheduleSelector(
    options: List<DayLoopScheduleOption>,
    selectedOptionId: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionIcon: @Composable (DayLoopScheduleOption, Boolean) -> Unit = { _, selected ->
        DayLoopTemporaryIconDot(selected = selected)
    },
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
    ) {
        options.forEach { option ->
            val selected = option.id == selectedOptionId
            Surface(
                modifier = Modifier
                    .widthIn(min = 104.dp)
                    .clip(RoundedCornerShape(tokens.shapeScale.comfortableCorner))
                    .clickable { onOptionSelected(option.id) }
                    .semantics {
                        contentDescription = "${option.label} schedule"
                        role = Role.Button
                    },
                shape = RoundedCornerShape(tokens.shapeScale.comfortableCorner),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
                ) {
                    optionIcon(option, selected)
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    if (option.supportingLabel.isNotBlank()) {
                        Text(
                            text = option.supportingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else tokens.colors.supportingText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayLoopProgressRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progressFill"
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = tokens.colors.progressTrack,
            strokeWidth = 8.dp,
        )
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = LocalContentColor.current,
        )
    }
}

@Composable
fun DayLoopWarningBanner(
    title: String,
    body: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.dayLoopTokens

    DayLoopHeroCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading() else DayLoopWarningDot()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.colors.onActionSurface.copy(alpha = 0.72f),
                )
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
fun DayLoopBottomNav(
    items: List<DayLoopBottomNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemIcon: @Composable (DayLoopBottomNavItem, Boolean) -> Unit = { item, selected ->
        DayLoopNavigationIcon(item.id, selected)
    },
) {
    AnimatedDayLoopNavigation(items, selectedItemId, onItemSelected, modifier, itemIcon)
}

@Composable
private fun DayLoopNavigationIcon(id: String, selected: Boolean) {
    val resource = when (id) {
        "today" -> R.drawable.ic_nav_today
        "plan" -> R.drawable.ic_nav_plan
        "focus" -> R.drawable.ic_nav_focus
        "goals" -> R.drawable.ic_nav_goals
        else -> R.drawable.ic_nav_more
    }
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = LocalContentColor.current,
    )
}

@Composable
private fun DayLoopTemporaryIconDot(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.dayLoopTokens.colors.progressTrack),
    )
}

@Composable
private fun DayLoopWarningDot() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.dayLoopTokens.colors.warning),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "!",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayLoopComponentsPreview() {
    DayLoopTheme(darkTheme = false) {
        DayLoopScreen {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dayLoopTokens.spacing.contentGap)) {
                DayLoopHeroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(text = "Daily progress", fontWeight = FontWeight.SemiBold)
                            Text(text = "9 / 12 tasks")
                        }
                        DayLoopProgressRing(progress = 0.72f, label = "72 percent complete")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dayLoopTokens.spacing.compactGap)) {
                    DayLoopMetricCard(value = "12", label = "day streak", modifier = Modifier.weight(1f))
                    DayLoopMetricCard(value = "87", label = "focus score", modifier = Modifier.weight(1f))
                }
                DayLoopPillTabRow(
                    options = listOf(
                        DayLoopPillOption("time", "Time block"),
                        DayLoopPillOption("interval", "Interval"),
                    ),
                    selectedOptionId = "time",
                    onOptionSelected = {},
                )
                DayLoopScheduleSelector(
                    options = listOf(
                        DayLoopScheduleOption("now", "Now", "Do it soon"),
                        DayLoopScheduleOption("next", "Next", "In order"),
                        DayLoopScheduleOption("anytime", "Anytime", "No time"),
                    ),
                    selectedOptionId = "now",
                    onOptionSelected = {},
                )
                DayLoopWarningBanner(
                    title = "3 reminders need attention",
                    body = "These may be late or missed.",
                    actionText = "Review",
                    onAction = {},
                )
                DayLoopBottomActionBar(text = "Start the day", onClick = {}) {
                    Text(text = "+")
                }
                DayLoopBottomNav(
                    items = listOf(
                        DayLoopBottomNavItem("today", "Today"),
                        DayLoopBottomNavItem("plan", "Plan"),
                        DayLoopBottomNavItem("focus", "Focus"),
                        DayLoopBottomNavItem("goals", "Goals"),
                        DayLoopBottomNavItem("more", "More"),
                    ),
                    selectedItemId = "today",
                    onItemSelected = {},
                )
            }
        }
    }
}
