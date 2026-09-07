package com.dayloop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val DayLoopTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeight = 40.sp,
    ),
    bodyLarge = Typography().bodyLarge.copy(
        lineHeight = 26.sp,
    ),
)