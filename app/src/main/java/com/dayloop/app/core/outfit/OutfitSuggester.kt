package com.dayloop.app.core.outfit

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.WardrobeMedia
import kotlin.random.Random

data class SuggestibleItem(
    val item: ClothingItem,
    val media: List<WardrobeMedia>
)

object OutfitSuggester {
    fun suggestOutfitFromMedia(available: List<SuggestibleItem>, random: Random = Random): List<ClothingItem> {
        val active = available.filter { !it.item.isArchived }
        if (active.isEmpty()) return emptyList()

        val grouped = active.groupBy { it.item.category }
        val suggestion = mutableListOf<ClothingItem>()

        // Core layers (prioritize items with high confidence labels if they existed, for now random)
        fun pick(category: ClothingCategory) {
            grouped[category]?.randomOrNull(random)?.let { suggestion.add(it.item) }
        }

        pick(ClothingCategory.TOP)
        pick(ClothingCategory.BOTTOM)
        pick(ClothingCategory.FOOTWEAR)
        pick(ClothingCategory.HEADWEAR)
        pick(ClothingCategory.OUTERWEAR)
        pick(ClothingCategory.ACCESSORY)

        return suggestion
    }

    // Keep legacy support for now
    fun suggestOutfit(availableItems: List<ClothingItem>, random: Random = Random): List<ClothingItem> {
        return suggestOutfitFromMedia(availableItems.map { SuggestibleItem(it, emptyList()) }, random)
    }
}
