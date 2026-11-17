package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorAnalyzerTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `names common wardrobe colours`() {
        mapOf(
            rgb(20, 20, 22) to ColorFamily.BLACK, rgb(250, 250, 250) to ColorFamily.WHITE,
            rgb(128, 128, 130) to ColorFamily.GREY, rgb(25, 35, 80) to ColorFamily.NAVY,
            rgb(40, 100, 200) to ColorFamily.BLUE, rgb(200, 40, 40) to ColorFamily.RED,
            rgb(215, 195, 165) to ColorFamily.BEIGE, rgb(100, 65, 35) to ColorFamily.BROWN,
            rgb(40, 140, 80) to ColorFamily.GREEN, rgb(235, 200, 40) to ColorFamily.YELLOW,
            rgb(240, 160, 190) to ColorFamily.PINK, rgb(110, 30, 45) to ColorFamily.BURGUNDY,
        ).forEach { (color, family) -> assertEquals(family, ColorFamily.of(color and 0xFFFFFF)) }
    }

    @Test
    fun `a shaded single-colour item reads as solid`() {
        // Navy cloth with folds: lightness varies, hue does not.
        val pixels = IntArray(10_000) { i -> val shade = (i % 7) * 6; rgb(22 + shade / 2, 32 + shade / 2, 78 + shade) }
        val reading = ColorAnalyzer.analyze(pixels)
        assertEquals(ColorFamily.NAVY, reading.primary?.value?.family)
        assertEquals(ClothingPattern.SOLID, reading.pattern?.value)
        assertNull(reading.secondary)
        assertTrue("colour confidence is capped below certainty", reading.primary!!.confidence <= 0.9f)
    }

    @Test
    fun `two unrelated colours give a tentative patterned reading and a second colour`() {
        val pixels = IntArray(10_000) { i -> if ((i / 100) % 10 < 6) rgb(200, 40, 40) else rgb(250, 250, 250) }
        val reading = ColorAnalyzer.analyze(pixels)
        assertEquals(ColorFamily.RED, reading.primary?.value?.family)
        assertEquals(ColorFamily.WHITE, reading.secondary?.value?.family)
        assertEquals(ClothingPattern.PATTERNED, reading.pattern?.value)
        assertEquals(ConfidenceBand.TENTATIVE, reading.pattern?.band)
    }

    @Test
    fun `background outside the mask does not tint the result`() {
        val pixels = IntArray(10_000) { i -> if (i % 100 < 30) rgb(40, 140, 80) else rgb(255, 255, 255) }
        val mask = FloatArray(10_000) { i -> if (i % 100 < 30) 1f else 0f }
        val reading = ColorAnalyzer.analyze(pixels, mask)
        assertEquals(ColorFamily.GREEN, reading.primary?.value?.family)
        assertEquals(ClothingPattern.SOLID, reading.pattern?.value)
    }

    @Test
    fun `too few garment pixels yields no reading rather than a guess`() {
        val reading = ColorAnalyzer.analyze(IntArray(10_000) { rgb(200, 40, 40) }, FloatArray(10_000) { i -> if (i < 20) 1f else 0f })
        assertNull(reading.primary)
        assertNull(reading.pattern)
    }

    @Test
    fun `representative hex is the mean of the dominant family`() {
        val reading = ColorAnalyzer.analyze(IntArray(1_000) { rgb(200, 40, 40) })
        assertNotNull(reading.primary)
        assertEquals("#C82828", reading.primary!!.value.hex)
    }
}
