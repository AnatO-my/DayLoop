package com.dayloop.app.feature.idle

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.dayloop.app.settings.*
import com.dayloop.app.ui.theme.DayLoopTheme
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarStyleTest {
    @get:Rule val compose = createComposeRule()

    private fun render(name: String, date: LocalDate, dark: Boolean = false, height: Int = 660, scale: Float = 1f,
        font: AppFont = AppFont.JETBRAINS_MONO) {
        compose.setContent {
            DayLoopTheme(AppSettings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, textScale = scale, font = font)) {
                Surface(Modifier.requiredSize(340.dp, height.dp)) {
                    CalendarFace(date, emptyList())
                }
            }
        }
        compose.waitForIdle()
        capture(name)
        val face = compose.onNodeWithTag("calendar-face").fetchSemanticsNode().boundsInRoot
        val weeks = (date.withDayOfMonth(1).dayOfWeek.value - 1 + date.lengthOfMonth() + 6) / 7
        val cells = (0 until weeks * 7).map {
            compose.onNodeWithTag("calendar-cell-$it", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        cells.forEach {
            assertEquals(it.width, it.height, 1f)
            assertTrue(it.width > 0 && it.left >= face.left - 1 && it.right <= face.right + 1)
            assertTrue(it.top >= face.top - 1 && it.bottom <= face.bottom + 1)
        }
        cells.forEachIndexed { i, a -> cells.drop(i + 1).forEach { b -> assertFalse(a.overlaps(b)) } }
        val month = compose.onNodeWithTag("calendar-month").fetchSemanticsNode().boundsInRoot
        val weekday = compose.onNodeWithTag("calendar-weekday").fetchSemanticsNode().boundsInRoot
        val year = compose.onNodeWithTag("calendar-year").fetchSemanticsNode().boundsInRoot
        assertTrue(month.right <= weekday.left)
        assertTrue(year.top >= month.bottom - 1)
    }

    private fun capture(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.getExternalFilesDir(null), "calendar-review").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            compose.onNodeWithTag("calendar-face").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun referenceMonthLight() = render("reference-light", LocalDate.of(2025, 2, 11), font = AppFont.ROBOTO)
    @Test fun sixWeekMonthDark() = render("six-week-dark", LocalDate.of(2026, 8, 11), dark = true)
    @Test fun enlargedText() = render("large-text", LocalDate.of(2026, 9, 9), scale = 1.3f)
    @Test fun landscape() = render("landscape", LocalDate.of(2026, 8, 11), height = 220)
}
