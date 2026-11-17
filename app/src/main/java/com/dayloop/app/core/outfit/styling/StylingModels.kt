package com.dayloop.app.core.outfit.styling

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import java.time.LocalDate

/** A wardrobe item with whatever the user has confirmed about it and when it was last worn. */
data class WardrobeEntry(
    val item: ClothingItem,
    val attributes: GarmentAttributes? = null,
    val lastWorn: LocalDate? = null,
)

enum class Weather(val label: String) { HOT("Hot"), MILD("Mild"), COLD("Cold") }

enum class StylePreference(val label: String) {
    MINIMAL("Minimal / neutral"),
    CLASSIC("Classic"),
    BOLD("Colourful"),
}

/** Everything the user may tell the recommender. Every field except [today] is optional. */
data class StylingRequest(
    val today: LocalDate,
    /** Build every outfit around this item ("style this piece"). */
    val anchorItemId: ClothingItemId? = null,
    val occasion: Occasion? = null,
    val weather: Weather? = null,
    val rainExpected: Boolean = false,
    val style: StylePreference? = null,
    val comfortFirst: Boolean = false,
    /** Items worn within this many days are marked down; 0 disables the check. */
    val avoidWornWithinDays: Int = 2,
    val excludedItemIds: Set<ClothingItemId> = emptySet(),
    val maxResults: Int = 3,
    /** Exact item combinations the user has said are not for them; never proposed again. */
    val rejectedCombinations: Set<Set<ClothingItemId>> = emptySet(),
) {
    init {
        require(avoidWornWithinDays in 0..60) { "avoidWornWithinDays must be 0..60." }
        require(maxResults in 1..10) { "maxResults must be 1..10." }
    }
}

data class OutfitPiece(val item: ClothingItem, val slot: ClothingCategory)

data class OutfitRecommendation(
    /** Only items the user owns. */
    val pieces: List<OutfitPiece>,
    /** 0..100 agreement with DayLoop's style guidelines. A heuristic, not a measure of taste. */
    val matchScore: Int,
    val reasons: List<String>,
    val cautions: List<String>,
    /** Requested constraints this outfit does not meet, stated plainly. Empty when all are met. */
    val unmetConstraints: List<String>,
    /** False when a core slot (top, bottom or footwear) could not be filled from the wardrobe. */
    val complete: Boolean,
) {
    /** One or two sentences for compact surfaces such as the Today card. */
    val headline: String
        get() = reasons.take(2).joinToString(" ").ifEmpty { "Built from items in your wardrobe." }

    val itemIds: Set<ClothingItemId> get() = pieces.map { it.item.id }.toSet()
}

/** Something the user does NOT own. Shown separately and labelled as a suggestion. */
data class SuggestedAddition(
    val category: ClothingCategory,
    val description: String,
    val reason: String,
)

data class StylingResult(
    val outfits: List<OutfitRecommendation>,
    val suggestedAdditions: List<SuggestedAddition>,
    val notes: List<String>,
) {
    companion object {
        const val DISCLAIMER =
            "These follow general style guidelines and the details you saved. They are a starting point, not a verdict on taste."
    }
}

/** Typical properties by garment type, used only when the user has not set the attribute. */
internal object GarmentDefaults {
    private val all = Occasion.entries.toSet()

    fun warmth(type: GarmentType): Warmth? = when (type) {
        GarmentType.T_SHIRT, GarmentType.SHIRT, GarmentType.POLO, GarmentType.SHORTS,
        GarmentType.SKIRT, GarmentType.SANDALS -> Warmth.LIGHT
        GarmentType.DRESS, GarmentType.TROUSERS, GarmentType.JEANS, GarmentType.LEGGINGS,
        GarmentType.JACKET, GarmentType.BLAZER, GarmentType.SNEAKERS, GarmentType.SHOES -> Warmth.MEDIUM
        GarmentType.SWEATER, GarmentType.COAT, GarmentType.BOOTS, GarmentType.BEANIE, GarmentType.SCARF -> Warmth.WARM
        GarmentType.HAT, GarmentType.CAP, GarmentType.BAG, GarmentType.TIE,
        GarmentType.EYEWEAR, GarmentType.JEWELLERY -> null
    }

    fun occasions(type: GarmentType): Set<Occasion> = when (type) {
        GarmentType.T_SHIRT, GarmentType.SHORTS, GarmentType.SNEAKERS, GarmentType.LEGGINGS ->
            setOf(Occasion.CASUAL, Occasion.ACTIVE)
        GarmentType.SHIRT, GarmentType.TROUSERS, GarmentType.DRESS, GarmentType.SHOES, GarmentType.COAT ->
            setOf(Occasion.CASUAL, Occasion.WORK, Occasion.FORMAL)
        GarmentType.POLO, GarmentType.SWEATER, GarmentType.SKIRT, GarmentType.BOOTS ->
            setOf(Occasion.CASUAL, Occasion.WORK)
        GarmentType.JEANS, GarmentType.JACKET, GarmentType.SANDALS, GarmentType.HAT,
        GarmentType.CAP, GarmentType.BEANIE -> setOf(Occasion.CASUAL)
        GarmentType.BLAZER, GarmentType.TIE -> setOf(Occasion.WORK, Occasion.FORMAL)
        GarmentType.BAG, GarmentType.SCARF, GarmentType.EYEWEAR, GarmentType.JEWELLERY -> all
    }
}
