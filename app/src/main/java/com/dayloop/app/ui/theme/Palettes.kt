package com.dayloop.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.dayloop.app.settings.AppPalette

internal fun AppPalette.seedColor(): Color = when (this) {
    AppPalette.MATCHA -> Color(0xFFA8CF3D)
    AppPalette.EMERALD -> Color(0xFF54DDA2)
    AppPalette.ROSE -> Color(0xFFF4A7BF)
}

internal fun paletteColorScheme(base: ColorScheme, palette: AppPalette, dark: Boolean): ColorScheme =
    MatchaPalette(palette.seedColor()).scheme(base)
