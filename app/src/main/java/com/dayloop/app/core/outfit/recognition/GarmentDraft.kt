package com.dayloop.app.core.outfit.recognition

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
import com.dayloop.app.core.model.RecognitionStamp
import com.dayloop.app.core.model.Warmth
import java.util.Locale

/**
 * What the user sees and edits before anything is saved. A draft made from a prediction
 * pre-fills only fields the recogniser was at least tentatively sure about; everything
 * else starts empty so the user chooses instead of inheriting a forced guess.
 */
data class GarmentDraft(
    val name: String = "",
    val category: ClothingCategory? = null,
    val type: GarmentType? = null,
    val colorHex: String? = null,
    val secondaryColorHex: String? = null,
    val pattern: ClothingPattern? = null,
    val material: ClothingMaterial? = null,
    val fit: ClothingFit? = null,
    val warmth: Warmth? = null,
    val occasions: Set<Occasion> = emptySet(),
) {
    /** Null when the draft can be saved, otherwise what the user still has to do. */
    val blockingProblem: String?
        get() = when {
            name.isBlank() -> "Give the item a name."
            category == null -> "Choose a category."
            colorHex != null && !HexColor.isValid(colorHex) -> "Colour must look like #RRGGBB."
            else -> null
        }

    /** Choosing a category clears a type that belongs to a different one. */
    fun withCategory(next: ClothingCategory): GarmentDraft =
        copy(category = next, type = type?.takeIf { it.category == next })

    /** Choosing a type also settles the category, since a type implies it. */
    fun withType(next: GarmentType?): GarmentDraft =
        if (next == null) copy(type = null) else copy(type = next, category = next.category)

    fun toItem(id: ClothingItemId, isArchived: Boolean = false): ClothingItem {
        check(blockingProblem == null) { blockingProblem.orEmpty() }
        return ClothingItem(id, name.trim(), checkNotNull(category), colorHex?.uppercase(Locale.US), isArchived)
    }

    fun toAttributes(recognition: RecognitionStamp? = null): GarmentAttributes = GarmentAttributes(
        type = type?.takeIf { it.category == category },
        secondaryColorHex = secondaryColorHex?.takeIf(HexColor::isValid)?.uppercase(Locale.US),
        pattern = pattern, material = material, fit = fit, warmth = warmth, occasions = occasions,
        recognition = recognition,
    )

    companion object {
        fun from(prediction: GarmentPrediction): GarmentDraft {
            val type = prediction.type?.value
            val category = prediction.category?.value
            val color = prediction.primaryColor?.takeIf { it.band != ConfidenceBand.UNCERTAIN }?.value
            return GarmentDraft(
                name = suggestName(color?.family, type),
                category = category,
                type = type?.takeIf { it.category == category },
                colorHex = color?.hex,
                secondaryColorHex = prediction.secondaryColor?.takeIf { it.band != ConfidenceBand.UNCERTAIN }?.value?.hex,
                pattern = prediction.pattern?.takeIf { it.band != ConfidenceBand.UNCERTAIN }?.value,
                material = null, // a photo cannot establish fibre content; see materialHint
            )
        }

        fun from(item: ClothingItem, attributes: GarmentAttributes?): GarmentDraft = GarmentDraft(
            name = item.name, category = item.category, type = attributes?.type?.takeIf { it.category == item.category },
            colorHex = item.colorHex, secondaryColorHex = attributes?.secondaryColorHex,
            pattern = attributes?.pattern, material = attributes?.material, fit = attributes?.fit,
            warmth = attributes?.warmth, occasions = attributes?.occasions.orEmpty(),
        )

        /** "Navy jeans" when both are known; the type alone otherwise; empty when unknown. */
        fun suggestName(color: ColorFamily?, type: GarmentType?): String = when {
            type == null -> ""
            color == null -> type.label
            else -> "${color.label} ${type.label.lowercase(Locale.US)}"
        }

        /** Records what the model proposed versus what the user kept, for later evaluation. */
        fun stamp(prediction: GarmentPrediction, saved: GarmentDraft, models: List<ModelInfo>): RecognitionStamp {
            val primary = models.firstOrNull { it.id == ShapeModelWeights.MODEL_ID } ?: models.firstOrNull()
            return RecognitionStamp(
                modelId = primary?.id ?: "none",
                modelVersion = primary?.version ?: "0",
                typeConfidence = prediction.type?.confidence,
                userCorrected = prediction.category?.value != saved.category ||
                    (prediction.type != null && prediction.type.value != saved.type),
            )
        }
    }
}
