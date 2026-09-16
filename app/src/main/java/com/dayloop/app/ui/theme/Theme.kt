package com.dayloop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import com.dayloop.app.settings.ThemeMode
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.settings.AppPalette
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun DayLoopTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scale = settings.textScale.takeIf { it.isFinite() }?.coerceIn(1f, 1.3f) ?: 1f
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * scale),
        LocalReducedMotion provides settings.reducedMotion,
    ) {
        DayLoopTheme(
            darkTheme = settings.themeMode.usesDarkTheme(isSystemInDarkTheme()),
            palette = settings.palette,
            fontWeightScale = settings.fontWeightScale,
            font = settings.font,
            customColorHex = settings.customColorHex,
            content = content
        )
    }
}

internal val LightColorScheme = lightColorScheme(
    primaryFixed = CalmPressureLightPrimaryContainer, primaryFixedDim = CalmPressureLightPrimary,
    onPrimaryFixed = CalmPressureLightOnPrimaryContainer, onPrimaryFixedVariant = CalmPressureLightOnSurfaceVariant,
    secondaryFixed = CalmPressureLightSecondaryContainer, secondaryFixedDim = CalmPressureDarkSecondary,
    onSecondaryFixed = CalmPressureLightOnSecondaryContainer, onSecondaryFixedVariant = CalmPressureLightOnSurfaceVariant,
    tertiaryFixed = CalmPressureLightTertiaryContainer, tertiaryFixedDim = CalmPressureDarkTertiary,
    onTertiaryFixed = CalmPressureLightOnTertiaryContainer, onTertiaryFixedVariant = CalmPressureDarkOnTertiary,
    background = CalmPressureLightBackground,
    onBackground = CalmPressureLightOnBackground,
    surface = CalmPressureLightSurface,
    onSurface = CalmPressureLightOnSurface,
    surfaceVariant = CalmPressureLightSurfaceVariant,
    onSurfaceVariant = CalmPressureLightOnSurfaceVariant,
    primary = CalmPressureLightPrimary,
    onPrimary = CalmPressureLightOnPrimary,
    primaryContainer = CalmPressureLightPrimaryContainer,
    onPrimaryContainer = CalmPressureLightOnPrimaryContainer,
    secondary = CalmPressureLightSecondary,
    onSecondary = CalmPressureLightOnSecondary,
    secondaryContainer = CalmPressureLightSecondaryContainer,
    onSecondaryContainer = CalmPressureLightOnSecondaryContainer,
    tertiary = CalmPressureLightTertiary,
    onTertiary = CalmPressureLightOnTertiary,
    tertiaryContainer = CalmPressureLightTertiaryContainer,
    onTertiaryContainer = CalmPressureLightOnTertiaryContainer,
    outline = CalmPressureLightOutline,
    error = CalmPressureLightError,
    onError = CalmPressureLightOnError,
    surfaceTint = CalmPressureLightPrimary,
    surfaceDim = Color(0xFFDCE1D3),
    surfaceBright = CalmPressureLightBackground,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CalmPressureLightBackground,
    surfaceContainer = Color(0xFFEEF2E5),
    surfaceContainerHigh = Color(0xFFE7EBDD),
    surfaceContainerHighest = Color(0xFFE1E6D7),
    outlineVariant = Color(0xFFC5CBB9),
    inverseSurface = CalmPressureDarkSurface,
    inverseOnSurface = CalmPressureDarkOnSurface,
    inversePrimary = CalmPressureDarkPrimary,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkColorScheme = darkColorScheme(
    primaryFixed = CalmPressureLightPrimaryContainer, primaryFixedDim = CalmPressureLightPrimary,
    onPrimaryFixed = CalmPressureLightOnPrimaryContainer, onPrimaryFixedVariant = CalmPressureLightOnSurfaceVariant,
    secondaryFixed = CalmPressureLightSecondaryContainer, secondaryFixedDim = CalmPressureDarkSecondary,
    onSecondaryFixed = CalmPressureLightOnSecondaryContainer, onSecondaryFixedVariant = CalmPressureLightOnSurfaceVariant,
    tertiaryFixed = CalmPressureLightTertiaryContainer, tertiaryFixedDim = CalmPressureDarkTertiary,
    onTertiaryFixed = CalmPressureLightOnTertiaryContainer, onTertiaryFixedVariant = CalmPressureDarkOnTertiary,
    background = CalmPressureDarkBackground,
    onBackground = CalmPressureDarkOnBackground,
    surface = CalmPressureDarkSurface,
    onSurface = CalmPressureDarkOnSurface,
    surfaceVariant = CalmPressureDarkSurfaceVariant,
    onSurfaceVariant = CalmPressureDarkOnSurfaceVariant,
    primary = CalmPressureDarkPrimary,
    onPrimary = CalmPressureDarkOnPrimary,
    primaryContainer = CalmPressureDarkPrimaryContainer,
    onPrimaryContainer = CalmPressureDarkOnPrimaryContainer,
    secondary = CalmPressureDarkSecondary,
    onSecondary = CalmPressureDarkOnSecondary,
    secondaryContainer = CalmPressureDarkSecondaryContainer,
    onSecondaryContainer = CalmPressureDarkOnSecondaryContainer,
    tertiary = CalmPressureDarkTertiary,
    onTertiary = CalmPressureDarkOnTertiary,
    tertiaryContainer = CalmPressureDarkTertiaryContainer,
    onTertiaryContainer = CalmPressureDarkOnTertiaryContainer,
    outline = CalmPressureDarkOutline,
    error = CalmPressureDarkError,
    onError = CalmPressureDarkOnError,
    surfaceTint = CalmPressureDarkPrimary,
    surfaceDim = CalmPressureDarkBackground,
    surfaceBright = Color(0xFF363C33),
    surfaceContainerLowest = Color(0xFF0C100C),
    surfaceContainerLow = CalmPressureDarkSurface,
    surfaceContainer = Color(0xFF20251E),
    surfaceContainerHigh = Color(0xFF292F26),
    surfaceContainerHighest = CalmPressureDarkSurfaceVariant,
    outlineVariant = Color(0xFF454D3E),
    inverseSurface = CalmPressureLightSurface,
    inverseOnSurface = CalmPressureLightOnSurface,
    inversePrimary = Color(0xFF53660F),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun DayLoopTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    DayLoopTheme(darkTheme = themeMode.usesDarkTheme(isSystemInDarkTheme()), content = content)
}

internal fun ThemeMode.usesDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DayLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.MATCHA,
    fontWeightScale: Float = 1f,
    font: com.dayloop.app.settings.AppFont = com.dayloop.app.settings.AppFont.JETBRAINS_MONO,
    customColorHex: String? = null,
    content: @Composable () -> Unit,
) {
    val baseTokens = if (darkTheme) CalmPressureDarkTokens else CalmPressureLightTokens
    val normalized = com.dayloop.app.settings.normalizeHexColor(customColorHex)
    val seed = normalized?.let { Color(it.toColorInt()) } ?: palette.seedColor()
    val mapper = androidx.compose.runtime.remember(seed) { MatchaPalette(seed) }
    val scheme = androidx.compose.runtime.remember(seed, darkTheme) {
        mapper.scheme(if (darkTheme) DarkColorScheme else LightColorScheme)
    }

    val tokens = if (seed == Color(0xFFA8CF3D)) baseTokens else DayLoopTokens(
        spacing = baseTokens.spacing, shapeScale = baseTokens.shapeScale,
        colors = DayLoopColorTokens(
            supportingText = scheme.onSurfaceVariant, quietAccent = scheme.secondary,
            actionSurface = mapper.tone(baseTokens.colors.actionSurface),
            onActionSurface = readable(mapper.tone(baseTokens.colors.onActionSurface), listOf(mapper.tone(baseTokens.colors.actionSurface))),
            actionAccent = readable(seed, listOf(mapper.tone(baseTokens.colors.actionSurface))),
            appField = mapper.tone(baseTokens.colors.appField), progressTrack = mapper.tone(baseTokens.colors.progressTrack),
            warning = baseTokens.colors.warning,
        ),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val background = scheme.background
                window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
                if (Build.VERSION.SDK_INT < 35) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = if (Build.VERSION.SDK_INT < 27) {
                        CalmPressureDarkBackground.toArgb()
                    } else background.toArgb()
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalDayLoopTokens provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = scaledTypography(fontWeightScale, font),
            content = content,
        )
    }
}
