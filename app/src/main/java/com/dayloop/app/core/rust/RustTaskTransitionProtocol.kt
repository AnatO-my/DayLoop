package com.dayloop.app.core.rust

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskStatus
import java.time.Instant

/**
 * Kotlin representation of the bounded `DLT1`/`DLR1` task-transition fixture.
 *
 * This type owns wire validation only. Callers still reload the Room task inside
 * their transaction, compare its current state with [source], and sequence
 * Android alarm/notification effects after saving the returned copy.
 */
internal object RustTaskTransitionProtocol {
    private val requestMagic = byteArrayOf('D'.code.toByte(), 'L'.code.toByte(), 'T'.code.toByte(), '1'.code.toByte())
    private val responseMagic = byteArrayOf('D'.code.toByte(), 'L'.code.toByte(), 'R'.code.toByte(), '1'.code.toByte())
    private const val headerBytes = 26
    private const val responseBytes = 13
    private const val maxRequestBytes = 6_144
    private const val maxIdBytes = 1_024
    private const val maxTitleBytes = 4_096

    fun encode(source: Task, command: Command, at: Instant): ByteArray {
        val id = source.id.value.encodeToByteArray()
        val title = source.title.encodeToByteArray()
        require(id.size in 1..maxIdBytes) { "Rust transition task ID must be 1..$maxIdBytes UTF-8 bytes." }
        require(title.size in 1..maxTitleBytes) { "Rust transition title must be 1..$maxTitleBytes UTF-8 bytes." }
        val total = headerBytes + id.size + title.size
        require(total <= maxRequestBytes) { "Rust transition request exceeds $maxRequestBytes bytes." }

        return ByteArray(total).also { bytes ->
            requestMagic.copyInto(bytes)
            bytes[4] = command.wireValue
            bytes[5] = source.status.wireValue()
            bytes.putUnsignedShortLittleEndian(6, id.size)
            bytes.putUnsignedShortLittleEndian(8, title.size)
            bytes.putLongLittleEndian(10, source.completedAt?.toEpochMilli() ?: -1)
            bytes.putLongLittleEndian(18, at.toEpochMilli())
            id.copyInto(bytes, destinationOffset = headerBytes)
            title.copyInto(bytes, destinationOffset = headerBytes + id.size)
        }
    }

    fun decode(source: Task, command: Command, response: ByteArray): Task {
        require(response.size == responseBytes) { "Rust transition response must be exactly $responseBytes bytes." }
        require(response.copyOfRange(0, 4).contentEquals(responseMagic)) { "Rust transition response magic is invalid." }
        val status = response[4].toTaskStatus()
        require(status == command.resultingStatus) { "Rust transition response status does not match the requested command." }
        val completedAtMillis = response.readLongLittleEndian(5)
        val completedAt = completedAtMillis.takeUnless { it == -1L }?.let(Instant::ofEpochMilli)
        require((status == TaskStatus.COMPLETED) == (completedAt != null)) {
            "Rust transition response has an invalid completion timestamp for $status."
        }
        return source.copy(status = status, completedAt = completedAt)
    }

    enum class Command(val wireValue: Byte, val resultingStatus: TaskStatus) {
        COMPLETE(1, TaskStatus.COMPLETED),
        REOPEN(2, TaskStatus.ACTIVE),
        SKIP(3, TaskStatus.SKIPPED),
    }

    private fun TaskStatus.wireValue(): Byte = when (this) {
        TaskStatus.ACTIVE -> 0
        TaskStatus.COMPLETED -> 1
        TaskStatus.SKIPPED -> 2
        TaskStatus.SOMEDAY -> 3
    }

    private fun Byte.toTaskStatus(): TaskStatus = when (toInt()) {
        0 -> TaskStatus.ACTIVE
        1 -> TaskStatus.COMPLETED
        2 -> TaskStatus.SKIPPED
        3 -> TaskStatus.SOMEDAY
        else -> throw IllegalArgumentException("Rust transition response status is invalid.")
    }

    private fun ByteArray.putUnsignedShortLittleEndian(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value ushr 8).toByte()
    }

    private fun ByteArray.putLongLittleEndian(offset: Int, value: Long) {
        repeat(Long.SIZE_BYTES) { index ->
            this[offset + index] = (value ushr (index * Byte.SIZE_BITS)).toByte()
        }
    }

    private fun ByteArray.readLongLittleEndian(offset: Int): Long =
        (0 until Long.SIZE_BYTES).fold(0L) { result, index ->
            result or ((this[offset + index].toLong() and 0xff) shl (index * Byte.SIZE_BITS))
        }
}
