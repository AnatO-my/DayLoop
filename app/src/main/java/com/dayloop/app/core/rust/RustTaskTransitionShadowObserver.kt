package com.dayloop.app.core.rust

import com.dayloop.app.core.model.Task
import java.time.Instant

/**
 * Developer-only observation boundary for a future Rust transition bridge.
 *
 * Implementations must never return a replacement task or throw into the Room
 * transaction. Kotlin remains the only mutation authority.
 */
internal fun interface RustTaskTransitionShadowObserver {
    fun observe(source: Task, command: RustTaskTransitionProtocol.Command, at: Instant)
}

internal object NoOpRustTaskTransitionShadowObserver : RustTaskTransitionShadowObserver {
    override fun observe(source: Task, command: RustTaskTransitionProtocol.Command, at: Instant) = Unit
}

/** Bounded, process-local developer diagnostics. Nothing here is persisted or uploaded. */
internal class InMemoryRustTaskTransitionDiagnostics(
    private val adapter: RustTaskTransitionShadowAdapter,
    private val capacity: Int = 32,
) : RustTaskTransitionShadowObserver {
    init {
        require(capacity > 0) { "Rust shadow diagnostic capacity must be positive." }
    }

    private val records = ArrayDeque<RustShadowDiagnostic>()

    override fun observe(source: Task, command: RustTaskTransitionProtocol.Command, at: Instant) {
        val outcome = try {
            adapter.check(source, command, at).toOutcome()
        } catch (_: Exception) {
            RustShadowOutcome.BRIDGE_FAILURE
        } catch (_: LinkageError) {
            RustShadowOutcome.BRIDGE_FAILURE
        }
        synchronized(records) {
            if (records.size == capacity) records.removeFirst()
            records.addLast(RustShadowDiagnostic(command, outcome))
        }
    }

    fun snapshot(): List<RustShadowDiagnostic> = synchronized(records) { records.toList() }
}

internal data class RustShadowDiagnostic(
    val command: RustTaskTransitionProtocol.Command,
    val outcome: RustShadowOutcome,
)

internal enum class RustShadowOutcome { DISABLED, UNAVAILABLE, MATCH, DIVERGED, NATIVE_REJECTED, INVALID_RESPONSE, BRIDGE_FAILURE }

private fun ShadowCheck.toOutcome(): RustShadowOutcome = when (this) {
    ShadowCheck.Disabled -> RustShadowOutcome.DISABLED
    ShadowCheck.Unavailable -> RustShadowOutcome.UNAVAILABLE
    ShadowCheck.Match -> RustShadowOutcome.MATCH
    is ShadowCheck.Diverged -> RustShadowOutcome.DIVERGED
    is ShadowCheck.NativeRejected -> RustShadowOutcome.NATIVE_REJECTED
    ShadowCheck.InvalidResponse -> RustShadowOutcome.INVALID_RESPONSE
}
