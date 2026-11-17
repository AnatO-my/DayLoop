package com.dayloop.app.rust

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.core.rust.RustTaskTransitionProtocol
import java.time.Instant
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Packaging smoke test only. The native ABI is exercised by Rust tests until a
 * small JNI adapter is added; Kotlin production code does not load this library.
 */
@RunWith(AndroidJUnit4::class)
class RustFfiLoadTest {
    @Test
    fun arm64InstrumentationPackageLoadsRustFfiLibrary() {
        assumeTrue(Build.SUPPORTED_ABIS.contains("arm64-v8a"))

        System.loadLibrary("dayloop_ffi_jni")
    }

    @Test
    fun completeFixtureMatchesTheRustTransitionProtocol() {
        assumeTrue(Build.SUPPORTED_ABIS.contains("arm64-v8a"))

        val response = RustFfiBridge.applyTaskTransition(completeFixture())

        assertArrayEquals(
            byteArrayOf(
                0, 0, 0, 0, // Native success status.
                0x44, 0x4c, 0x52, 0x31, 1,
                0xe8.toByte(), 3, 0, 0, 0, 0, 0, 0,
            ),
            response,
        )
    }

    @Test
    fun kotlinAndRustTransitionsAgreeForEveryV1Command() {
        assumeTrue(Build.SUPPORTED_ABIS.contains("arm64-v8a"))

        val completionTime = Instant.ofEpochMilli(1_000)
        assertNativeTransition(
            source = Task(TaskId("complete"), "Complete me"),
            command = RustTaskTransitionProtocol.Command.COMPLETE,
            at = completionTime,
            expected = Task(TaskId("complete"), "Complete me").markCompleted(completionTime),
        )
        assertNativeTransition(
            source = Task(
                id = TaskId("reopen"),
                title = "Reopen me",
                status = TaskStatus.COMPLETED,
                completedAt = completionTime,
            ),
            command = RustTaskTransitionProtocol.Command.REOPEN,
            at = completionTime.plusSeconds(1),
            expected = Task(
                id = TaskId("reopen"),
                title = "Reopen me",
                status = TaskStatus.COMPLETED,
                completedAt = completionTime,
            ).reopen(),
        )
        assertNativeTransition(
            source = Task(TaskId("skip"), "Skip me"),
            command = RustTaskTransitionProtocol.Command.SKIP,
            at = completionTime,
            expected = Task(TaskId("skip"), "Skip me").skip(),
        )
    }

    private fun assertNativeTransition(
        source: Task,
        command: RustTaskTransitionProtocol.Command,
        at: Instant,
        expected: Task,
    ) {
        val packedResponse = RustFfiBridge.applyTaskTransition(
            RustTaskTransitionProtocol.encode(source, command, at),
        )
        assertEquals(17, packedResponse.size)
        assertEquals(0, packedResponse.nativeStatus())
        assertEquals(expected, RustTaskTransitionProtocol.decode(source, command, packedResponse.copyOfRange(4, 17)))
    }

    private fun ByteArray.nativeStatus(): Int =
        (0 until 4).fold(0) { result, index -> result or ((this[index].toInt() and 0xff) shl (index * 8)) }

    private fun completeFixture(): ByteArray = byteArrayOf(
        0x44, 0x4c, 0x54, 0x31, 1, 0,
        6, 0, 8, 0,
        -1, -1, -1, -1, -1, -1, -1, -1,
        0xe8.toByte(), 3, 0, 0, 0, 0, 0, 0,
        0x74, 0x61, 0x73, 0x6b, 0x2d, 0x31,
        0x50, 0x6c, 0x61, 0x6e, 0x20, 0x64, 0x61, 0x79,
    )
}

private object RustFfiBridge {
    init {
        System.loadLibrary("dayloop_ffi_jni")
    }

    external fun applyTaskTransition(input: ByteArray): ByteArray
}
