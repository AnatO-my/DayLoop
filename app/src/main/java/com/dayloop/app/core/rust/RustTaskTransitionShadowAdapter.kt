package com.dayloop.app.core.rust

import com.dayloop.app.core.model.Task
import java.time.Instant

/**
 * Disabled-by-default observer for a future Rust transition call.
 *
 * A caller must invoke [check] only after it has reloaded the current Room task
 * inside its existing transaction. This adapter never writes Room state and
 * never schedules or cancels Android effects; its output is evidence that the
 * existing Kotlin transition and Rust calculation agree.
 */
internal class RustTaskTransitionShadowAdapter(
    private val enabled: Boolean = false,
    private val bridge: RustTransitionBridge? = null,
) {
    fun check(
        source: Task,
        command: RustTaskTransitionProtocol.Command,
        at: Instant,
    ): ShadowCheck = when {
        !enabled -> ShadowCheck.Disabled
        bridge == null -> ShadowCheck.Unavailable
        else -> {
            val request = RustTaskTransitionProtocol.encode(source, command, at)
            val response = bridge.apply(request)
            if (response.status != 0) return ShadowCheck.NativeRejected(response.status)
            val rustTask = try {
                RustTaskTransitionProtocol.decode(source, command, response.payload)
            } catch (_: IllegalArgumentException) {
                return ShadowCheck.InvalidResponse
            }
            val kotlinTask = when (command) {
                RustTaskTransitionProtocol.Command.COMPLETE -> source.markCompleted(at)
                RustTaskTransitionProtocol.Command.REOPEN -> source.reopen()
                RustTaskTransitionProtocol.Command.SKIP -> source.skip()
            }
            if (rustTask == kotlinTask) ShadowCheck.Match else ShadowCheck.Diverged(kotlinTask, rustTask)
        }
    }
}

internal fun interface RustTransitionBridge {
    fun apply(request: ByteArray): RustTransitionBridgeResponse
}

internal data class RustTransitionBridgeResponse(
    val status: Int,
    val payload: ByteArray = ByteArray(0),
)

internal sealed interface ShadowCheck {
    data object Disabled : ShadowCheck
    data object Unavailable : ShadowCheck
    data class NativeRejected(val status: Int) : ShadowCheck
    data object InvalidResponse : ShadowCheck
    data object Match : ShadowCheck
    data class Diverged(val kotlinTask: Task, val rustTask: Task) : ShadowCheck
}
