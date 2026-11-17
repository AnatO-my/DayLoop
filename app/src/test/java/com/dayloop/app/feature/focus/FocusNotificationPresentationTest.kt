package com.dayloop.app.feature.focus

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class FocusNotificationPresentationTest {
    private val now = Instant.parse("2026-09-19T12:00:00Z")

    @Test fun presentationUsesActiveModeNotViewedTab() {
        val state = FocusState(mode = FocusMode.LONG_BREAK, activeMode = FocusMode.WORK,
            endsAt = now.plusSeconds(61))
        val presentation = FocusNotificationPresentation.from(state, now)
        assertEquals(FocusMode.WORK, presentation.mode)
        assertEquals("01:01", presentation.remaining)
        assertFalse(presentation.paused)
        assertEquals(state.notificationToken(), state.copy(mode = FocusMode.SHORT_BREAK).notificationToken())
    }

    @Test fun pausedTimeStaysFixedAndHasExplicitLabel() {
        val state = FocusState(activeMode = FocusMode.SHORT_BREAK, remaining = 125)
        val presentation = FocusNotificationPresentation.from(state, now.plusSeconds(600))
        assertEquals("02:05", presentation.remaining)
        assertEquals("Short break paused", presentation.label)
        assertTrue(presentation.paused)
    }

    @Test fun expiredCountdownIsClampedAndLongTimersDoNotWrap() {
        assertEquals("00:00", FocusNotificationPresentation.from(
            FocusState(activeMode = FocusMode.WORK, endsAt = now.minusSeconds(2)), now).remaining)
        assertEquals("180:00", FocusNotificationPresentation.from(
            FocusState(activeMode = FocusMode.WORK, remaining = 10800), now).remaining)
    }
}
