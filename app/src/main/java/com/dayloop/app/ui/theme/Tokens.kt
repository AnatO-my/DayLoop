package com.dayloop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class DayLoopSpacing internal constructor(
    val screenPadding: Dp,
    val contentGap: Dp,
    val compactGap: Dp,
    val microGap: Dp,
)

@Immutable
class DayLoopShapeScale internal constructor(
    val compactCorner: Dp,
    val comfortableCorner: Dp,
    val expressiveCorner: Dp,
    val pillCorner: Dp,
)

@Immutable
class DayLoopColorTokens internal constructor(
    val supportingText: Color,
    val quietAccent: Color,
    val actionSurface: Color,
    val onActionSurface: Color,
    val appField: Color,
    val progressTrack: Color,
    val warning: Color,
    val actionAccent: Color = quietAccent,
)

@Immutable
class DayLoopTokens internal constructor(
    val spacing: DayLoopSpacing,
    val shapeScale: DayLoopShapeScale,
    val colors: DayLoopColorTokens,
)

internal val CalmPressureLightTokens = DayLoopTokens(
    spacing = DayLoopSpacing(
        screenPadding = 24.dp,
        contentGap = 16.dp,
        compactGap = 8.dp,
        microGap = 4.dp,
    ),
    shapeScale = DayLoopShapeScale(
        compactCorner = 14.dp,
        comfortableCorner = 20.dp,
        expressiveCorner = 28.dp,
        pillCorner = 999.dp,
    ),
    colors = DayLoopColorTokens(
        supportingText = CalmPressureLightSupportingText,
        quietAccent = CalmPressureLightQuietAccent,
        actionSurface = CalmPressureLightActionSurface,
        onActionSurface = CalmPressureLightOnActionSurface,
        appField = CalmPressureLightAppField,
        progressTrack = CalmPressureLightProgressTrack,
        warning = CalmPressureLightWarning,
        actionAccent = CalmPressureLightPrimary,
    ),
)

internal val CalmPressureDarkTokens = DayLoopTokens(
    spacing = DayLoopSpacing(
        screenPadding = 24.dp,
        contentGap = 16.dp,
        compactGap = 8.dp,
        microGap = 4.dp,
    ),
    shapeScale = DayLoopShapeScale(
        compactCorner = 14.dp,
        comfortableCorner = 20.dp,
        expressiveCorner = 28.dp,
        pillCorner = 999.dp,
    ),
    colors = DayLoopColorTokens(
        supportingText = CalmPressureDarkSupportingText,
        quietAccent = CalmPressureDarkQuietAccent,
        actionSurface = CalmPressureDarkActionSurface,
        onActionSurface = CalmPressureDarkOnActionSurface,
        appField = CalmPressureDarkAppField,
        progressTrack = CalmPressureDarkProgressTrack,
        warning = CalmPressureDarkWarning,
        actionAccent = CalmPressureDarkPrimary,
    ),
)

internal val LocalDayLoopTokens = staticCompositionLocalOf { CalmPressureLightTokens }

val MaterialTheme.dayLoopTokens: DayLoopTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDayLoopTokens.current
