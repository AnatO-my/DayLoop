package com.dayloop.app.time

import java.time.Duration
import java.time.Instant

class FakeTimeProvider(
    initialInstant: Instant,
) : TimeProvider {
    private var currentInstant: Instant = initialInstant

    override fun now(): Instant = currentInstant

    fun setNow(instant: Instant) {
        currentInstant = instant
    }

    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }
}