package com.dayloop.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens

@Immutable
data class DayLoopPillOption(
    val id: String,
    val label: String,
)

@Immutable
data class DayLoopBottomNavItem(
    val id: String,
    val label: String,
)

@Immutable
data class DayLoopScheduleOption(
    val id: String,
    val label: String,
    val description: String,
)

@Composable
fun DayLoopScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.dayLoopTokens.spacing.pageMargin),
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
    backgroundColor: Color = MaterialTheme.dayLoopTokens.colors.actionSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val shape = RoundedCornerShape(tokens.shapeScale.cardCorner)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColorFor(backgroundColor),
        tonalElevation = 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Box(modifier = Modifier.padding(tokens.spacing.cardPadding)) {
            content()
        }
    }
}

@Composable
fun DayLoopProgressRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.dayLoopTokens.colors.accent,
    trackColor: Color = MaterialTheme.dayLoopTokens.colors.progressTrack.copy(alpha = 0.2f),
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.dayLoopTokens.colors.onActionSurface
        )
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    reportingPeriod: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.dayLoopTokens.colors.accentText,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val shape = RoundedCornerShape(tokens.shapeScale.cardCorner)

    Surface(
        modifier = modifier.heightIn(min = 144.dp),
        shape = shape,
        color = backgroundColor,
        contentColor = foregroundColor,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(tokens.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = accentColor,
                maxLines = 1,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                )
                Text(
                    text = reportingPeriod,
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
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
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .bounceClick(enabled = enabled, interactionSource = interactionSource),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
            textAlign = if (trailing == null) TextAlign.Center else TextAlign.Start,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(tokens.spacing.relatedGap))
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
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedLabelColor: Color = MaterialTheme.dayLoopTokens.colors.accentText,
) {
    val tokens = MaterialTheme.dayLoopTokens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.shapeScale.pillCorner))
            .background(MaterialTheme.colorScheme.background)
            .padding(tokens.spacing.microGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
    ) {
        options.forEach { option ->
            val selected = option.id == selectedOptionId
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(tokens.shapeScale.pillCorner))
                    .bounceClick(interactionSource = interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = { onOptionSelected(option.id) }
                    )
                    .semantics {
                        contentDescription = option.label
                        role = Role.Tab
                    },
                shape = RoundedCornerShape(tokens.shapeScale.pillCorner),
                color = Color.Transparent,
                contentColor = if (selected) selectedLabelColor else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Column {
                Box(Modifier.height(48.dp).fillMaxWidth().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLoopIconButton(
    @DrawableRes resource: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.dayLoopTokens.colors.text
) {
    val interactionSource = remember { MutableInteractionSource() }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(isPersistent = false),
        enableUserInput = enabled,
        hasAction = false
    ) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .bounceClick(enabled = enabled, interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = label,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            DayLoopIcon(
                resource = resource,
                description = label,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = null,
) {
    val tokens = MaterialTheme.dayLoopTokens
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (count != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, tokens.colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (onActionClick != null && actionLabel != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    title: String,
    dueLabel: String?,
    projectLabel: String?,
    modifier: Modifier = Modifier,
    completed: Boolean = false,
    description: String? = null,
    onClick: () -> Unit = {},
) {
    val tokens = MaterialTheme.dayLoopTokens
    DayLoopHeroCard(
        modifier = modifier,
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.inverseSurface
    ) {
        val ink = MaterialTheme.colorScheme.inverseOnSurface
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!dueLabel.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent) {
                        Text(dueLabel, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                if (projectLabel != null) {
                    Text(
                        text = projectLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.W600
                    )
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
        }
    }
}

@Composable
fun MetadataChip(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        modifier = modifier
            .heightIn(min = 28.dp)
            .border(1.dp, color, RoundedCornerShape(tokens.shapeScale.inputCorner)),
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
        color = Color.Transparent,
        contentColor = color,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProjectCard(
    name: String,
    progress: Float,
    effortLabel: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
) {
    val tokens = MaterialTheme.dayLoopTokens
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = tokens.colors.decorativeBorder)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(effortLabel, style = MaterialTheme.typography.bodySmall, color = tokens.colors.secondaryText)
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = tokens.colors.accentText, trackColor = tokens.colors.progressTrack)
            }
            DayLoopIcon(R.drawable.ic_action_forward, tint = tokens.colors.secondaryText)
        }
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
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading() else DayLoopWarningDot()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.secondaryText,
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
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun DayLoopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val tokens = MaterialTheme.dayLoopTokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.colors.accent,
            unfocusedBorderColor = tokens.colors.controlBorder,
            errorBorderColor = tokens.colors.error,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        )
    )
}

@Composable
fun DayLoopBottomNav(
    items: List<DayLoopBottomNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    itemIcon: @Composable (DayLoopBottomNavItem, Boolean) -> Unit = { item, selected ->
        DayLoopNavigationIcon(item.id, selected)
    },
) {
    AnimatedDayLoopNavigation(
        items = items,
        selectedId = selectedItemId,
        onSelected = onItemSelected,
        modifier = modifier,
        onAddClick = onAdd,
        icon = itemIcon
    )
}

@Composable
fun DayLoopScheduleSelector(
    options: List<DayLoopScheduleOption>,
    selectedOptionId: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)
    ) {
        options.forEach { option ->
            val selected = option.id == selectedOptionId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.shapeScale.cardCorner))
                    .clickable { onOptionSelected(option.id) }
                    .semantics {
                        contentDescription = "${option.label} schedule"
                        role = Role.RadioButton
                    },
                shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            ) {
                Column(Modifier.padding(tokens.spacing.cardPadding)) {
                    Text(option.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) LocalContentColor.current.copy(alpha = 0.8f) else tokens.colors.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun DayLoopNavigationIcon(id: String, selected: Boolean) {
    val resource = when (id) {
        "today" -> R.drawable.ic_nav_today
        "plan" -> R.drawable.ic_nav_plan
        "focus" -> R.drawable.ic_nav_focus
        "goals" -> R.drawable.ic_nav_goals
        else -> R.drawable.ic_action_menu
    }
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = LocalContentColor.current,
    )
}

@Preview(showBackground = true)
@Composable
private fun DayLoopComponentsPreview() {
    DayLoopTheme(darkTheme = false) {
        DayLoopScreen {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dayLoopTokens.spacing.sectionGap)) {
                TaskCard(
                    title = "Research for Flix Media",
                    dueLabel = "Due 11:30am",
                    projectLabel = "Marketing",
                    description = "Gathering data for user persona..."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dayLoopTokens.spacing.itemGap)) {
                    StatCard(value = "30", label = "Tasks Completed", reportingPeriod = "Today", modifier = Modifier.weight(1f))
                    StatCard(value = "16", label = "Tasks planned", reportingPeriod = "TUE.13.JAN", modifier = Modifier.weight(1f))
                }
                ProjectCard(
                    name = "Admin Dashboard",
                    progress = 0.45f,
                    effortLabel = "45 hours",
                    icon = { Text("💼") }
                )
            }
        }
    }
}


/** Filled primary action with the theme's squared corners. Use instead of Material's pill-shaped Button. */
@Composable
fun DayLoopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.inputCorner),
        content = content,
    )
}

/** Outlined secondary action with the same corners as [DayLoopButton]. */
@Composable
fun DayLoopOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.inputCorner),
        content = content,
    )
}
