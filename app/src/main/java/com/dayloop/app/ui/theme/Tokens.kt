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
    val microGap: Dp,     // space.1 = 4dp
    val relatedGap: Dp,   // space.2 = 8dp
    val itemGap: Dp,      // space.3 = 12dp
    val cardPadding: Dp,  // space.4 = 16dp
    val pageMargin: Dp,   // space.5 = 20dp
    val sectionGap: Dp,   // space.6 = 24dp
    val extraGap: Dp,     // space.8 = 32dp
    val deepGap: Dp,      // space.10 = 40dp
    val maxGap: Dp,       // space.12 = 48dp
)

@Immutable
class DayLoopShapeScale internal constructor(
    val smallCorner: Dp,      // 8dp
    val inputCorner: Dp,      // 16dp
    val cardCorner: Dp,       // 24dp
    val sheetTopCorner: Dp,   // 28dp
    val dockCorner: Dp,       // 28dp
    val pillCorner: Dp,       // height/2
)

@Immutable
class DayLoopColorTokens internal constructor(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val decorativeBorder: Color,
    val controlBorder: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentSubtle: Color,
    val dock: Color,
    val dockInactive: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val progressTrack: Color,
    val actionSurface: Color,
    val onActionSurface: Color,
)

@Immutable
class DayLoopTokens internal constructor(
    val spacing: DayLoopSpacing,
    val shapeScale: DayLoopShapeScale,
    val colors: DayLoopColorTokens,
)

internal val DefaultDayLoopSpacing = DayLoopSpacing(
    microGap = 4.dp,
    relatedGap = 8.dp,
    itemGap = 12.dp,
    cardPadding = 16.dp,
    pageMargin = 24.dp,
    sectionGap = 24.dp,
    extraGap = 32.dp,
    deepGap = 40.dp,
    maxGap = 48.dp,
)

internal val DefaultDayLoopShapeScale = DayLoopShapeScale(
    smallCorner = 2.dp,
    inputCorner = 4.dp,
    cardCorner = 4.dp,
    sheetTopCorner = 6.dp,
    dockCorner = 8.dp,
    pillCorner = 4.dp,
)

internal val DayLoopLightTokens = DayLoopTokens(
    spacing = DefaultDayLoopSpacing,
    shapeScale = DefaultDayLoopShapeScale,
    colors = DayLoopColorTokens(
        background = DayLoopLightBackground,
        surface = DayLoopLightSurface,
        raised = DayLoopLightRaised,
        text = DayLoopLightText,
        secondaryText = DayLoopLightSecondaryText,
        mutedText = DayLoopLightMutedText,
        decorativeBorder = DayLoopLightDecorativeBorder,
        controlBorder = DayLoopLightControlBorder,
        accent = DayLoopAccent,
        onAccent = DayLoopOnAccent,
        accentText = DayLoopLightAccentText,
        accentSubtle = DayLoopLightAccentSubtle,
        dock = DayLoopLightDock,
        dockInactive = DayLoopLightSecondaryText,
        success = DayLoopLightSuccess,
        warning = DayLoopLightWarning,
        error = DayLoopLightError,
        info = DayLoopLightInfo,
        progressTrack = DayLoopLightRaised,
        actionSurface = DayLoopLightSurface,
        onActionSurface = DayLoopLightText,
    ),
)

internal val DayLoopDarkTokens = DayLoopTokens(
    spacing = DefaultDayLoopSpacing,
    shapeScale = DefaultDayLoopShapeScale,
    colors = DayLoopColorTokens(
        background = DayLoopDarkBackground,
        surface = DayLoopDarkSurface,
        raised = DayLoopDarkRaised,
        text = DayLoopDarkText,
        secondaryText = DayLoopDarkSecondaryText,
        mutedText = DayLoopDarkMutedText,
        decorativeBorder = DayLoopDarkDecorativeBorder,
        controlBorder = DayLoopDarkControlBorder,
        accent = DayLoopAccent,
        onAccent = DayLoopOnAccent,
        accentText = DayLoopDarkAccentText,
        accentSubtle = DayLoopDarkAccentSubtle,
        dock = DayLoopDarkDock,
        dockInactive = DayLoopDarkSecondaryText,
        success = DayLoopDarkSuccess,
        warning = DayLoopDarkWarning,
        error = DayLoopDarkError,
        info = DayLoopDarkInfo,
        progressTrack = DayLoopDarkRaised,
        actionSurface = DayLoopDarkSurface,
        onActionSurface = DayLoopDarkText,
    ),
)

internal val LocalDayLoopTokens = staticCompositionLocalOf { DayLoopLightTokens }

val MaterialTheme.dayLoopTokens: DayLoopTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDayLoopTokens.current
