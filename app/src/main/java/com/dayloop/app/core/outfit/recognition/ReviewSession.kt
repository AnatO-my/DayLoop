package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.label
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Three plain-language levels shown beside each recognised detail. */
enum class Sureness(val label: String, val filledSteps: Int) {
    SURE("Sure", 3),
    CHECK("Check", 2),
    CANT_TELL("Can't tell", 0);

    companion object {
        fun of(scored: Scored<*>?): Sureness = when (scored?.band) {
            ConfidenceBand.CONFIDENT -> SURE
            ConfidenceBand.TENTATIVE -> CHECK
            else -> CANT_TELL
        }
    }
}

data class SummaryRow(
    val label: String,
    /** Null means the detail is not set. */
    val value: String?,
    val sureness: Sureness,
    val colorHex: String? = null,
)

/** The compact "Navy jeans?" confirmation shown before the full editor. */
object ReviewSummary {
    /** Offered only when the draft could be saved as it stands; otherwise the full editor opens. */
    fun quickConfirmAvailable(prediction: GarmentPrediction?, draft: GarmentDraft): Boolean =
        prediction?.category != null && draft.blockingProblem == null

    fun headline(draft: GarmentDraft): String = if (draft.name.isBlank()) "New item" else "${draft.name.trim()}?"

    fun rows(prediction: GarmentPrediction): List<SummaryRow> = listOf(
        SummaryRow("Category", prediction.category?.value?.label, Sureness.of(prediction.category)),
        SummaryRow("Type", prediction.type?.value?.label, Sureness.of(prediction.type)),
        SummaryRow(
            "Colour",
            prediction.primaryColor?.takeIf { it.band != ConfidenceBand.UNCERTAIN }?.value?.family?.label,
            Sureness.of(prediction.primaryColor),
            prediction.primaryColor?.takeIf { it.band != ConfidenceBand.UNCERTAIN }?.value?.hex,
        ),
        // Fibre content cannot be established from a picture, whatever a label suggests.
        SummaryRow("Material", null, Sureness.CANT_TELL),
    )
}

/** One item found in (or drawn on) a photo, with the user's current edits. */
data class ReviewEntry(
    val key: Int,
    val prediction: GarmentPrediction?,
    val draft: GarmentDraft,
    /** Ticked for saving. Unsure items start unticked so nothing unreviewed is added. */
    val included: Boolean,
    val drawnByUser: Boolean = false,
) {
    val ready: Boolean get() = draft.blockingProblem == null

    val title: String
        get() = draft.name.trim().ifEmpty { draft.category?.let { "${it.label}?" } ?: "Unknown item" }

    val status: String
        get() = when {
            !ready -> if (draft.name.isBlank()) "Needs a name and a check" else draft.blockingProblem.orEmpty()
            prediction == null -> "${draft.category?.label.orEmpty()} · entered by you"
            else -> "${draft.category?.label.orEmpty()} · ${Sureness.of(prediction.category).label.lowercase()}"
        }
}

/** The set of items being reviewed from one photo. Immutable; every change returns a copy. */
data class ReviewSession(val entries: List<ReviewEntry>) {
    /** Exactly what "Add N items" will save. */
    val toSave: List<ReviewEntry> get() = entries.filter { it.included && it.ready }

    /** Ticked but not saveable yet; the UI must say so rather than drop them silently. */
    val tickedButIncomplete: List<ReviewEntry> get() = entries.filter { it.included && !it.ready }

    fun entry(key: Int): ReviewEntry? = entries.firstOrNull { it.key == key }

    fun setIncluded(key: Int, included: Boolean): ReviewSession =
        copy(entries = entries.map { if (it.key == key) it.copy(included = included) else it })

    /** Saving an edit ticks the item once it is complete: reviewing it is the user's confirmation. */
    fun update(key: Int, draft: GarmentDraft): ReviewSession =
        copy(entries = entries.map { if (it.key == key) it.copy(draft = draft, included = draft.blockingProblem == null) else it })

    fun add(prediction: GarmentPrediction?, drawnByUser: Boolean): ReviewSession {
        val key = (entries.maxOfOrNull { it.key } ?: 0) + 1
        val draft = prediction?.let(GarmentDraft::from) ?: GarmentDraft()
        return copy(entries = entries + ReviewEntry(key, prediction, draft, included = draft.blockingProblem == null, drawnByUser = drawnByUser))
    }

    fun remove(keys: Collection<Int>): ReviewSession = copy(entries = entries.filterNot { it.key in keys })

    companion object {
        fun from(predictions: List<GarmentPrediction>): ReviewSession =
            predictions.fold(ReviewSession(emptyList())) { session, prediction -> session.add(prediction, drawnByUser = false) }
    }
}

/** Geometry for outlines on the photo, in fractions of its width and height. */
object RegionMath {
    /** A drawn box must cover at least this share of each edge to count as deliberate. */
    const val MIN_EDGE = 0.08f

    fun contains(box: NormalizedBox, x: Float, y: Float): Boolean = x in box.left..box.right && y in box.top..box.bottom

    /** Converts a drag between two points (in pixels) to a box, or null when it is too small. */
    fun fromDrag(startX: Float, startY: Float, endX: Float, endY: Float, width: Int, height: Int): NormalizedBox? {
        if (width <= 0 || height <= 0) return null
        val left = (min(startX, endX) / width).coerceIn(0f, 1f)
        val right = (max(startX, endX) / width).coerceIn(0f, 1f)
        val top = (min(startY, endY) / height).coerceIn(0f, 1f)
        val bottom = (max(startY, endY) / height).coerceIn(0f, 1f)
        if (abs(right - left) < MIN_EDGE || abs(bottom - top) < MIN_EDGE) return null
        return NormalizedBox(left, top, right, bottom)
    }

    /** The topmost (last drawn) region under a tap, so small boxes inside large ones stay reachable. */
    fun <T> hit(regions: List<Pair<T, NormalizedBox?>>, x: Float, y: Float): T? =
        regions.filter { it.second != null && contains(it.second!!, x, y) }
            .minByOrNull { (it.second!!.right - it.second!!.left) * (it.second!!.bottom - it.second!!.top) }?.first
}
