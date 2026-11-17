package com.dayloop.app.core.outfit.sync

/**
 * One syncable wardrobe record as flat text fields (a clothing item and its attributes).
 * Photos are not synced by this core; only their ids could travel as a field.
 */
data class SyncRecord(
    val id: String,
    val fields: Map<String, String?>,
    val deleted: Boolean = false,
)

/** Two devices changed the same field to different values since they last agreed. */
data class FieldConflict(
    val field: String,
    val base: String?,
    val local: String?,
    val remote: String?,
)

sealed interface MergeResult {
    val record: SyncRecord

    /** Every difference could be combined without losing anyone's edit. */
    data class Merged(override val record: SyncRecord) : MergeResult

    /**
     * [record] holds every non-conflicting change from both sides and KEEPS THE LOCAL VALUE
     * for each conflicting field. Nothing is overwritten until the user chooses.
     */
    data class Conflicted(
        override val record: SyncRecord,
        val conflicts: List<FieldConflict>,
        /** Fields that merged without a conflict, and whose change was taken. */
        val autoMerged: Map<String, MergeSide> = emptyMap(),
    ) : MergeResult
}

enum class MergeSide { LOCAL, REMOTE }

/**
 * Field-level three-way merge. For each field: if only one side changed it since [base],
 * that change wins; if both made the same change, fine; if both changed it differently,
 * it is a conflict for the user. Deletion is treated as one more field, so "deleted here,
 * edited there" is a conflict rather than a silent loss.
 */
object ThreeWayMerge {
    const val DELETED_FIELD = "\u0000deleted"

    fun merge(base: SyncRecord?, local: SyncRecord, remote: SyncRecord): MergeResult {
        require(local.id == remote.id && (base == null || base.id == local.id)) { "Records must share an id." }
        fun flat(record: SyncRecord?): Map<String, String?> =
            record?.let { it.fields + (DELETED_FIELD to if (it.deleted) "1" else null) }.orEmpty()
        val b = flat(base); val l = flat(local); val r = flat(remote)
        val merged = mutableMapOf<String, String?>()
        val conflicts = mutableListOf<FieldConflict>()
        val autoMerged = linkedMapOf<String, MergeSide>()
        for (field in (b.keys + l.keys + r.keys).sorted()) {
            val bv = b[field]; val lv = l[field]; val rv = r[field]
            merged[field] = when {
                lv == rv -> lv
                lv == bv -> { autoMerged[field] = MergeSide.REMOTE; rv }
                rv == bv -> { autoMerged[field] = MergeSide.LOCAL; lv }
                else -> { conflicts += FieldConflict(field, bv, lv, rv); lv }
            }
        }
        // One side deleted the record while the other edited it: field-wise that merges cleanly
        // into "deleted", which would discard the edit. Make the user decide instead.
        if (local.deleted != remote.deleted && conflicts.none { it.field == DELETED_FIELD }) {
            val editor = if (local.deleted) r else l
            if ((editor - DELETED_FIELD) != (b - DELETED_FIELD)) {
                conflicts += FieldConflict(DELETED_FIELD, b[DELETED_FIELD], l[DELETED_FIELD], r[DELETED_FIELD])
                merged[DELETED_FIELD] = l[DELETED_FIELD]
            }
        }
        val deleted = merged.remove(DELETED_FIELD) == "1"
        val record = SyncRecord(local.id, merged.filterValues { it != null }, deleted)
        return if (conflicts.isEmpty()) MergeResult.Merged(record)
        else MergeResult.Conflicted(record, conflicts, autoMerged - DELETED_FIELD)
    }
}
