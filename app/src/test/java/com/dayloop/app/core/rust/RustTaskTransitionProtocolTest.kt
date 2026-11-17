package com.dayloop.app.core.rust

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskStatus
import java.time.Instant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RustTaskTransitionProtocolTest {
    @Test
    fun completeRequestMatchesTheSharedV1Fixture() {
        val request = RustTaskTransitionProtocol.encode(
            source = Task(TaskId("task-1"), "Plan day"),
            command = RustTaskTransitionProtocol.Command.COMPLETE,
            at = Instant.ofEpochMilli(1_000),
        )

        assertArrayEquals(
            byteArrayOf(
                0x44, 0x4c, 0x54, 0x31, 0x01, 0x00, 0x06, 0x00, 0x08, 0x00,
                0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
                0xe8.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x74, 0x61, 0x73, 0x6b, 0x2d, 0x31, 0x50, 0x6c, 0x61, 0x6e, 0x20, 0x64, 0x61, 0x79,
            ),
            request,
        )
    }

    @Test
    fun completionResponseChangesOnlyRustOwnedFields() {
        val source = Task(
            id = TaskId("task-1"),
            title = "Plan day",
            priority = TaskPriority.HIGH,
        )

        val updated = RustTaskTransitionProtocol.decode(
            source,
            RustTaskTransitionProtocol.Command.COMPLETE,
            byteArrayOf(0x44, 0x4c, 0x52, 0x31, 0x01, 0xe8.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        )

        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals(Instant.ofEpochMilli(1_000), updated.completedAt)
        assertEquals(source.id, updated.id)
        assertEquals(source.title, updated.title)
        assertEquals(source.priority, updated.priority)
    }

    @Test
    fun rejectsResponseForADifferentCommand() {
        assertThrows(IllegalArgumentException::class.java) {
            RustTaskTransitionProtocol.decode(
                Task(TaskId("task-1"), "Plan day"),
                RustTaskTransitionProtocol.Command.REOPEN,
                byteArrayOf(0x44, 0x4c, 0x52, 0x31, 0x01, 0xe8.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            )
        }
    }
}
