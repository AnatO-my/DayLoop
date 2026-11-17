package com.dayloop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
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
import com.dayloop.app.settings.AppFont

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
    primary = DayLoopAccent,
    onPrimary = DayLoopOnAccent,
    primaryContainer = DayLoopLightAccentSubtle,
    onPrimaryContainer = DayLoopLightAccentText,
    secondary = DayLoopCategoryMint,
    onSecondary = DayLoopOnCategory,
    secondaryContainer = DayLoopLightRaised,
    onSecondaryContainer = DayLoopLightSecondaryText,
    tertiary = DayLoopCategoryLavender,
    onTertiary = DayLoopOnCategory,
    background = DayLoopLightBackground,
    onBackground = DayLoopLightText,
    surface = DayLoopLightSurface,
    onSurface = DayLoopLightText,
    surfaceVariant = DayLoopLightRaised,
    onSurfaceVariant = DayLoopLightSecondaryText,
    outline = DayLoopLightMutedText,
    outlineVariant = DayLoopLightDecorativeBorder,
    error = DayLoopLightError,
    onError = Color.White,
    surfaceContainerLow = DayLoopLightRaised,
    surfaceContainerLowest = Color.White,
    surfaceContainer = DayLoopLightRaised,
    surfaceContainerHigh = DayLoopLightRaised,
    surfaceContainerHighest = Color(0xFFDDE1E4),
    tertiaryContainer = DayLoopCategoryLavender,
    onTertiaryContainer = DayLoopOnCategory,
    inverseSurface = Color(0xFF191A18),
    inverseOnSurface = Color(0xFFF4F8F9),
    inversePrimary = DayLoopAccent,
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFDDE1E4),
    surfaceBright = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkColorScheme = darkColorScheme(
    primary = DayLoopAccent,
    onPrimary = DayLoopOnAccent,
    primaryContainer = DayLoopDarkAccentSubtle,
    onPrimaryContainer = DayLoopDarkAccentText,
    secondary = DayLoopCategoryMint,
    onSecondary = DayLoopOnCategory,
    secondaryContainer = DayLoopDarkRaised,
    onSecondaryContainer = DayLoopDarkSecondaryText,
    tertiary = DayLoopCategoryLavender,
    onTertiary = DayLoopOnCategory,
    background = DayLoopDarkBackground,
    onBackground = DayLoopDarkText,
    surface = DayLoopDarkSurface,
    onSurface = DayLoopDarkText,
    surfaceVariant = DayLoopDarkRaised,
    onSurfaceVariant = DayLoopDarkSecondaryText,
    outline = DayLoopDarkMutedText,
    outlineVariant = DayLoopDarkDecorativeBorder,
    error = DayLoopDarkError,
    onError = Color.Black,
    surfaceContainerLow = DayLoopDarkRaised,
    surfaceContainerLowest = DayLoopDarkBackground,
    surfaceContainer = DayLoopDarkSurface,
    surfaceContainerHigh = DayLoopDarkRaised,
    surfaceContainerHighest = Color(0xFF30363A),
    tertiaryContainer = DayLoopCategoryLavender,
    onTertiaryContainer = DayLoopOnCategory,
    inverseSurface = Color(0xFFF4F8F9),
    inverseOnSurface = Color(0xFF191A18),
    inversePrimary = DayLoopLightAccentText,
    surfaceTint = Color.Transparent,
    surfaceDim = DayLoopDarkBackground,
    surfaceBright = DayLoopDarkRaised,
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
@Suppress("UNUSED_PARAMETER")
fun DayLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.MATCHA,
    fontWeightScale: Float = 1f,
    font: AppFont = AppFont.FUNNEL_DISPLAY,
    customColorHex: String? = null,
    content: @Composable () -> Unit,
) {
    val baseTokens = if (darkTheme) DayLoopDarkTokens else DayLoopLightTokens
    // Palette stays fixed; the saved font preference remains user-selectable.
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = baseTokens

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
                        DayLoopDarkBackground.toArgb()
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
            shapes = Shapes(
                extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(4.dp), large = RoundedCornerShape(4.dp),
                extraLarge = RoundedCornerShape(8.dp)),
            content = content,
        )
    }
}
