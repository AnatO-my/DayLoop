package com.dayloop.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.*

/** Preserve the matcha role relationships around its reference accent, not a selected preset. */
internal class MatchaPalette(private val seed: Color) {
    private val reference = lab(Color(0xFFA8CF3D))
    private val target = lab(seed)
    private val referenceChroma = hypot(reference[1], reference[2])
    private val targetChroma = hypot(target[1], target[2])
    private val rotation = atan2(target[2], target[1]) - atan2(reference[2], reference[1])
    private val identity = seed.toArgb() == Color(0xFFA8CF3D).toArgb()

    fun tone(color: Color): Color {
        if (identity) return color
        val source = lab(color)
        // Extreme seeds still need useful light/dark surface separation.
        val anchor = target[0].coerceIn(30.0, 80.0)
        val lightness = if (source[0] >= reference[0])
            anchor + (100 - anchor) * (source[0] - reference[0]) / (100 - reference[0])
        else anchor * source[0] / reference[0]
        val chroma = if (targetChroma < 1.0) 0.0 else hypot(source[1], source[2]) * targetChroma / referenceChroma
        val hue = atan2(source[2], source[1]) + rotation
        return Color(ColorUtils.LABToColor(lightness.coerceIn(0.0, 100.0), chroma * cos(hue), chroma * sin(hue)))
    }

    fun scheme(base: ColorScheme): ColorScheme {
        if (identity) return base
        val background = tone(base.background)
        val surface = tone(base.surface)
        val variant = tone(base.surfaceVariant)
        val low = tone(base.surfaceContainerLow)
        val container = tone(base.surfaceContainer)
        val high = tone(base.surfaceContainerHigh)
        val highest = tone(base.surfaceContainerHighest)
        val lowest = tone(base.surfaceContainerLowest)
        val bright = tone(base.surfaceBright)
        val dim = tone(base.surfaceDim)
        val surfaces = listOf(background, surface, variant, low, container, high, highest, lowest, bright, dim)
        val primary = readable(if (base.primary == CalmPressureLightPrimary) seed else tone(base.primary), surfaces)
        val secondary = readable(tone(base.secondary), surfaces)
        val tertiary = readable(tone(base.tertiary), surfaces)
        val primaryContainer = tone(base.primaryContainer)
        val secondaryContainer = tone(base.secondaryContainer)
        val tertiaryContainer = tone(base.tertiaryContainer)
        val inverse = tone(base.inverseSurface)
        val primaryFixed = listOf(tone(base.primaryFixed), tone(base.primaryFixedDim))
        val secondaryFixed = listOf(tone(base.secondaryFixed), tone(base.secondaryFixedDim))
        val tertiaryFixed = listOf(tone(base.tertiaryFixed), tone(base.tertiaryFixedDim))
        return base.copy(
            background = background, onBackground = readable(tone(base.onBackground), listOf(background)),
            surface = surface, onSurface = readable(tone(base.onSurface), surfaces),
            surfaceVariant = variant, onSurfaceVariant = readable(tone(base.onSurfaceVariant), surfaces),
            primary = primary, onPrimary = readable(tone(base.onPrimary), listOf(primary)),
            primaryContainer = primaryContainer, onPrimaryContainer = readable(tone(base.onPrimaryContainer), listOf(primaryContainer)),
            secondary = secondary, onSecondary = readable(tone(base.onSecondary), listOf(secondary)),
            secondaryContainer = secondaryContainer, onSecondaryContainer = readable(tone(base.onSecondaryContainer), listOf(secondaryContainer)),
            tertiary = tertiary, onTertiary = readable(tone(base.onTertiary), listOf(tertiary)),
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = readable(tone(base.onTertiaryContainer), listOf(tertiaryContainer)),
            surfaceTint = seed, surfaceDim = dim, surfaceBright = bright,
            surfaceContainerLowest = lowest, surfaceContainerLow = low, surfaceContainer = container,
            surfaceContainerHigh = high, surfaceContainerHighest = highest,
            outline = readable(tone(base.outline), listOf(surface), 3.0), outlineVariant = tone(base.outlineVariant),
            inverseSurface = inverse, inverseOnSurface = readable(tone(base.inverseOnSurface), listOf(inverse)),
            inversePrimary = readable(tone(base.inversePrimary), listOf(inverse)),
            primaryFixed = primaryFixed[0], primaryFixedDim = primaryFixed[1],
            onPrimaryFixed = readable(tone(base.onPrimaryFixed), primaryFixed),
            onPrimaryFixedVariant = readable(tone(base.onPrimaryFixedVariant), primaryFixed),
            secondaryFixed = secondaryFixed[0], secondaryFixedDim = secondaryFixed[1],
            onSecondaryFixed = readable(tone(base.onSecondaryFixed), secondaryFixed),
            onSecondaryFixedVariant = readable(tone(base.onSecondaryFixedVariant), secondaryFixed),
            tertiaryFixed = tertiaryFixed[0], tertiaryFixedDim = tertiaryFixed[1],
            onTertiaryFixed = readable(tone(base.onTertiaryFixed), tertiaryFixed),
            onTertiaryFixedVariant = readable(tone(base.onTertiaryFixedVariant), tertiaryFixed),
            // Error and scrim roles remain semantic, not brand colors.
        )
    }

    private fun lab(color: Color) = DoubleArray(3).also { ColorUtils.colorToLAB(color.toArgb(), it) }
}

internal fun readable(candidate: Color, backgrounds: List<Color>, minimum: Double = 4.5): Color {
    fun score(color: Color) = backgrounds.minOf { ColorUtils.calculateContrast(color.toArgb(), it.toArgb()) }
    if (score(candidate) >= minimum) return candidate
    val target = listOf(Color.Black, Color.White).maxBy { score(it) }
    for (step in 1..100) {
        val adjusted = Color(ColorUtils.setAlphaComponent(
            ColorUtils.blendARGB(candidate.toArgb(), target.toArgb(), step / 100f), 255))
        if (score(adjusted) >= minimum) return adjusted
    }
    return target
}
