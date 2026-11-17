package com.dayloop.app.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPinningManagerTest {
    @Test fun pinningRequiresAndroidOAndLauncherSupport() {
        assertFalse(WidgetPinningManager.isPinningSupported(25, launcherSupportsPinning = true))
        assertFalse(WidgetPinningManager.isPinningSupported(26, launcherSupportsPinning = false))
        assertTrue(WidgetPinningManager.isPinningSupported(26, launcherSupportsPinning = true))
    }
}
