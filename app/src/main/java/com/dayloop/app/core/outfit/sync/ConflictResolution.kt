package com.dayloop.app.core.outfit.sync

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingFit
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.HexColor
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import com.dayloop.app.core.model.label
import com.dayloop.app.core.outfit.recognition.ColorFamily

enum class ConflictChoice { LOCAL, REMOTE, CUSTOM }

/**
 * The user's pending decisions for one conflicted record. Starts with every field on the
 * local value, which is also what stays on screen until they decide.
 */
data class ConflictDraft(
    val record: TrackedRecord,
    val choices: Map<String, ConflictChoice> = record.conflicts.associate { it.field to ConflictChoice.LOCAL },
    val custom: Map<String, String> = emptyMap(),
) {
    init {
        require(record.conflicts.isNotEmpty()) { "Record has no conflict." }
    }

    fun choose(field: String, choice: ConflictChoice): ConflictDraft {
        require(record.conflicts.any { it.field == field }) { "$field is not in conflict." }
        return copy(choices = choices + (field to choice))
    }

    /** Typing a value selects it. Deletion cannot take a custom value. */
    fun typed(field: String, value: String): ConflictDraft {
        require(field != ThreeWayMerge.DELETED_FIELD) { "Deletion is keep or delete only." }
        return choose(field, ConflictChoice.CUSTOM).copy(custom = custom + (field to value))
    }

    fun valueOf(conflict: FieldConflict): String? = when (choices[conflict.field] ?: ConflictChoice.LOCAL) {
        ConflictChoice.LOCAL -> conflict.local
        ConflictChoice.REMOTE -> conflict.remote
        ConflictChoice.CUSTOM -> custom[conflict.field]?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Null when everything is decided, otherwise what is still missing. */
    val blockingProblem: String?
        get() = record.conflicts.firstOrNull { choices[it.field] == ConflictChoice.CUSTOM && valueOf(it) == null }
            ?.let { "Type a value for ${WardrobeSyncMapper.fieldLabel(it.field).lowercase()} or pick one of the versions." }

    /** The record to hand to [WardrobeSyncEngine.resolveConflict]. */
    fun resolved(): SyncRecord {
        check(blockingProblem == null) { blockingProblem.orEmpty() }
        val fields = record.current.fields.toMutableMap()
        var deleted = record.current.deleted
        for (conflict in record.conflicts) {
            val value = valueOf(conflict)
            if (conflict.field == ThreeWayMerge.DELETED_FIELD) deleted = value == "1"
            else if (value == null) fields.remove(conflict.field) else fields[conflict.field] = value
        }
        return SyncRecord(record.current.id, fields.filterValues { it != null }, deleted)
    }
}

/** Flattens a wardrobe item to sync fields and back, and names fields for people. */
object WardrobeSyncMapper {
    const val NAME = "name"
    const val CATEGORY = "category"
    const val COLOR = "color"
    const val ARCHIVED = "archived"
    const val TYPE = "type"
    const val SECONDARY_COLOR = "secondary_color"
    const val PATTERN = "pattern"
    const val MATERIAL = "material"
    const val FIT = "fit"
    const val WARMTH = "warmth"
    const val OCCASIONS = "occasions"

    fun toRecord(item: ClothingItem, attributes: GarmentAttributes?): SyncRecord = SyncRecord(
        item.id.value,
        mapOf(
            NAME to item.name,
            CATEGORY to item.category.name,
            COLOR to item.colorHex,
            ARCHIVED to if (item.isArchived) "1" else null,
            TYPE to attributes?.type?.name,
            SECONDARY_COLOR to attributes?.secondaryColorHex,
            PATTERN to attributes?.pattern?.name,
            MATERIAL to attributes?.material?.name,
            FIT to attributes?.fit?.name,
            WARMTH to attributes?.warmth?.name,
            OCCASIONS to attributes?.occasions?.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }?.joinToString(",") { it.name },
        ).filterValues { it != null },
    )

    /** Null when the record is deleted or lacks a usable name or category. Unknown enum names read as "not set". */
    fun fromRecord(record: SyncRecord): Pair<ClothingItem, GarmentAttributes>? {
        if (record.deleted) return null
        val f = record.fields
        val name = f[NAME]?.takeIf { it.isNotBlank() } ?: return null
        val category = enumOrNull<ClothingCategory>(f[CATEGORY]) ?: return null
        val item = ClothingItem(ClothingItemId(record.id), name, category, f[COLOR]?.takeIf(HexColor::isValid), f[ARCHIVED] == "1")
        val attributes = GarmentAttributes(
            type = enumOrNull<GarmentType>(f[TYPE])?.takeIf { it.category == category },
            secondaryColorHex = f[SECONDARY_COLOR]?.takeIf(HexColor::isValid),
            pattern = enumOrNull<ClothingPattern>(f[PATTERN]),
            material = enumOrNull<ClothingMaterial>(f[MATERIAL]),
            fit = enumOrNull<ClothingFit>(f[FIT]),
            warmth = enumOrNull<Warmth>(f[WARMTH]),
            occasions = f[OCCASIONS].orEmpty().split(',').mapNotNull { enumOrNull<Occasion>(it.trim()) }.toSet(),
        )
        return item to attributes
    }

    fun fieldLabel(field: String): String = when (field) {
        NAME -> "Name"
        CATEGORY -> "Category"
        COLOR -> "Main colour"
        ARCHIVED -> "Archived"
        TYPE -> "Type"
        SECONDARY_COLOR -> "Second colour"
        PATTERN -> "Pattern"
        MATERIAL -> "Material"
        FIT -> "Fit"
        WARMTH -> "Warmth"
        OCCASIONS -> "Occasions"
        ThreeWayMerge.DELETED_FIELD -> "Keep or delete"
        else -> field
    }

    /** A stored value in words: "Navy" rather than "#1F2A44", "Work / smart" rather than "WORK". */
    fun displayValue(field: String, value: String?): String = when (field) {
        ThreeWayMerge.DELETED_FIELD -> if (value == "1") "Deleted" else "Kept"
        ARCHIVED -> if (value == "1") "Archived" else "Not archived"
        else -> when {
            value.isNullOrBlank() -> "Not set"
            field == CATEGORY -> enumOrNull<ClothingCategory>(value)?.label ?: value
            field == TYPE -> enumOrNull<GarmentType>(value)?.label ?: value
            field == PATTERN -> enumOrNull<ClothingPattern>(value)?.label ?: value
            field == MATERIAL -> enumOrNull<ClothingMaterial>(value)?.label ?: value
            field == FIT -> enumOrNull<ClothingFit>(value)?.label ?: value
            field == WARMTH -> enumOrNull<Warmth>(value)?.label ?: value
            field == OCCASIONS -> value.split(',').joinToString(", ") { enumOrNull<Occasion>(it.trim())?.label ?: it }
            field == COLOR || field == SECONDARY_COLOR -> ColorFamily.ofHex(value)?.label ?: value
            else -> value
        }
    }

    /** Only free text can sensibly be retyped during a conflict. */
    fun allowsTypedValue(field: String): Boolean = field == NAME

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
}
