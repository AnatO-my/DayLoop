package com.dayloop.app.ui.theme

import androidx.compose.ui.text.font.*
import com.dayloop.app.R
import com.dayloop.app.settings.AppFont

val FunnelDisplayFontFamily = FontFamily(Font(R.font.funnel_display_variable, FontWeight.Normal))
val GeistMonoFontFamily = FontFamily(Font(R.font.geist_mono_variable, FontWeight.Normal))
val StackSansTextFontFamily = FontFamily(Font(R.font.stack_sans_text_variable, FontWeight.Normal))

fun appFontFamily(font: AppFont): FontFamily = when (font) {
    AppFont.FUNNEL_DISPLAY -> FunnelDisplayFontFamily
    AppFont.JETBRAINS_MONO -> DayLoopFontFamily
    AppFont.GEIST_MONO -> GeistMonoFontFamily
    AppFont.STACK_SANS_TEXT -> StackSansTextFontFamily
    AppFont.SYSTEM_MONO -> FontFamily.Monospace
    AppFont.ROBOTO -> FontFamily(Font(R.font.roboto_regular), Font(R.font.roboto_regular, FontWeight.Bold))
    AppFont.OPEN_SANS -> FontFamily(Font(R.font.opensans_regular), Font(R.font.opensans_regular, FontWeight.Bold))
    AppFont.LATO -> FontFamily(Font(R.font.lato_regular), Font(R.font.lato_bold, FontWeight.Bold))
    AppFont.MONTSERRAT -> FontFamily(Font(R.font.montserrat_regular), Font(R.font.montserrat_regular, FontWeight.Bold))
    AppFont.POPPINS -> FontFamily(Font(R.font.poppins_regular), Font(R.font.poppins_bold, FontWeight.Bold))
}
