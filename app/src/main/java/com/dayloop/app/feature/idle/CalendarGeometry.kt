package com.dayloop.app.feature.idle

internal fun calendarDiameter(width: Float, height: Float, weeks: Int, gap: Float, labels: Float): Float {
    require(weeks in 4..6 && gap >= 0 && labels >= 0)
    return minOf((width - gap * 6) / 7, (height - labels - gap * (weeks - 1)) / weeks).coerceAtLeast(0f)
}
