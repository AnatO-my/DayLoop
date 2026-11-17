package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.GarmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {
    private fun <T> scored(value: T, confidence: Float) =
        Scored(value, confidence, RecognitionPolicy.band(confidence), EvidenceSource.COMBINED)

    private fun prediction(category: ClothingCategory?, type: GarmentType?, confidence: Float = 0.9f, color: ColorFamily? = ColorFamily.NAVY) =
        GarmentPrediction(
            NormalizedBox(0.1f, 0.1f, 0.5f, 0.5f),
            category?.let { scored(it, confidence) }, type?.let { scored(it, confidence) }, emptyList(),
            color?.let { scored(NamedColor(it, it.swatchHex), 0.7f) }, null, null, null, emptySet(),
        )

    private val jeans = prediction(ClothingCategory.BOTTOM, GarmentType.JEANS)
    private val unknown = prediction(null, null, color = null)

    @Test
    fun `summary rows put sureness into words and never claim a material`() {
        val rows = ReviewSummary.rows(jeans)
        assertEquals(listOf("Category", "Type", "Colour", "Material"), rows.map { it.label })
        assertEquals(listOf("Bottoms", "Jeans", "Navy", null), rows.map { it.value })
        assertEquals(listOf(Sureness.SURE, Sureness.SURE, Sureness.CHECK, Sureness.CANT_TELL), rows.map { it.sureness })
        assertEquals("Navy jeans?", ReviewSummary.headline(GarmentDraft.from(jeans)))
    }

    @Test
    fun `quick confirm is offered only when the draft could be saved as it stands`() {
        assertTrue(ReviewSummary.quickConfirmAvailable(jeans, GarmentDraft.from(jeans)))
        assertFalse(ReviewSummary.quickConfirmAvailable(unknown, GarmentDraft.from(unknown)))
        assertFalse(ReviewSummary.quickConfirmAvailable(null, GarmentDraft(name = "Tee", category = ClothingCategory.TOP)))
        // Category known but no type means no suggested name, so the full form is needed.
        val categoryOnly = prediction(ClothingCategory.TOP, null)
        assertFalse(ReviewSummary.quickConfirmAvailable(categoryOnly, GarmentDraft.from(categoryOnly)))
    }

    @Test
    fun `confident items start ticked and unsure ones do not`() {
        val session = ReviewSession.from(listOf(jeans, unknown))
        assertEquals(listOf(true, false), session.entries.map { it.included })
        assertEquals(listOf(1), session.toSave.map { it.key })
        assertEquals("Bottoms · sure", session.entries[0].status)
        assertEquals("Needs a name and a check", session.entries[1].status)
        assertEquals("Unknown item", session.entries[1].title)
    }

    @Test
    fun `ticking an incomplete item does not make it saveable and is reported`() {
        val session = ReviewSession.from(listOf(jeans, unknown)).setIncluded(2, true)
        assertEquals(1, session.toSave.size)
        assertEquals(listOf(2), session.tickedButIncomplete.map { it.key })
    }

    @Test
    fun `completing an item in review ticks it and unticking excludes it`() {
        var session = ReviewSession.from(listOf(jeans, unknown))
        session = session.update(2, GarmentDraft(name = "Sneakers", category = ClothingCategory.FOOTWEAR))
        assertEquals(2, session.toSave.size)
        session = session.setIncluded(1, false)
        assertEquals(listOf("Sneakers"), session.toSave.map { it.draft.name })
    }

    @Test
    fun `a drawn region is added as a new entry with a fresh key and can be removed after saving`() {
        var session = ReviewSession.from(listOf(jeans)).add(null, drawnByUser = true)
        assertEquals(listOf(1, 2), session.entries.map { it.key })
        assertTrue(session.entries[1].drawnByUser)
        assertFalse(session.entries[1].included)
        session = session.remove(listOf(1))
        assertEquals(listOf(2), session.entries.map { it.key })
        assertEquals(3, session.add(jeans, false).entries.last().key)
    }

    @Test
    fun `drag becomes a clamped box and tiny drags are ignored`() {
        val box = RegionMath.fromDrag(300f, 50f, -20f, 250f, 400, 500)!!
        assertEquals(NormalizedBox(0f, 0.1f, 0.75f, 0.5f), box)
        assertNull(RegionMath.fromDrag(10f, 10f, 20f, 300f, 400, 500))
        assertNull(RegionMath.fromDrag(0f, 0f, 100f, 100f, 0, 0))
    }

    @Test
    fun `a tap picks the smallest region under it`() {
        val regions = listOf(
            "big" to NormalizedBox(0f, 0f, 1f, 1f), "small" to NormalizedBox(0.4f, 0.4f, 0.6f, 0.6f), "none" to null,
        )
        assertEquals("small", RegionMath.hit(regions, 0.5f, 0.5f))
        assertEquals("big", RegionMath.hit(regions, 0.1f, 0.1f))
        assertNull(RegionMath.hit(listOf("x" to NormalizedBox(0f, 0f, 0.2f, 0.2f)), 0.9f, 0.9f))
    }
}
