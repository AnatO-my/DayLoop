package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarmentDraftTest {
    private fun <T> scored(value: T, confidence: Float) =
        Scored(value, confidence, RecognitionPolicy.band(confidence), EvidenceSource.COMBINED)

    private fun prediction(
        category: Scored<ClothingCategory>? = null, type: Scored<GarmentType>? = null,
        color: Scored<NamedColor>? = null, pattern: Scored<ClothingPattern>? = null,
    ) = GarmentPrediction(null, category, type, emptyList(), color, null, pattern, null, emptySet())

    private val models = listOf(ModelInfo(ShapeModelWeights.MODEL_ID, ShapeModelWeights.MODEL_VERSION, "shape"))

    @Test
    fun `a confident prediction pre-fills name, category, type and colour`() {
        val draft = GarmentDraft.from(prediction(
            scored(ClothingCategory.BOTTOM, 0.9f), scored(GarmentType.JEANS, 0.85f),
            scored(NamedColor(ColorFamily.NAVY, "#1F2A44"), 0.88f), scored(ClothingPattern.SOLID, 0.9f),
        ))
        assertEquals("Navy jeans", draft.name)
        assertEquals(ClothingCategory.BOTTOM, draft.category)
        assertEquals(GarmentType.JEANS, draft.type)
        assertEquals("#1F2A44", draft.colorHex)
        assertEquals(ClothingPattern.SOLID, draft.pattern)
        assertNull(draft.blockingProblem)
    }

    @Test
    fun `an uncertain prediction leaves the user to choose and cannot be saved as is`() {
        val draft = GarmentDraft.from(prediction(color = scored(NamedColor(ColorFamily.RED, "#C8322B"), 0.3f)))
        assertEquals("", draft.name)
        assertNull(draft.category); assertNull(draft.type); assertNull(draft.colorHex)
        assertNotNull(draft.blockingProblem)
    }

    @Test
    fun `material is never pre-filled from a photo`() {
        val p = prediction(scored(ClothingCategory.BOTTOM, 0.9f)).copy(
            materialHint = Scored(com.dayloop.app.core.model.ClothingMaterial.DENIM, 0.79f, ConfidenceBand.TENTATIVE, EvidenceSource.IMAGE_LABELS))
        assertNull(GarmentDraft.from(p).material)
    }

    @Test
    fun `category and type stay consistent while editing`() {
        val jeans = GarmentDraft(name = "x").withType(GarmentType.JEANS)
        assertEquals(ClothingCategory.BOTTOM, jeans.category)
        assertNull(jeans.withCategory(ClothingCategory.TOP).type)
        assertEquals(GarmentType.JEANS, jeans.withCategory(ClothingCategory.BOTTOM).type)
    }

    @Test
    fun `saving trims the name and records whether the user corrected the model`() {
        val p = prediction(scored(ClothingCategory.TOP, 0.7f), scored(GarmentType.SHIRT, 0.7f))
        val kept = GarmentDraft.from(p).copy(name = "  Oxford  ")
        val item = kept.toItem(ClothingItemId("1"))
        assertEquals("Oxford", item.name)
        assertFalse(GarmentDraft.stamp(p, kept, models).userCorrected)
        val corrected = kept.withType(GarmentType.T_SHIRT)
        val stamp = GarmentDraft.stamp(p, corrected, models)
        assertTrue(stamp.userCorrected)
        assertEquals(ShapeModelWeights.MODEL_ID, stamp.modelId)
        assertEquals(0.7f, stamp.typeConfidence)
        assertEquals(GarmentType.T_SHIRT, corrected.toAttributes(stamp).type)
    }

    @Test
    fun `invalid colour blocks saving`() {
        val draft = GarmentDraft(name = "Tee", category = ClothingCategory.TOP, colorHex = "blue")
        assertEquals("Colour must look like #RRGGBB.", draft.blockingProblem)
    }
}
