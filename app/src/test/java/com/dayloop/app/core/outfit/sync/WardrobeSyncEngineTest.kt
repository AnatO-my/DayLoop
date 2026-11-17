package com.dayloop.app.core.outfit.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WardrobeSyncEngineTest {
    /** In-memory stand-in for a backend, with failure injection. Not a real service. */
    private class FakeServer : SyncTransport {
        val records = linkedMapOf<String, RemoteChange>()
        private val seen = mutableMapOf<String, PushResponse>()
        private var revision = 0L
        var failNext = 0
        var loseNextAck = false
        var applied = 0

        override suspend fun push(change: PendingChange): PushResponse {
            if (failNext > 0) { failNext--; throw SyncUnavailableException("offline") }
            val response = seen[change.opId] ?: run {
                val existing = records[change.record.id]
                if (existing != null && existing.revision != change.baseRevision) PushResponse.Stale(existing.record, existing.revision)
                else {
                    applied++
                    records[change.record.id] = RemoteChange(change.record, ++revision)
                    PushResponse.Accepted(revision).also { seen[change.opId] = it }
                }
            }
            if (loseNextAck) { loseNextAck = false; throw SyncUnavailableException("ack lost") }
            return response
        }

        override suspend fun pull(afterRevision: Long): List<RemoteChange> {
            if (failNext > 0) { failNext--; throw SyncUnavailableException("offline") }
            return records.values.filter { it.revision > afterRevision }
        }
    }

    private class Device(server: FakeServer, name: String) {
        var clock: Instant = Instant.parse("2026-11-10T08:00:00Z")
        val store = InMemorySyncStore()
        private var ops = 0
        val engine = WardrobeSyncEngine(store, server, { clock }, { "$name-${++ops}" }, BackoffPolicy(jitter = { 0.5 }))
        fun edit(id: String, vararg fields: Pair<String, String?>, deleted: Boolean = false) {
            val before = store.record(id)?.current?.fields.orEmpty()
            engine.recordLocalChange(SyncRecord(id, (before + fields).filterValues { it != null }, deleted))
        }
        fun sync() = runBlocking { engine.sync() }
        fun fields(id: String) = store.record(id)!!.current.fields
    }

    private val server = FakeServer()
    private val phone = Device(server, "phone")
    private val tablet = Device(server, "tablet")

    private fun shareShirt() {
        phone.edit("shirt", "name" to "Shirt", "color" to "#FFFFFF")
        phone.sync(); tablet.sync()
        assertEquals(phone.fields("shirt"), tablet.fields("shirt"))
    }

    @Test
    fun `edits to different fields on two devices both survive`() {
        shareShirt()
        phone.edit("shirt", "name" to "Oxford shirt")
        tablet.edit("shirt", "color" to "#F5F5F5")
        phone.sync()
        tablet.sync()   // stale push -> merge -> queued
        tablet.sync()   // merged version uploads
        phone.sync()
        val expected = mapOf("name" to "Oxford shirt", "color" to "#F5F5F5")
        assertEquals(expected, phone.fields("shirt"))
        assertEquals(expected, tablet.fields("shirt"))
        assertTrue(phone.store.pending().isEmpty() && tablet.store.pending().isEmpty())
    }

    @Test
    fun `the same field edited on two devices becomes a conflict and nothing is overwritten`() {
        shareShirt()
        phone.edit("shirt", "name" to "Work shirt")
        tablet.edit("shirt", "name" to "White shirt", "color" to "#EEEEEE")
        phone.sync()
        val report = tablet.sync()

        assertEquals(listOf("shirt"), report.conflictedIds)
        assertEquals("the tablet keeps its own value", "White shirt", tablet.fields("shirt")["name"])
        assertEquals("the server still holds the phone's value", "Work shirt", server.records["shirt"]!!.record.fields["name"])
        val conflict = tablet.store.record("shirt")!!.conflicts.single()
        assertEquals(FieldConflict("name", "Shirt", "White shirt", "Work shirt"), conflict)
        assertTrue("upload is held until the user decides", tablet.store.pending().isEmpty())

        tablet.sync(); tablet.sync()
        assertEquals("repeated syncs do not resolve it by themselves", "Work shirt", server.records["shirt"]!!.record.fields["name"])

        tablet.engine.resolveConflict("shirt", SyncRecord("shirt", mapOf("name" to "White work shirt", "color" to "#EEEEEE")))
        tablet.sync(); phone.sync()
        assertEquals("White work shirt", phone.fields("shirt")["name"])
        assertEquals("#EEEEEE", phone.fields("shirt")["color"])
        assertTrue(tablet.store.record("shirt")!!.conflicts.isEmpty())
    }

    @Test
    fun `delete on one device and edit on another is a conflict, not a silent loss`() {
        shareShirt()
        phone.edit("shirt", deleted = true)
        tablet.edit("shirt", "name" to "Favourite shirt")
        phone.sync()
        val report = tablet.sync()
        assertEquals(listOf("shirt"), report.conflictedIds)
        assertFalse(tablet.store.record("shirt")!!.current.deleted)
        assertEquals("Favourite shirt", tablet.fields("shirt")["name"])
    }

    @Test
    fun `a lost acknowledgement is retried without applying the change twice`() {
        phone.edit("hat", "name" to "Hat")
        server.loseNextAck = true
        val first = phone.sync()
        assertTrue(first.offline)
        assertEquals(1, phone.store.pending().size)
        phone.clock = phone.clock.plus(Duration.ofMinutes(5))
        val second = phone.sync()
        assertFalse(second.offline)
        assertEquals(1, second.pushed)
        assertEquals("server applied it once", 1, server.applied)
        assertTrue(phone.store.pending().isEmpty())
    }

    @Test
    fun `offline edits queue, coalesce and back off, then upload once`() {
        server.failNext = 3
        phone.edit("tee", "name" to "Tee")
        val a = phone.sync()
        assertTrue(a.offline)
        assertEquals(Instant.parse("2026-11-10T08:00:30Z"), a.retryAt)
        assertTrue("not retried before the backoff expires", phone.sync().let { it.pushed == 0 && server.failNext == 1 })

        phone.edit("tee", "name" to "Tee", "color" to "#000000")   // replaces the queued change
        phone.edit("tee", "name" to "Black tee", "color" to "#000000")
        assertEquals(1, phone.store.pending().size)
        server.failNext = 0
        val done = phone.sync()
        assertEquals(1, done.pushed)
        assertEquals(1, server.applied)
        assertEquals("Black tee", server.records["tee"]!!.record.fields["name"])
        assertNull(done.retryAt)
    }

    @Test
    fun `backoff grows and is capped`() {
        val policy = BackoffPolicy(jitter = { 0.5 })
        assertEquals(Duration.ofSeconds(30), policy.delay(1))
        assertEquals(Duration.ofSeconds(60), policy.delay(2))
        assertEquals(Duration.ofSeconds(240), policy.delay(4))
        assertEquals(Duration.ofHours(1), policy.delay(30))
        assertTrue(BackoffPolicy(jitter = { 0.0 }).delay(1) < BackoffPolicy(jitter = { 1.0 }).delay(1))
    }

    @Test
    fun `receiving the same remote change twice changes nothing`() {
        shareShirt()
        val before = tablet.store.record("shirt")
        tablet.store.cursor = 0   // force redelivery
        val report = tablet.sync()
        assertEquals(0, report.pulled)
        assertEquals(before, tablet.store.record("shirt"))
    }

    @Test
    fun `an edit made after an unacknowledged upload does not conflict with itself`() {
        phone.edit("cap", "name" to "Cap")
        server.loseNextAck = true
        assertTrue(phone.sync().offline)            // server applied "Cap"; the phone never heard back
        phone.edit("cap", "name" to "Blue cap")     // replaces the queued change with a new opId
        phone.clock = phone.clock.plus(Duration.ofMinutes(5))
        phone.sync(); phone.sync()
        assertEquals("Blue cap", server.records["cap"]!!.record.fields["name"])
        assertTrue(phone.store.record("cap")!!.conflicts.isEmpty())
        assertNotNull(phone.store.record("cap")!!.base)
        assertTrue(phone.store.pending().isEmpty())
    }

    @Test
    fun `merge keeps same-value edits and reports only true conflicts`() {
        val base = SyncRecord("x", mapOf("a" to "1", "b" to "1", "c" to "1"))
        val local = SyncRecord("x", mapOf("a" to "2", "b" to "9", "c" to "1"))
        val remote = SyncRecord("x", mapOf("a" to "2", "b" to "8"))
        val result = ThreeWayMerge.merge(base, local, remote) as MergeResult.Conflicted
        assertEquals(listOf("b"), result.conflicts.map { it.field })
        assertEquals(mapOf("a" to "2", "b" to "9"), result.record.fields)
    }
}
