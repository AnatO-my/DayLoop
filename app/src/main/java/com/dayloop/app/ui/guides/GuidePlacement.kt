package com.dayloop.app.ui.guides

internal fun guidePanelTop(height: Float, panelHeight: Float, targetTop: Float?, targetBottom: Float?, gap: Float): Float {
    val maximum = (height - panelHeight).coerceAtLeast(0f)
    if (targetTop == null || targetBottom == null) return maximum / 2f
    val below = targetBottom + gap
    val above = targetTop - gap - panelHeight
    return when {
        below <= maximum -> below
        above >= 0f -> above
        else -> maximum
    }.coerceIn(0f, maximum)
}
