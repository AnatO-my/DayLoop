package com.dayloop.app.ui.theme

import com.dayloop.app.settings.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun systemModeFollowsDeviceAppearance() {
        assertFalse(ThemeMode.SYSTEM.usesDarkTheme(false))
        assertTrue(ThemeMode.SYSTEM.usesDarkTheme(true))
    }

    @Test fun explicitPreferenceOverridesDeviceAppearance() {
        for (systemDark in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.usesDarkTheme(systemDark))
            assertTrue(ThemeMode.DARK.usesDarkTheme(systemDark))
        }
    }
}
