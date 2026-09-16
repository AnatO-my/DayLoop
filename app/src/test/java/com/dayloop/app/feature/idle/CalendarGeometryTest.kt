package com.dayloop.app.feature.idle

import org.junit.Assert.*
import org.junit.Test

class CalendarGeometryTest {
    @Test fun circlesFitEverySupportedRowCount() {
        for (weeks in 4..6) for (width in listOf(180f, 280f, 360f, 600f)) for (height in listOf(160f, 280f, 500f)) {
            val diameter = calendarDiameter(width, height, weeks, 3f, 32f)
            assertTrue(diameter > 0)
            assertTrue(diameter * 7 + 18 <= width + 0.01f)
            assertTrue(diameter * weeks + 3 * (weeks - 1) + 32 <= height + 0.01f)
        }
    }
    @Test fun constrainedHostNeverProducesNegativeCircle() {
        assertEquals(0f, calendarDiameter(0f, 0f, 6, 3f, 24f), 0f)
    }
}
