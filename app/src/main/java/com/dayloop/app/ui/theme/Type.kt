package com.dayloop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import com.dayloop.app.R
import androidx.compose.ui.unit.sp

val DayLoopFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun mono(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = DayLoopFontFamily, fontSize = size.sp, lineHeight = line.sp,
    fontWeight = weight, letterSpacing = 0.sp,
)

internal val DayLoopTypography = Typography(
    displayLarge = mono(52, 60, FontWeight.Bold), displayMedium = mono(44, 52, FontWeight.Bold),
    displaySmall = mono(36, 44, FontWeight.Bold),
    headlineLarge = mono(28, 36, FontWeight.Bold), headlineMedium = mono(24, 32, FontWeight.Bold),
    headlineSmall = mono(22, 30, FontWeight.SemiBold),
    titleLarge = mono(20, 28, FontWeight.SemiBold), titleMedium = mono(16, 24, FontWeight.SemiBold),
    titleSmall = mono(14, 20, FontWeight.Medium),
    bodyLarge = mono(15, 23), bodyMedium = mono(14, 22), bodySmall = mono(12, 18),
    labelLarge = mono(13, 18, FontWeight.Medium), labelMedium = mono(12, 17, FontWeight.Medium),
    labelSmall = mono(11, 16),
)

internal fun scaledTypography(scale: Float, font: com.dayloop.app.settings.AppFont = com.dayloop.app.settings.AppFont.JETBRAINS_MONO): Typography {
    val family = appFontFamily(font)
    fun scaleWeight(weight: FontWeight?): FontWeight {
        val original = weight?.weight ?: 400
        return FontWeight((original * scale).toInt().coerceIn(1, 1000))
    }
    return Typography(
        displayLarge = DayLoopTypography.displayLarge.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.displayLarge.fontWeight)),
        displayMedium = DayLoopTypography.displayMedium.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.displayMedium.fontWeight)),
        displaySmall = DayLoopTypography.displaySmall.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.displaySmall.fontWeight)),
        headlineLarge = DayLoopTypography.headlineLarge.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.headlineLarge.fontWeight)),
        headlineMedium = DayLoopTypography.headlineMedium.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.headlineMedium.fontWeight)),
        headlineSmall = DayLoopTypography.headlineSmall.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.headlineSmall.fontWeight)),
        titleLarge = DayLoopTypography.titleLarge.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.titleLarge.fontWeight)),
        titleMedium = DayLoopTypography.titleMedium.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.titleMedium.fontWeight)),
        titleSmall = DayLoopTypography.titleSmall.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.titleSmall.fontWeight)),
        bodyLarge = DayLoopTypography.bodyLarge.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.bodyLarge.fontWeight)),
        bodyMedium = DayLoopTypography.bodyMedium.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.bodyMedium.fontWeight)),
        bodySmall = DayLoopTypography.bodySmall.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.bodySmall.fontWeight)),
        labelLarge = DayLoopTypography.labelLarge.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.labelLarge.fontWeight)),
        labelMedium = DayLoopTypography.labelMedium.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.labelMedium.fontWeight)),
        labelSmall = DayLoopTypography.labelSmall.copy(fontFamily = family, fontWeight = scaleWeight(DayLoopTypography.labelSmall.fontWeight)),
    )
}
