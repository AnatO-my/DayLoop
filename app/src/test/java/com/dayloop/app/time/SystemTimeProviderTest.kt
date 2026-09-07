package com.dayloop.app.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemTimeProviderTest {
    @Test
    fun nowUsesInjectedClock() {
        val fixedInstant = Instant.parse("2026-08-29T12:00:00Z")
        val provider = SystemTimeProvider(
            clock = Clock.fixed(fixedInstant, ZoneOffset.UTC),
        )

        assertEquals(fixedInstant, provider.now())
    }
}