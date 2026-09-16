package com.dayloop.app.settings

import org.junit.Assert.*
import org.junit.Test

class CustomColorTest {
    @Test fun normalizesSixDigitHex() {
        assertEquals("#A8CF3D", normalizeHexColor("  a8cf3d "))
        assertEquals("#000000", normalizeHexColor("#000000"))
    }
    @Test fun rejectsInvalidOrTransparentSeeds() {
        listOf(null, "", "#abc", "#12345G", "#80A8CF3D", "red").forEach { assertNull(normalizeHexColor(it)) }
    }
}
