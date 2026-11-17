package com.dayloop.app.core.rust

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RustTaskTransitionShadowAdapterTest {
    private val source = Task(TaskId("task-1"), "Plan day")
    private val at = Instant.ofEpochMilli(1_000)

    @Test
    fun disabledAdapterDoesNotInvokeTheBridge() {
        val adapter = RustTaskTransitionShadowAdapter(enabled = false) {
            error("Disabled shadow mode must not call Rust.")
        }

        assertEquals(ShadowCheck.Disabled, adapter.check(source, RustTaskTransitionProtocol.Command.COMPLETE, at))
    }

    @Test
    fun matchingRustResponseIsReportedWithoutChangingTheSource() {
        val adapter = RustTaskTransitionShadowAdapter(enabled = true) {
            RustTransitionBridgeResponse(0, completeResponse())
        }

        assertEquals(ShadowCheck.Match, adapter.check(source, RustTaskTransitionProtocol.Command.COMPLETE, at))
        assertEquals(source, Task(TaskId("task-1"), "Plan day"))
    }

    @Test
    fun unexpectedNativeStatusIsReportedAsNoWriteFailure() {
        val adapter = RustTaskTransitionShadowAdapter(enabled = true) {
            RustTransitionBridgeResponse(2)
        }

        assertEquals(
            ShadowCheck.NativeRejected(2),
            adapter.check(source, RustTaskTransitionProtocol.Command.COMPLETE, at),
        )
    }

    @Test
    fun inMemoryDiagnosticsAreBoundedAndContainNoTaskPayload() {
        val diagnostics = InMemoryRustTaskTransitionDiagnostics(
            adapter = RustTaskTransitionShadowAdapter(enabled = false),
            capacity = 2,
        )

        diagnostics.observe(source, RustTaskTransitionProtocol.Command.COMPLETE, at)
        diagnostics.observe(source, RustTaskTransitionProtocol.Command.REOPEN, at)
        diagnostics.observe(source, RustTaskTransitionProtocol.Command.SKIP, at)

        assertEquals(
            listOf(
                RustShadowDiagnostic(RustTaskTransitionProtocol.Command.REOPEN, RustShadowOutcome.DISABLED),
                RustShadowDiagnostic(RustTaskTransitionProtocol.Command.SKIP, RustShadowOutcome.DISABLED),
            ),
            diagnostics.snapshot(),
        )
    }

    @Test
    fun bridgeFailureIsRecordedInsteadOfEscapingTheObserver() {
        val diagnostics = InMemoryRustTaskTransitionDiagnostics(
            adapter = RustTaskTransitionShadowAdapter(enabled = true) { error("bridge failed") },
        )

        diagnostics.observe(source, RustTaskTransitionProtocol.Command.COMPLETE, at)

        assertEquals(
            listOf(RustShadowDiagnostic(RustTaskTransitionProtocol.Command.COMPLETE, RustShadowOutcome.BRIDGE_FAILURE)),
            diagnostics.snapshot(),
        )
    }

    private fun completeResponse(): ByteArray = byteArrayOf(
        0x44, 0x4c, 0x52, 0x31, 0x01,
        0xe8.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    )
}
