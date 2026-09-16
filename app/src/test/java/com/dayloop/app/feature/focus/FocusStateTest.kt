package com.dayloop.app.feature.focus

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusStateTest {
    @Test fun elapsedDeadlineDoesNotProduceNegativeTime() {
        val end = Instant.parse("2026-09-08T12:00:00Z")
        val state = FocusState(endsAt = end)
        assertEquals(1L, state.secondsAt(end.minusMillis(500)))
        assertEquals(0L, state.secondsAt(end.plusSeconds(10)))
    }

    @Test fun streakAllowsTodayToRemainUnfinished() {
        val today = LocalDate.of(2026, 9, 8)
        val state = FocusState(minutes = mapOf(today.minusDays(1) to 25L, today.minusDays(2) to 50L))
        assertEquals(2, state.streakOn(today))
        assertEquals(0L, state.minutesOn(today))
        assertEquals(0, state.streakOn(today.plusDays(1)))
    }
}
