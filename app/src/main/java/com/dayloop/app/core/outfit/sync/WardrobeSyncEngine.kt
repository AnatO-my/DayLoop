package com.dayloop.app.core.outfit.sync

import java.time.Duration
import java.time.Instant
import kotlin.math.min

/** A record plus what this device knows about the server's copy. */
data class TrackedRecord(
    val current: SyncRecord,
    /** The last version both this device and the server agreed on; null if never synced. */
    val base: SyncRecord?,
    val baseRevision: Long,
    val conflicts: List<FieldConflict> = emptyList(),
    /**
     * Versions this device tried to upload without getting an answer (newest last, at most
     * five). If the server later shows one of these, it is our own change arriving late,
     * not someone else's edit.
     */
    val unconfirmedUploads: List<SyncRecord> = emptyList(),
    /** While a conflict is open: the other fields that merged cleanly, and from which side. */
    val autoMerged: Map<String, MergeSide> = emptyMap(),
)

/** One queued upload. [opId] never changes across retries, so the server can drop duplicates. */
data class PendingChange(
    val opId: String,
    val record: SyncRecord,
    val baseRevision: Long,
    val attempts: Int = 0,
    val notBefore: Instant = Instant.EPOCH,
)

sealed interface PushResponse {
    data class Accepted(val revision: Long) : PushResponse
    /** The server copy moved on since [PendingChange.baseRevision]; merge and try again. */
    data class Stale(val remote: SyncRecord, val revision: Long) : PushResponse
}

data class RemoteChange(val record: SyncRecord, val revision: Long)

/** Thrown by a transport for anything worth retrying: no network, timeout, 5xx. */
class SyncUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The only thing a backend must provide. No implementation ships with DayLoop: the app has
 * no INTERNET permission and no account system (see ADR 0006).
 */
interface SyncTransport {
    suspend fun push(change: PendingChange): PushResponse
    suspend fun pull(afterRevision: Long): List<RemoteChange>
}

/** Durable state. In the app this would be Room tables written in one transaction per call. */
interface SyncStore {
    fun record(id: String): TrackedRecord?
    fun records(): List<TrackedRecord>
    fun putRecord(record: TrackedRecord)
    fun pending(): List<PendingChange>
    fun putPending(change: PendingChange)
    fun removePending(recordId: String)
    var cursor: Long
}

class InMemorySyncStore : SyncStore {
    private val tracked = linkedMapOf<String, TrackedRecord>()
    private val queue = linkedMapOf<String, PendingChange>()
    override var cursor: Long = 0
    override fun record(id: String) = tracked[id]
    override fun records() = tracked.values.toList()
    override fun putRecord(record: TrackedRecord) { tracked[record.current.id] = record }
    override fun pending() = queue.values.toList()
    override fun putPending(change: PendingChange) { queue[change.record.id] = change }
    override fun removePending(recordId: String) { queue.remove(recordId) }
}

/** Exponential backoff with jitter: 30 s, 1 min, 2 min ... capped at 1 h, +/-20%. */
class BackoffPolicy(
    private val base: Duration = Duration.ofSeconds(30),
    private val cap: Duration = Duration.ofHours(1),
    private val jitter: () -> Double = { Math.random() },
) {
    fun delay(attempts: Int): Duration {
        val exact = min(cap.toMillis().toDouble(), base.toMillis() * Math.pow(2.0, (attempts - 1).coerceIn(0, 20).toDouble()))
        return Duration.ofMillis((exact * (0.8 + 0.4 * jitter().coerceIn(0.0, 1.0))).toLong())
    }
}

data class SyncReport(
    val pushed: Int,
    val pulled: Int,
    /** Ids waiting for the user to choose between two versions. */
    val conflictedIds: List<String>,
    /** True when the server could not be reached; local work is untouched and queued. */
    val offline: Boolean,
    val retryAt: Instant?,
)

/**
 * Offline-first sync for wardrobe records.
 *
 * - Local edits always succeed immediately and are queued (one queued change per record).
 * - Uploads are idempotent: a retry reuses the same opId.
 * - A stale upload or an incoming change is three-way merged; real conflicts are parked for
 *   the user and the local value is kept meanwhile. Nothing is silently overwritten.
 * - Failures back off exponentially and never drop the queue.
 */
class WardrobeSyncEngine(
    private val store: SyncStore,
    private val transport: SyncTransport,
    private val now: () -> Instant,
    private val newOpId: () -> String,
    private val backoff: BackoffPolicy = BackoffPolicy(),
) {
    /** Call for every local create, edit or delete (pass `deleted = true`). */
    fun recordLocalChange(record: SyncRecord) {
        val existing = store.record(record.id)
        if (existing?.current == record) return
        val tracked = existing?.copy(current = record) ?: TrackedRecord(record, base = null, baseRevision = 0)
        store.putRecord(tracked)
        if (tracked.conflicts.isEmpty()) enqueue(tracked)
    }

    /**
     * Settles a conflict with the user's decision. [resolved] must hold the final value for
     * every conflicting field; the result is then uploaded like any other edit.
     */
    fun resolveConflict(id: String, resolved: SyncRecord) {
        val tracked = requireNotNull(store.record(id)) { "Unknown record $id" }
        require(tracked.conflicts.isNotEmpty()) { "Record $id has no conflict to resolve." }
        val settled = tracked.copy(current = resolved, conflicts = emptyList(), autoMerged = emptyMap())
        store.putRecord(settled)
        if (settled.current != settled.base) enqueue(settled) else store.removePending(id)
    }

    suspend fun sync(): SyncReport {
        var pushed = 0
        var pulled = 0
        val current = now()
        try {
            for (change in store.pending()) {
                if (change.notBefore.isAfter(current)) continue
                store.record(change.record.id)?.let { store.putRecord(it.copy(unconfirmedUploads = (it.unconfirmedUploads - change.record + change.record).takeLast(5))) }
                when (val response = try {
                    transport.push(change)
                } catch (unavailable: SyncUnavailableException) {
                    val attempts = change.attempts + 1
                    // The queue may have been replaced by a newer edit while the call was in flight.
                    store.pending().firstOrNull { it.opId == change.opId }
                        ?.let { store.putPending(it.copy(attempts = attempts, notBefore = current.plus(backoff.delay(attempts)))) }
                    throw unavailable
                }) {
                    is PushResponse.Accepted -> { acknowledge(change, response.revision); pushed++ }
                    is PushResponse.Stale -> integrate(response.remote, response.revision)
                }
            }
            for (remote in transport.pull(store.cursor).sortedBy { it.revision }) {
                if (integrate(remote.record, remote.revision)) pulled++
                if (remote.revision > store.cursor) store.cursor = remote.revision
            }
        } catch (unavailable: SyncUnavailableException) {
            return report(pushed, pulled, offline = true)
        }
        return report(pushed, pulled, offline = false)
    }

    private fun report(pushed: Int, pulled: Int, offline: Boolean) = SyncReport(
        pushed, pulled,
        conflictedIds = store.records().filter { it.conflicts.isNotEmpty() }.map { it.current.id },
        offline = offline,
        retryAt = store.pending().map { it.notBefore }.filter { it.isAfter(now()) }.minOrNull(),
    )

    private fun enqueue(tracked: TrackedRecord) {
        if (tracked.current == tracked.base) { store.removePending(tracked.current.id); return }
        store.putPending(PendingChange(newOpId(), tracked.current, tracked.baseRevision))
    }

    private fun acknowledge(change: PendingChange, revision: Long) {
        val tracked = store.record(change.record.id) ?: return
        val agreed = tracked.copy(base = change.record, baseRevision = revision, unconfirmedUploads = emptyList())
        store.putRecord(agreed)
        if (agreed.current == change.record) store.removePending(change.record.id)
        else enqueue(agreed) // edited again while the upload was in flight
    }

    /** Folds a server version into local state. Returns true if the visible record changed. */
    private fun integrate(remote: SyncRecord, revision: Long): Boolean {
        val tracked = store.record(remote.id)
        if (tracked == null) {
            store.putRecord(TrackedRecord(remote, base = remote, baseRevision = revision))
            return true
        }
        if (revision <= tracked.baseRevision) return false // duplicate or out-of-date delivery
        if (remote in tracked.unconfirmedUploads) {
            // Our own earlier upload was applied but its acknowledgement never reached us.
            val agreed = tracked.copy(base = remote, baseRevision = revision, unconfirmedUploads = emptyList())
            store.putRecord(agreed)
            if (agreed.conflicts.isEmpty()) enqueue(agreed)
            return false
        }
        if (tracked.current == tracked.base) {
            store.putRecord(TrackedRecord(remote, base = remote, baseRevision = revision))
            store.removePending(remote.id)
            return tracked.current != remote
        }
        return when (val merged = ThreeWayMerge.merge(tracked.base, tracked.current, remote)) {
            is MergeResult.Merged -> {
                val next = TrackedRecord(merged.record, base = remote, baseRevision = revision)
                store.putRecord(next)
                enqueue(next)
                merged.record != tracked.current
            }
            is MergeResult.Conflicted -> {
                // Hold the upload until the user decides; keep their value on screen.
                store.putRecord(
                    TrackedRecord(merged.record, base = remote, baseRevision = revision, conflicts = merged.conflicts, autoMerged = merged.autoMerged)
                )
                store.removePending(remote.id)
                merged.record != tracked.current
            }
        }
    }
}
