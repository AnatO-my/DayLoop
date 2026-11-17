package com.dayloop.app.core.outfit.styling

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class OutfitRecommenderTest {
    private val today = LocalDate.of(2026, 11, 10)

    private fun entry(
        id: String, type: GarmentType, color: String? = null, lastWorn: LocalDate? = null,
        pattern: ClothingPattern? = null, archived: Boolean = false, warmth: Warmth? = null,
        occasions: Set<Occasion> = emptySet(),
    ) = WardrobeEntry(
        ClothingItem(ClothingItemId(id), id, type.category, color, archived),
        GarmentAttributes(type = type, pattern = pattern, warmth = warmth, occasions = occasions),
        lastWorn,
    )

    private val white = "#F5F5F5"; private val navy = "#1F2A44"; private val black = "#1A1A1A"
    private val red = "#C8322B"; private val green = "#2E8B57"; private val beige = "#D8C3A5"

    private val wardrobe = listOf(
        entry("white shirt", GarmentType.SHIRT, white),
        entry("red tee", GarmentType.T_SHIRT, red),
        entry("grey sweater", GarmentType.SWEATER, "#8A8A8A"),
        entry("navy trousers", GarmentType.TROUSERS, navy),
        entry("blue jeans", GarmentType.JEANS, "#2F4F7F"),
        entry("green shorts", GarmentType.SHORTS, green),
        entry("black shoes", GarmentType.SHOES, black),
        entry("white sneakers", GarmentType.SNEAKERS, white),
        entry("sandals", GarmentType.SANDALS, beige),
        entry("black coat", GarmentType.COAT, black),
        entry("navy blazer", GarmentType.BLAZER, navy),
    )

    private fun names(o: OutfitRecommendation) = o.pieces.map { it.item.name }.toSet()

    @Test
    fun `empty wardrobe explains itself instead of inventing an outfit`() {
        val result = OutfitRecommender.recommend(emptyList(), StylingRequest(today))
        assertTrue(result.outfits.isEmpty())
        assertTrue(result.notes.single().contains("no active items"))
    }

    @Test
    fun `only owned, active, non-excluded items are ever used`() {
        val withArchived = wardrobe + entry("archived tee", GarmentType.T_SHIRT, white, archived = true)
        val excluded = setOf(ClothingItemId("white shirt"))
        val result = OutfitRecommender.recommend(withArchived, StylingRequest(today, excludedItemIds = excluded, maxResults = 10))
        val owned = wardrobe.map { it.item.name }.toSet()
        result.outfits.forEach { outfit ->
            assertTrue(owned.containsAll(names(outfit)))
            assertFalse("white shirt" in names(outfit))
            assertFalse("archived tee" in names(outfit))
        }
    }

    @Test
    fun `a complete outfit has one top, one bottom and footwear`() {
        val best = OutfitRecommender.recommend(wardrobe, StylingRequest(today)).outfits.first()
        assertTrue(best.complete)
        assertEquals(setOf(ClothingCategory.TOP, ClothingCategory.BOTTOM, ClothingCategory.FOOTWEAR), best.pieces.map { it.slot }.toSet())
        assertEquals(3, best.pieces.size)
    }

    @Test
    fun `formal request picks pieces that suit it and says why`() {
        val best = OutfitRecommender.recommend(wardrobe, StylingRequest(today, occasion = Occasion.FORMAL)).outfits.first()
        assertTrue(names(best).containsAll(setOf("white shirt", "navy trousers", "black shoes")))
        assertTrue(best.unmetConstraints.isEmpty())
        assertTrue(best.reasons.any { it.contains("formal") })
    }

    @Test
    fun `an impossible constraint is reported, not hidden`() {
        val casualOnly = listOf(
            entry("tee", GarmentType.T_SHIRT, white), entry("jeans", GarmentType.JEANS, navy), entry("sneakers", GarmentType.SNEAKERS, white),
        )
        val result = OutfitRecommender.recommend(casualOnly, StylingRequest(today, occasion = Occasion.FORMAL))
        val best = result.outfits.first()
        assertTrue(best.unmetConstraints.any { it.contains("not marked for formal") })
        assertTrue(result.suggestedAdditions.any { it.description.contains("formal") })
    }

    @Test
    fun `hot weather avoids warm pieces and layers`() {
        val result = OutfitRecommender.recommend(wardrobe, StylingRequest(today, weather = Weather.HOT))
        result.outfits.forEach { assertTrue(names(it).none { n -> n in setOf("grey sweater", "black coat", "navy blazer") }) }
    }

    @Test
    fun `cold weather adds a layer`() {
        val best = OutfitRecommender.recommend(wardrobe, StylingRequest(today, weather = Weather.COLD)).outfits.first()
        assertTrue(best.pieces.any { it.slot == ClothingCategory.OUTERWEAR })
        assertTrue("sandals" !in names(best) && "green shorts" !in names(best))
    }

    @Test
    fun `rain rules out sandals when alternatives exist`() {
        val best = OutfitRecommender.recommend(wardrobe, StylingRequest(today, rainExpected = true)).outfits.first()
        assertFalse("sandals" in names(best))
    }

    @Test
    fun `styling an item keeps it in every suggestion`() {
        val result = OutfitRecommender.recommend(wardrobe, StylingRequest(today, anchorItemId = ClothingItemId("green shorts"), maxResults = 5))
        assertTrue(result.outfits.isNotEmpty())
        result.outfits.forEach { assertTrue("green shorts" in names(it)) }
    }

    @Test
    fun `clashing strong colours score below a neutral pairing`() {
        val items = listOf(
            entry("red tee", GarmentType.T_SHIRT, red), entry("white tee", GarmentType.T_SHIRT, white),
            entry("green shorts", GarmentType.SHORTS, green), entry("sneakers", GarmentType.SNEAKERS, white),
        )
        val outfits = OutfitRecommender.recommend(items, StylingRequest(today, maxResults = 5)).outfits
        val neutral = outfits.first { "white tee" in names(it) }
        val clash = outfits.first { "red tee" in names(it) }
        assertTrue(neutral.matchScore > clash.matchScore)
        assertTrue(clash.cautions.any { it.contains("unrelated colours") })
        assertEquals(neutral, outfits.first())
    }

    @Test
    fun `recently worn pieces are marked down and flagged`() {
        val items = listOf(
            entry("tee a", GarmentType.T_SHIRT, white, lastWorn = today.minusDays(1)),
            entry("tee b", GarmentType.T_SHIRT, white, lastWorn = today.minusDays(20)),
            entry("jeans", GarmentType.JEANS, navy), entry("sneakers", GarmentType.SNEAKERS, white),
        )
        val outfits = OutfitRecommender.recommend(items, StylingRequest(today)).outfits
        assertTrue("tee b" in names(outfits.first()))
        assertTrue(outfits.first { "tee a" in names(it) }.cautions.any { it.contains("worn in the last 2 days") })
    }

    @Test
    fun `missing footwear gives a partial outfit and a clearly separate suggested addition`() {
        val items = listOf(entry("tee", GarmentType.T_SHIRT, white), entry("jeans", GarmentType.JEANS, navy))
        val result = OutfitRecommender.recommend(items, StylingRequest(today))
        val best = result.outfits.first()
        assertFalse(best.complete)
        assertEquals(setOf("tee", "jeans"), names(best))
        assertTrue(result.suggestedAdditions.any { it.category == ClothingCategory.FOOTWEAR })
    }

    @Test
    fun `a dress fills the top and bottom slots`() {
        val items = listOf(entry("dress", GarmentType.DRESS, navy), entry("jeans", GarmentType.JEANS, navy), entry("shoes", GarmentType.SHOES, black))
        val result = OutfitRecommender.recommend(items, StylingRequest(today, anchorItemId = ClothingItemId("dress")))
        val best = result.outfits.first()
        assertTrue(best.complete)
        assertEquals(setOf("dress", "shoes"), names(best))
    }

    @Test
    fun `two patterned pieces draw a caution`() {
        val items = listOf(
            entry("striped tee", GarmentType.T_SHIRT, white, pattern = ClothingPattern.STRIPED),
            entry("checked trousers", GarmentType.TROUSERS, navy, pattern = ClothingPattern.CHECKED),
            entry("sneakers", GarmentType.SNEAKERS, white, pattern = ClothingPattern.SOLID),
        )
        val best = OutfitRecommender.recommend(items, StylingRequest(today)).outfits.first()
        assertTrue(best.cautions.any { it.contains("both patterned") })
    }

    @Test
    fun `items with no saved details are neutral and the result says so`() {
        val bare = listOf(
            WardrobeEntry(ClothingItem(ClothingItemId("a"), "a", ClothingCategory.TOP)),
            WardrobeEntry(ClothingItem(ClothingItemId("b"), "b", ClothingCategory.BOTTOM)),
            WardrobeEntry(ClothingItem(ClothingItemId("c"), "c", ClothingCategory.FOOTWEAR)),
        )
        val result = OutfitRecommender.recommend(bare, StylingRequest(today, occasion = Occasion.WORK, weather = Weather.HOT))
        assertEquals(50, result.outfits.first().matchScore)
        assertTrue(result.outfits.first().unmetConstraints.isEmpty())
        assertTrue(result.notes.any { it.contains("Few details") })
    }

    @Test
    fun `user-stated attributes override type defaults`() {
        val items = listOf(
            entry("linen shirt", GarmentType.SHIRT, white), entry("jeans", GarmentType.JEANS, navy, occasions = setOf(Occasion.FORMAL)),
            entry("shoes", GarmentType.SHOES, black),
        )
        val best = OutfitRecommender.recommend(items, StylingRequest(today, occasion = Occasion.FORMAL)).outfits.first()
        assertTrue(best.unmetConstraints.isEmpty())
    }

    @Test
    fun `results are deterministic and distinct`() {
        val request = StylingRequest(today, maxResults = 3)
        val first = OutfitRecommender.recommend(wardrobe, request)
        assertEquals(first, OutfitRecommender.recommend(wardrobe.shuffled(Random(7)), request))
        assertEquals(3, first.outfits.size)
        assertEquals(3, first.outfits.map(::names).toSet().size)
        assertTrue(first.outfits.zipWithNext().all { (a, b) -> a.matchScore >= b.matchScore })
    }

    /**
     * Synthetic evaluation: random wardrobes and requests, seeded. This measures whether the
     * engine honours its own contract. It says nothing about whether people like the outfits.
     */
    @Test
    fun `synthetic evaluation of contract metrics`() {
        val random = Random(20261110)
        val palette = listOf(white, navy, black, red, green, beige, "#8A8A8A", "#E8C52A", "#7A4BA8", null)
        var requests = 0; var satisfiable = 0; var satisfied = 0; var falseClaims = 0
        var itemsUsed = 0; var itemsOwned = 0; var distinctSum = 0.0; var multi = 0; var complete = 0; var completable = 0

        // Independent oracle: the same constraint definitions, checked by brute force.
        fun ok(e: WardrobeEntry, request: StylingRequest): Boolean {
            val type = e.attributes?.type
            val occasions = e.attributes?.occasions.orEmpty().ifEmpty { type?.let(GarmentDefaults::occasions).orEmpty() }
            if (request.occasion != null && occasions.isNotEmpty() && request.occasion !in occasions) return false
            val warmth = e.attributes?.warmth ?: type?.let(GarmentDefaults::warmth)
            if (warmth != null) when (request.weather) {
                Weather.HOT -> if (warmth == Warmth.WARM) return false
                Weather.MILD -> if (warmth == Warmth.WARM) return false
                Weather.COLD -> if (warmth == Warmth.LIGHT) return false
                null -> Unit
            }
            if (request.rainExpected && type == GarmentType.SANDALS) return false
            return true
        }

        repeat(300) { run ->
            val items = List(random.nextInt(4, 25)) { i ->
                entry("i$run-$i", GarmentType.entries.random(random), palette.random(random),
                    lastWorn = if (random.nextBoolean()) today.minusDays(random.nextLong(0, 30)) else null,
                    pattern = listOf(null, ClothingPattern.SOLID, ClothingPattern.STRIPED).random(random),
                    archived = random.nextInt(10) == 0)
            }
            val request = StylingRequest(
                today,
                occasion = (Occasion.entries + null).random(random),
                weather = (Weather.entries + null).random(random),
                rainExpected = random.nextInt(5) == 0,
                style = (StylePreference.entries + null).random(random),
                maxResults = 3,
            )
            val result = OutfitRecommender.recommend(items, request)
            requests++
            val active = items.filter { !it.item.isArchived }
            val activeIds = active.map { it.item.id }.toSet()
            result.outfits.forEach { outfit ->
                assertTrue("uses only owned active items", outfit.pieces.all { it.item.id in activeIds })
                assertEquals("no item twice", outfit.pieces.size, outfit.pieces.map { it.item.id }.toSet().size)
                assertTrue(outfit.pieces.count { it.slot == ClothingCategory.FOOTWEAR } <= 1)
                assertTrue(outfit.matchScore in 0..100)
            }
            val dresses = active.filter { it.attributes?.type == GarmentType.DRESS }
            val tops = active.filter { it.item.category == ClothingCategory.TOP && it.attributes?.type != GarmentType.DRESS }
            val bottoms = active.filter { it.item.category == ClothingCategory.BOTTOM }
            val shoes = active.filter { it.item.category == ClothingCategory.FOOTWEAR }
            val canComplete = shoes.isNotEmpty() && (dresses.isNotEmpty() || (tops.isNotEmpty() && bottoms.isNotEmpty()))
            if (canComplete) {
                completable++
                if (result.outfits.first().complete) complete++
                val possible = shoes.any { ok(it, request) } &&
                    (dresses.any { ok(it, request) } || (tops.any { ok(it, request) } && bottoms.any { ok(it, request) }))
                val top = result.outfits.first()
                if (possible) {
                    satisfiable++
                    if (top.unmetConstraints.isEmpty()) satisfied++
                } else if (top.unmetConstraints.isEmpty() && top.pieces.all { p -> p.slot == ClothingCategory.OUTERWEAR || ok(active.first { it.item.id == p.item.id }, request) }.not()) {
                    falseClaims++
                }
            }
            itemsOwned += active.size
            itemsUsed += result.outfits.flatMap { o -> o.pieces.map { it.item.id } }.toSet().size
            if (result.outfits.size > 1) {
                multi++
                val sets = result.outfits.map { o -> o.pieces.map { it.item.id }.toSet() }
                val pairs = sets.flatMapIndexed { i, a -> sets.drop(i + 1).map { b -> 1.0 - (a intersect b).size.toDouble() / (a union b).size } }
                distinctSum += pairs.average()
            }
        }
        println("SYNTHETIC recommender evaluation (seed 20261110, $requests random requests; not user data)")
        println("  complete outfit returned when the wardrobe allows one: $complete / $completable")
        println("  top outfit meets all occasion/weather/rain constraints when some outfit can: $satisfied / $satisfiable")
        println("  outfits claiming no unmet constraint while breaking one: $falseClaims")
        println("  mean share of active wardrobe used across the returned outfits: ${"%.3f".format(itemsUsed.toDouble() / itemsOwned)}")
        println("  mean pairwise Jaccard distance between returned outfits: ${"%.3f".format(distinctSum / multi)} ($multi requests with >1 outfit)")
        assertEquals(completable, complete)
        assertEquals(satisfiable, satisfied)
        assertEquals(0, falseClaims)
    }
}
