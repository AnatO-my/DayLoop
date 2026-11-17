package com.dayloop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import com.dayloop.app.R
import androidx.compose.ui.unit.sp
import com.dayloop.app.settings.AppFont

val DayLoopFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, family: FontFamily = FontFamily.SansSerif) = TextStyle(
    fontFamily = family, fontSize = size.sp, lineHeight = line.sp,
    fontWeight = weight, letterSpacing = 0.sp,
)

internal val DayLoopTypography = Typography(
    displayMedium = type(44, 48, FontWeight.Bold),       // Display -> Bold
    headlineLarge = type(32, 38, FontWeight.Bold),       // Heading 1 -> Bold
    headlineMedium = type(24, 30, FontWeight.SemiBold),  // Heading 2 -> SemiBold
    headlineSmall = type(20, 26, FontWeight.SemiBold),   // Heading 3 -> SemiBold
    titleLarge = type(18, 24, FontWeight.Medium),        // Title -> Medium
    bodyLarge = type(16, 24, FontWeight.Normal),         // Body Large -> Regular
    bodyMedium = type(14, 20, FontWeight.Normal),         // Body -> Regular
    bodySmall = type(13, 18, FontWeight.Normal),         // Body Small -> Regular
    labelLarge = type(14, 20, FontWeight.Medium),        // Label -> Medium
    labelSmall = type(12, 16, FontWeight.Medium),        // Caption -> Medium
    // Styles that screens use but the scale did not define; without these they fell back
    // to Material's default sizes and letter spacing.
    displayLarge = type(57, 64, FontWeight.Bold),        // Hero numerals -> Bold
    displaySmall = type(36, 44, FontWeight.Bold),        // Display Small -> Bold
    titleMedium = type(16, 22, FontWeight.Medium),       // Card title -> Medium
    titleSmall = type(14, 20, FontWeight.Medium),        // Section title -> Medium
    labelMedium = type(13, 18, FontWeight.Medium),       // Label Medium -> Medium
)

internal fun scaledTypography(scale: Float, font: AppFont = AppFont.FUNNEL_DISPLAY): Typography {
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
