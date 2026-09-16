package com.dayloop.app.settings

import java.util.Locale

fun normalizeHexColor(value: String?): String? {
    val digits = value?.trim()?.removePrefix("#") ?: return null
    return if (digits.matches(Regex("[0-9a-fA-F]{6}"))) "#${digits.uppercase(Locale.ROOT)}" else null
}
