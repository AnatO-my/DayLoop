package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.GarmentType
import kotlin.math.max
import kotlin.math.min

/**
 * Combines the shape model, generic image labels and colour analysis into one draft.
 *
 * Rules, in order:
 * 1. A person in the frame disables the shape model (it was trained on single flat items).
 * 2. Two independent sources agreeing on a category raise confidence; disagreement lowers
 *    it and is reported, never hidden.
 * 3. A shape-only guess is capped at TENTATIVE: its calibration was measured on studio
 *    product images, not phone photos.
 * 4. Below the TENTATIVE threshold the field is left empty for the user to choose.
 */
object PredictionFusion {
    private const val SHAPE_ONLY_CAP = 0.79f
    private const val AGREEMENT_CAP = 0.97f

    fun fuse(
        shapeProbabilities: FloatArray?,
        labels: LabelEvidence,
        colors: ColorReading,
        region: NormalizedBox? = null,
        extraNotes: Set<RecognitionNote> = emptySet(),
    ): GarmentPrediction {
        val notes = extraNotes.toMutableSet()
        val personPresent = labels.personCue >= RecognitionPolicy.PERSON_CUE
        if (personPresent) notes += RecognitionNote.PERSON_IN_PHOTO
        val shape = shapeProbabilities?.takeUnless { personPresent }?.also {
            require(it.size == ShapeClass.entries.size) { "Expected ${ShapeClass.entries.size} shape probabilities." }
        }

        val shapeTypes: List<Pair<GarmentType, Float>> = shape
            ?.mapIndexed { index, p -> ShapeClass.entries[index].garmentType to p }
            ?.sortedByDescending { it.second }.orEmpty()
        val shapeCategories: Map<ClothingCategory, Float> = shapeTypes
            .groupBy({ it.first.category }, { it.second }).mapValues { it.value.sum() }
        val shapeTop = shapeCategories.maxByOrNull { it.value }
        val labelTop = labels.categories.maxByOrNull { it.value }

        var category: Scored<ClothingCategory>? = null
        var type: Scored<GarmentType>? = null
        when {
            shapeTop != null && labelTop != null && shapeTop.key == labelTop.key -> {
                val hi = max(shapeTop.value, labelTop.value); val lo = min(shapeTop.value, labelTop.value)
                val confidence = min(AGREEMENT_CAP, hi + 0.5f * lo * (1f - hi))
                category = scored(shapeTop.key, confidence, EvidenceSource.COMBINED)
                // Labels were learnt from real photos and name finer types (jeans vs trousers).
                val labelType = labels.types.filterKeys { it.category == shapeTop.key }.maxByOrNull { it.value }
                val shapeType = shapeTypes.first { it.first.category == shapeTop.key }
                type = if (labelType != null) scored(labelType.key, min(confidence, labelType.value + 0.1f), EvidenceSource.COMBINED)
                else scored(shapeType.first, min(SHAPE_ONLY_CAP, shapeType.second), EvidenceSource.SHAPE_MODEL)
            }
            shapeTop != null && labelTop != null -> {
                notes += RecognitionNote.SOURCES_DISAGREE
                val labelWins = labelTop.value >= shapeTop.value
                val confidence = min(SHAPE_ONLY_CAP, max(shapeTop.value, labelTop.value) - 0.5f * min(shapeTop.value, labelTop.value))
                if (labelWins) {
                    category = scored(labelTop.key, confidence, EvidenceSource.IMAGE_LABELS)
                    labels.types.filterKeys { it.category == labelTop.key }.maxByOrNull { it.value }
                        ?.let { type = scored(it.key, min(confidence, it.value), EvidenceSource.IMAGE_LABELS) }
                } else {
                    category = scored(shapeTop.key, confidence, EvidenceSource.SHAPE_MODEL)
                    val shapeType = shapeTypes.first { it.first.category == shapeTop.key }
                    type = scored(shapeType.first, min(confidence, shapeType.second), EvidenceSource.SHAPE_MODEL)
                }
            }
            shapeTop != null -> {
                category = scored(shapeTop.key, min(SHAPE_ONLY_CAP, shapeTop.value), EvidenceSource.SHAPE_MODEL)
                val best = shapeTypes.first()
                if (best.first.category == shapeTop.key) {
                    type = scored(best.first, min(SHAPE_ONLY_CAP, best.second), EvidenceSource.SHAPE_MODEL)
                }
            }
            labelTop != null -> {
                category = scored(labelTop.key, labelTop.value, EvidenceSource.IMAGE_LABELS)
                labels.types.filterKeys { it.category == labelTop.key }.maxByOrNull { it.value }
                    ?.let { type = scored(it.key, it.value, EvidenceSource.IMAGE_LABELS) }
            }
        }
        // "Not sure" is a result: drop anything under the tentative threshold.
        category = category?.takeIf { it.band != ConfidenceBand.UNCERTAIN }
        type = type?.takeIf { it.band != ConfidenceBand.UNCERTAIN && category != null }

        val alternatives = (shapeTypes.map { Triple(it.first, min(SHAPE_ONLY_CAP, it.second), EvidenceSource.SHAPE_MODEL) } +
            labels.types.map { Triple(it.key, it.value, EvidenceSource.IMAGE_LABELS) })
            .filter { it.first != type?.value && it.second >= 0.10f }
            .sortedByDescending { it.second }
            .distinctBy { it.first }
            .take(3)
            .map { Scored(it.first, it.second, RecognitionPolicy.band(it.second), it.third) }

        val materialHint: Scored<ClothingMaterial>? = labels.material?.let { (material, confidence) ->
            // A texture label is weak evidence of fibre content: at most a suggestion.
            Scored(material, min(confidence, SHAPE_ONLY_CAP), ConfidenceBand.TENTATIVE, EvidenceSource.IMAGE_LABELS)
        }
        if (colors.primary != null) notes += RecognitionNote.LIGHTING_AFFECTS_COLOR

        return GarmentPrediction(
            region = region,
            category = category,
            type = type,
            alternatives = alternatives,
            primaryColor = colors.primary,
            secondaryColor = colors.secondary,
            pattern = colors.pattern,
            materialHint = materialHint,
            notes = notes,
        )
    }

    private fun <T> scored(value: T, confidence: Float, source: EvidenceSource): Scored<T> {
        val bounded = confidence.coerceIn(0f, 1f)
        return Scored(value, bounded, RecognitionPolicy.band(bounded), source)
    }
}
