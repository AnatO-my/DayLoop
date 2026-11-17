package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.GarmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionFusionTest {
    private val noColor = ColorReading(null, null, null)

    private fun shape(vararg pairs: Pair<ShapeClass, Float>): FloatArray {
        val p = FloatArray(10)
        pairs.forEach { p[it.first.ordinal] = it.second }
        val rest = (1f - p.sum()) / (10 - pairs.size)
        for (i in p.indices) if (pairs.none { it.first.ordinal == i }) p[i] = rest
        return p
    }

    private fun labels(vararg pairs: Pair<String, Float>) =
        LabelEvidenceMapper.map(pairs.map { ImageLabel(it.first, it.second) })

    @Test
    fun `label map ignores weak labels and unknown text`() {
        val evidence = labels("Jeans" to 0.9f, "Hat" to 0.3f, "Sky" to 0.99f, "Denim" to 0.7f, "Pattern" to 0.6f)
        assertEquals(setOf(GarmentType.JEANS), evidence.types.keys)
        assertEquals(ClothingMaterial.DENIM, evidence.material?.first)
        assertEquals(0.6f, evidence.patterned)
        assertEquals(0f, evidence.personCue)
    }

    @Test
    fun `agreement between sources is confident and uses the finer label type`() {
        val result = PredictionFusion.fuse(shape(ShapeClass.TROUSER to 0.9f), labels("Jeans" to 0.8f), noColor)
        assertEquals(ClothingCategory.BOTTOM, result.category?.value)
        assertEquals(ConfidenceBand.CONFIDENT, result.category?.band)
        assertEquals(GarmentType.JEANS, result.type?.value)
        assertEquals(EvidenceSource.COMBINED, result.type?.source)
        assertFalse(RecognitionNote.SOURCES_DISAGREE in result.notes)
    }

    @Test
    fun `a shape-only guess is never shown as confident`() {
        val result = PredictionFusion.fuse(shape(ShapeClass.SNEAKER to 0.99f), LabelEvidence.NONE, noColor)
        assertEquals(GarmentType.SNEAKERS, result.type?.value)
        assertEquals(ConfidenceBand.TENTATIVE, result.type?.band)
        assertEquals(ConfidenceBand.TENTATIVE, result.category?.band)
    }

    @Test
    fun `a flat distribution is reported as not sure`() {
        val result = PredictionFusion.fuse(FloatArray(10) { 0.1f }, LabelEvidence.NONE, noColor)
        assertNull(result.category)
        assertNull(result.type)
    }

    @Test
    fun `category can be offered while the exact type stays unknown`() {
        // T-shirt, pullover and shirt are all tops: the category is clear, the type is not.
        val p = shape(ShapeClass.T_SHIRT_TOP to 0.34f, ShapeClass.PULLOVER to 0.33f, ShapeClass.SHIRT to 0.31f)
        val result = PredictionFusion.fuse(p, LabelEvidence.NONE, noColor)
        assertEquals(ClothingCategory.TOP, result.category?.value)
        assertNull(result.type)
        assertTrue(result.alternatives.map { it.value }.containsAll(listOf(GarmentType.T_SHIRT, GarmentType.SWEATER, GarmentType.SHIRT)))
    }

    @Test
    fun `disagreement is surfaced and lowers confidence`() {
        val result = PredictionFusion.fuse(shape(ShapeClass.BAG to 0.9f), labels("Hat" to 0.95f), noColor)
        assertTrue(RecognitionNote.SOURCES_DISAGREE in result.notes)
        assertTrue(result.category == null || result.category!!.band != ConfidenceBand.CONFIDENT)
    }

    @Test
    fun `a person in the photo disables the shape model`() {
        val result = PredictionFusion.fuse(shape(ShapeClass.DRESS to 0.97f), labels("Smile" to 0.9f), noColor)
        assertTrue(RecognitionNote.PERSON_IN_PHOTO in result.notes)
        assertNull(result.category)
        assertNull(result.type)
        assertTrue(result.alternatives.isEmpty())
    }

    @Test
    fun `labels alone can identify types outside the shape model vocabulary`() {
        val result = PredictionFusion.fuse(null, labels("Sunglasses" to 0.92f), noColor)
        assertEquals(GarmentType.EYEWEAR, result.type?.value)
        assertEquals(ConfidenceBand.CONFIDENT, result.type?.band)
    }

    @Test
    fun `material is only ever a tentative hint`() {
        val result = PredictionFusion.fuse(null, labels("Leather" to 0.99f), noColor)
        assertEquals(ClothingMaterial.LEATHER, result.materialHint?.value)
        assertEquals(ConfidenceBand.TENTATIVE, result.materialHint?.band)
    }

    @Test
    fun `nothing recognised gives an empty draft`() {
        val result = PredictionFusion.fuse(null, LabelEvidence.NONE, noColor)
        assertNull(result.category); assertNull(result.type); assertNull(result.materialHint)
        assertEquals(GarmentPrediction.SCHEMA_VERSION, result.schemaVersion)
    }
}
