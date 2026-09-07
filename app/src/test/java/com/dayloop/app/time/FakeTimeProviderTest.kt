package com.dayloop.app.time

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeTimeProviderTest {
    @Test
    fun nowReturnsControlledInstant() {
        val initial = Instant.parse("2026-08-29T08:00:00Z")
        val fakeTimeProvider = FakeTimeProvider(initialInstant = initial)

        assertEquals(initial, fakeTimeProvider.now())
    }

    @Test
    fun fakeTimeCanBeSetAndAdvanced() {
        val fakeTimeProvider = FakeTimeProvider(
            initialInstant = Instant.parse("2026-08-29T08:00:00Z"),
        )

        fakeTimeProvider.setNow(Instant.parse("2026-08-29T09:30:00Z"))
        fakeTimeProvider.advanceBy(Duration.ofMinutes(15))

        assertEquals(
            Instant.parse("2026-08-29T09:45:00Z"),
            fakeTimeProvider.now(),
        )
    }
}