package com.dayloop.app.core.alarms

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// One process owns receivers, recovery and UI mutations; Room transactions provide durable commits.
object AlarmMutationLock {
    private val mutex = Mutex()
    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }
}
