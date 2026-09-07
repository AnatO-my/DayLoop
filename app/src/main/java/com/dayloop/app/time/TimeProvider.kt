package com.dayloop.app.time

import java.time.Clock
import java.time.Instant

interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider(
    private val clock: Clock = Clock.systemUTC(),
) : TimeProvider {
    override fun now(): Instant = clock.instant()
}