package com.dayloop.app.core.outfit.sync

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ConflictResolutionTest {
    private val item = ClothingItem(ClothingItemId("shirt"), "Shirt", ClothingCategory.TOP, "#F5F5F5")
    private val details = GarmentAttributes(type = GarmentType.SHIRT, warmth = Warmth.LIGHT, occasions = setOf(Occasion.WORK, Occasion.CASUAL))

    private fun conflicted(): TrackedRecord {
        val base = WardrobeSyncMapper.toRecord(item, null)
        val local = WardrobeSyncMapper.toRecord(item.copy(name = "White work shirt"), details)
        val remote = WardrobeSyncMapper.toRecord(item.copy(name = "Oxford shirt", colorHex = "#1F2A44"), null)
        val merged = ThreeWayMerge.merge(base, local, remote) as MergeResult.Conflicted
        return TrackedRecord(merged.record, base = remote, baseRevision = 2, conflicts = merged.conflicts, autoMerged = merged.autoMerged)
    }

    @Test
    fun `item and details survive a round trip through sync fields`() {
        val record = WardrobeSyncMapper.toRecord(item.copy(isArchived = true), details)
        val (backItem, backDetails) = WardrobeSyncMapper.fromRecord(record)!!
        assertEquals(item.copy(isArchived = true), backItem)
        assertEquals(details, backDetails)
        assertNull(WardrobeSyncMapper.fromRecord(record.copy(deleted = true)))
        assertNull(WardrobeSyncMapper.fromRecord(SyncRecord("x", mapOf("name" to "No category"))))
        assertNotNull(WardrobeSyncMapper.fromRecord(SyncRecord("x", mapOf("name" to "Hat", "category" to "HEADWEAR", "type" to "FROM_A_NEWER_APP"))))
    }

    @Test
    fun `merge reports which side each clean change came from`() {
        val record = conflicted()
        assertEquals(listOf("name"), record.conflicts.map { it.field })
        assertEquals(MergeSide.REMOTE, record.autoMerged[WardrobeSyncMapper.COLOR])
        assertEquals(MergeSide.LOCAL, record.autoMerged[WardrobeSyncMapper.OCCASIONS])
        assertEquals("White work shirt", record.current.fields["name"])
    }

    @Test
    fun `the draft starts on this device's values and applies the user's choices`() {
        val draft = ConflictDraft(conflicted())
        assertEquals("White work shirt", draft.resolved().fields["name"])
        assertEquals("Oxford shirt", draft.choose("name", ConflictChoice.REMOTE).resolved().fields["name"])
        val typed = draft.typed("name", "  White oxford  ").resolved()
        assertEquals("White oxford", typed.fields["name"])
        assertEquals("#1F2A44", typed.fields["color"])
        assertEquals("CASUAL,WORK", typed.fields["occasions"])
    }

    @Test
    fun `an empty typed value blocks resolving and non-conflicting fields cannot be chosen`() {
        val draft = ConflictDraft(conflicted()).typed("name", "   ")
        assertNotNull(draft.blockingProblem)
        assertThrows(IllegalStateException::class.java) { draft.resolved() }
        assertThrows(IllegalArgumentException::class.java) { ConflictDraft(conflicted()).choose("color", ConflictChoice.REMOTE) }
    }

    @Test
    fun `delete versus edit can be settled either way`() {
        val base = WardrobeSyncMapper.toRecord(item, null)
        val merged = ThreeWayMerge.merge(base, base.copy(deleted = true), base.copy(fields = base.fields + ("name" to "Best shirt"))) as MergeResult.Conflicted
        val draft = ConflictDraft(TrackedRecord(merged.record, base, 1, merged.conflicts))
        assertEquals(true, draft.resolved().deleted)
        val kept = draft.choose(ThreeWayMerge.DELETED_FIELD, ConflictChoice.REMOTE).resolved()
        assertEquals(false, kept.deleted)
        assertEquals("Best shirt", kept.fields["name"])
        assertThrows(IllegalArgumentException::class.java) { draft.typed(ThreeWayMerge.DELETED_FIELD, "x") }
    }

    @Test
    fun `values are shown in words`() {
        assertEquals("Navy", WardrobeSyncMapper.displayValue("color", "#1F2A44"))
        assertEquals("Casual, Work / smart", WardrobeSyncMapper.displayValue("occasions", "CASUAL,WORK"))
        assertEquals("Not set", WardrobeSyncMapper.displayValue("material", null))
        assertEquals("Deleted", WardrobeSyncMapper.displayValue(ThreeWayMerge.DELETED_FIELD, "1"))
        assertEquals("Main colour", WardrobeSyncMapper.fieldLabel("color"))
    }

    @Test
    fun `resolving through the engine uploads the chosen version`() {
        val store = InMemorySyncStore()
        val record = conflicted()
        store.putRecord(record)
        var ops = 0
        val engine = WardrobeSyncEngine(store, object : SyncTransport {
            override suspend fun push(change: PendingChange) = PushResponse.Accepted(3)
            override suspend fun pull(afterRevision: Long) = emptyList<RemoteChange>()
        }, { java.time.Instant.EPOCH }, { "op-${++ops}" })
        engine.resolveConflict("shirt", ConflictDraft(record).choose("name", ConflictChoice.REMOTE).resolved())
        assertEquals("Oxford shirt", store.pending().single().record.fields["name"])
        assertEquals(emptyMap<String, MergeSide>(), store.record("shirt")!!.autoMerged)
    }
}
