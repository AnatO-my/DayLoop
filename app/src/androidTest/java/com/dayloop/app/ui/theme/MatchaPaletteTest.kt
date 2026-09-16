package com.dayloop.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test

class MatchaPaletteTest {
    @Test fun originalAnchorExactlyPreservesMatchaRoles() {
        val mapper = MatchaPalette(Color(0xFFA8CF3D))
        assertEquals(LightColorScheme, mapper.scheme(LightColorScheme))
        assertEquals(DarkColorScheme, mapper.scheme(DarkColorScheme))
        assertEquals(CalmPressureLightAppField, mapper.tone(CalmPressureLightAppField))
    }

    @Test fun completeRolePairsRemainReadableForExtremeAndChromaticSeeds() {
        val seeds = listOf(0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFFF0000, 0xFF0000FF, 0xFF00FF00, 0xFFF4A7BF)
        for (seed in seeds) for (base in listOf(LightColorScheme, DarkColorScheme)) {
            val scheme = MatchaPalette(Color(seed)).scheme(base)
            val surfaces = listOf(scheme.background, scheme.surface, scheme.surfaceVariant,
                scheme.surfaceContainerLowest, scheme.surfaceContainerLow, scheme.surfaceContainer,
                scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.surfaceDim, scheme.surfaceBright)
            val pairs = listOf(scheme.onPrimary to scheme.primary, scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondary to scheme.secondary, scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiary to scheme.tertiary, scheme.onTertiaryContainer to scheme.tertiaryContainer,
                scheme.inverseOnSurface to scheme.inverseSurface, scheme.inversePrimary to scheme.inverseSurface,
                scheme.onBackground to scheme.background) +
                listOf(scheme.onPrimaryFixed to scheme.primaryFixed, scheme.onPrimaryFixed to scheme.primaryFixedDim,
                    scheme.onSecondaryFixed to scheme.secondaryFixed, scheme.onSecondaryFixed to scheme.secondaryFixedDim,
                    scheme.onTertiaryFixed to scheme.tertiaryFixed, scheme.onTertiaryFixed to scheme.tertiaryFixedDim) +
                surfaces.flatMap { listOf(scheme.onSurface to it, scheme.onSurfaceVariant to it, scheme.primary to it, scheme.secondary to it) }
            pairs.forEach { (foreground, background) ->
                assertTrue("Seed ${seed.toString(16)}: $foreground / $background",
                    ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb()) >= 4.5)
            }
            assertNotEquals(scheme.primary, scheme.primaryContainer)
            assertNotEquals(scheme.surfaceContainerLow, scheme.surfaceContainerHigh)
            assertEquals(base.error, scheme.error)
            assertEquals(base.errorContainer, scheme.errorContainer)
        }
    }
}
