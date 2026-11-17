package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentType

/**
 * Stable recognition output. Bump [GarmentPrediction.SCHEMA_VERSION] on any breaking change.
 *
 * A prediction is a draft for the user to review. Nothing here is written to the wardrobe
 * until the user confirms it.
 */
data class RecognitionResult(
    val garments: List<GarmentPrediction>,
    val notes: Set<RecognitionNote>,
    val models: List<ModelInfo>,
    val elapsedMillis: Long,
)

data class GarmentPrediction(
    /** Where the garment sits in the photo, as fractions of width/height; null for the whole image. */
    val region: NormalizedBox?,
    /** Null means "not sure": the user must choose. */
    val category: Scored<ClothingCategory>?,
    /** Null means "not sure". Never forced to the most likely class. */
    val type: Scored<GarmentType>?,
    /** Other plausible types, best first, for a quick correction. */
    val alternatives: List<Scored<GarmentType>>,
    val primaryColor: Scored<NamedColor>?,
    val secondaryColor: Scored<NamedColor>?,
    val pattern: Scored<ClothingPattern>?,
    /** Only ever a hint from generic image labels; never pre-filled as fact. */
    val materialHint: Scored<ClothingMaterial>?,
    val notes: Set<RecognitionNote>,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

data class Scored<T>(
    val value: T,
    /** 0..1. Calibrated only for the shape model on its own test set; treat as a ranking aid. */
    val confidence: Float,
    val band: ConfidenceBand,
    val source: EvidenceSource,
)

enum class ConfidenceBand { CONFIDENT, TENTATIVE, UNCERTAIN }

enum class EvidenceSource { SHAPE_MODEL, IMAGE_LABELS, COMBINED, COLOR_ANALYSIS }

enum class RecognitionNote(val message: String) {
    PERSON_IN_PHOTO("This looks like a photo of someone wearing clothes. Shape recognition works best on a single item laid flat or hung up."),
    NO_CUTOUT("The item could not be separated from the background, so colours may include the background."),
    SOURCES_DISAGREE("The recognisers disagreed about this item. Please check the category."),
    SMALL_IMAGE("The photo is small; results may be less reliable."),
    SEVERAL_ITEMS("More than one item was found. Review each one."),
    LIGHTING_AFFECTS_COLOR("Colour depends on lighting. Adjust it if it looks wrong."),
}

data class NormalizedBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class ModelInfo(val id: String, val version: String, val role: String)

/** A colour family with the representative shade actually seen. */
data class NamedColor(val family: ColorFamily, val hex: String)

/**
 * Thresholds, in one place so they can be tuned against real data later.
 *
 * Evidence (docs/wardrobe/EVALUATION.md): on the Fashion-MNIST test set the shape model's
 * type guesses at confidence >= 0.80 were right 96.3% of the time and covered 81.8% of
 * images; at >= 0.55 accuracy when shown was about 92%. Phone photos are out of
 * distribution, so a shape-only guess is never shown as CONFIDENT (see [PredictionFusion]).
 */
object RecognitionPolicy {
    const val CONFIDENT = 0.80f
    const val TENTATIVE = 0.55f
    /** ML Kit labels below this are ignored as evidence. */
    const val LABEL_FLOOR = 0.50f
    /** A person-related label at or above this disables the shape model for the photo. */
    const val PERSON_CUE = 0.60f
    const val MAX_GARMENTS = 4
    /** A segmented subject smaller than this share of the photo is ignored. */
    const val MIN_SUBJECT_AREA = 0.02f

    fun band(confidence: Float): ConfidenceBand = when {
        confidence >= CONFIDENT -> ConfidenceBand.CONFIDENT
        confidence >= TENTATIVE -> ConfidenceBand.TENTATIVE
        else -> ConfidenceBand.UNCERTAIN
    }
}
