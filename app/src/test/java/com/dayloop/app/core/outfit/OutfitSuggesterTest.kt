package com.dayloop.app.core.outfit

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OutfitSuggesterTest {

    @Test
    fun `suggestOutfit returns empty list when wardrobe is empty`() {
        val suggestion = OutfitSuggester.suggestOutfit(emptyList())
        assertTrue(suggestion.isEmpty())
    }

    @Test
    fun `suggestOutfit selects one item from each available category`() {
        val t1 = ClothingItem(ClothingItemId("1"), "White Tee", ClothingCategory.TOP)
        val t2 = ClothingItem(ClothingItemId("2"), "Black Tee", ClothingCategory.TOP)
        val b1 = ClothingItem(ClothingItemId("3"), "Blue Jeans", ClothingCategory.BOTTOM)
        val f1 = ClothingItem(ClothingItemId("4"), "Sneakers", ClothingCategory.FOOTWEAR)

        val wardrobe = listOf(t1, t2, b1, f1)
        
        // Use a fixed random seed for deterministic test choice
        val random = Random(42)
        val suggestion = OutfitSuggester.suggestOutfit(wardrobe, random)

        // Should pick one top, one bottom, one footwear
        assertEquals(3, suggestion.size)
        assertTrue(suggestion.any { it.category == ClothingCategory.TOP })
        assertTrue(suggestion.any { it.category == ClothingCategory.BOTTOM })
        assertTrue(suggestion.any { it.category == ClothingCategory.FOOTWEAR })
    }
}
