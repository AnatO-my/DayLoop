package com.dayloop.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dayloop.app.ui.designsystem.LocalDayLoopColors

@Composable
fun DayLoopIcon(
    @DrawableRes resource: Int,
    description: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalDayLoopColors.current.onBackground
) {
    Image(
        painter = painterResource(id = resource),
        contentDescription = description,
        modifier = modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint)
    )
}

/**
 * Backward compatible signature for IconButton
 */
@Composable
fun DayLoopIconButton(
    @DrawableRes resource: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalDayLoopColors.current.onBackground
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        DayLoopIcon(
            resource = resource,
            description = label,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}
