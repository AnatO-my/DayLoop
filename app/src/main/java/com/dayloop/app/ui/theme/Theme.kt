package com.dayloop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
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
)

private val DarkColorScheme = darkColorScheme(
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
)

@Composable
fun DayLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) CalmPressureDarkTokens else CalmPressureLightTokens

    CompositionLocalProvider(LocalDayLoopTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = DayLoopTypography,
            content = content,
        )
    }
}