package com.dayloop.app.core.outfit.styling

import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OutfitFeedbackTest {
    private val today = LocalDate.of(2026, 11, 10)
    private fun ids(vararg names: String) = names.map(::ClothingItemId).toSet()
    private fun entry(id: String, type: GarmentType, color: String) =
        WardrobeEntry(ClothingItem(ClothingItemId(id), id, type.category, color), GarmentAttributes(type = type))

    private val wardrobe = listOf(
        entry("tee a", GarmentType.T_SHIRT, "#F5F5F5"), entry("tee b", GarmentType.T_SHIRT, "#1A1A1A"),
        entry("jeans", GarmentType.JEANS, "#1F2A44"), entry("sneakers", GarmentType.SNEAKERS, "#F5F5F5"),
    )

    @Test
    fun `a turned-down combination is not suggested again`() {
        val first = OutfitRecommender.recommend(wardrobe, StylingRequest(today)).outfits.first()
        val again = OutfitRecommender.recommend(wardrobe, StylingRequest(today, rejectedCombinations = setOf(first.itemIds)))
        assertTrue(again.outfits.isNotEmpty())
        assertTrue(again.outfits.none { it.itemIds == first.itemIds })
    }

    @Test
    fun `rejecting every combination leaves no suggestion rather than repeating one`() {
        val all = OutfitRecommender.recommend(wardrobe, StylingRequest(today, maxResults = 10)).outfits.map { it.itemIds }.toSet()
        assertEquals(2, all.size)
        assertTrue(OutfitRecommender.recommend(wardrobe, StylingRequest(today, rejectedCombinations = all)).outfits.isEmpty())
    }

    @Test
    fun `taste rejections last thirty days and day-bound ones only that day`() {
        val feedback = listOf(
            OutfitFeedback(ids("a"), FeedbackVerdict.NOT_FOR_ME, FeedbackReason.COLOURS, today.minusDays(30)),
            OutfitFeedback(ids("b"), FeedbackVerdict.NOT_FOR_ME, FeedbackReason.COLOURS, today.minusDays(31)),
            OutfitFeedback(ids("c"), FeedbackVerdict.NOT_FOR_ME, FeedbackReason.WEATHER, today),
            OutfitFeedback(ids("d"), FeedbackVerdict.NOT_FOR_ME, FeedbackReason.WEATHER, today.minusDays(1)),
            OutfitFeedback(ids("e"), FeedbackVerdict.WORN, null, today),
            OutfitFeedback(ids("f"), FeedbackVerdict.NOT_FOR_ME, null, today.plusDays(1)),
        )
        assertEquals(setOf(ids("a"), ids("c")), OutfitFeedbackPolicy.rejectedCombinations(feedback, today))
    }

    @Test
    fun `headline is a short readable reason`() {
        val outfit = OutfitRecommender.recommend(wardrobe, StylingRequest(today)).outfits.first()
        assertTrue(outfit.headline.isNotBlank())
        assertFalse(outfit.headline.contains("\n"))
        val bare = OutfitRecommendation(emptyList(), 50, emptyList(), emptyList(), emptyList(), true)
        assertEquals("Built from items in your wardrobe.", bare.headline)
    }
}
