package com.dayloop.app.core.outfit.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class ShapeClassifierTest {
    private val weights = WardrobeTestFiles.shapeModel().inputStream().use(ShapeModelWeights::read)
    private val classifier = ShapeClassifier(weights)

    /** 200 fixed Fashion-MNIST test images with logits exported by the training script. */
    private class Fixture(val inputs: List<FloatArray>, val logits: List<FloatArray>, val labels: IntArray)

    private fun fixture(): Fixture {
        val buffer = ByteBuffer.wrap(WardrobeTestFiles.resource("wardrobe/shape_parity_v1.bin")).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(4).also { buffer.get(it) }
        assertEquals("DLPF", String(magic, Charsets.US_ASCII))
        val count = buffer.int
        val inputs = List(count) { FloatArray(784) { (buffer.get().toInt() and 0xFF) / 255f } }
        val logits = List(count) { FloatArray(10) { buffer.float } }
        val labels = IntArray(count) { buffer.get().toInt() and 0xFF }
        return Fixture(inputs, logits, labels)
    }

    @Test
    fun `kotlin forward pass reproduces the training framework logits`() {
        val fixture = fixture()
        var worst = 0f
        fixture.inputs.forEachIndexed { index, input ->
            val actual = classifier.logits(input)
            for (k in 0 until 10) worst = maxOf(worst, abs(actual[k] - fixture.logits[index][k]))
        }
        assertTrue("largest logit difference was $worst", worst < 1e-3f)
    }

    @Test
    fun `fixture accuracy matches the value recorded at export`() {
        val fixture = fixture()
        val correct = fixture.inputs.indices.count { index ->
            val p = classifier.probabilities(fixture.inputs[index])
            p.indices.maxByOrNull { p[it] } == fixture.labels[index]
        }
        // eval_report.json: parity_fixture_accuracy = 0.92 on these 200 images.
        assertEquals(184, correct)
    }

    @Test
    fun `probabilities are a distribution`() {
        val p = classifier.probabilities(fixture().inputs.first())
        assertEquals(1f, p.sum(), 1e-4f)
        assertTrue(p.all { it in 0f..1f })
    }

    @Test
    fun `corrupt or truncated model files are rejected`() {
        val bytes = WardrobeTestFiles.shapeModel().readBytes()
        assertThrows(IllegalArgumentException::class.java) { ShapeModelWeights.read(bytes.copyOf(bytes.size - 5).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { ShapeModelWeights.read((bytes + 1).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { ShapeModelWeights.read(bytes.copyOf().also { it[0] = 'X'.code.toByte() }.inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { ShapeModelWeights.read(ByteArray(0).inputStream()) }
    }

    @Test
    fun `input builder scales the longer edge to the frame and centres the garment`() {
        // A 40 wide x 80 tall block inside a 100x100 photo.
        val mask = FloatArray(100 * 100) { i -> if ((i % 100) in 30 until 70 && (i / 100) in 10 until 90) 1f else 0f }
        val input = assertNotNullValue(ShapeInputBuilder.build(mask, null, 100, 100))
        val filledRows = (0 until 28).count { y -> (0 until 28).any { x -> input[y * 28 + x] > 0.5f } }
        val filledColumns = (0 until 28).count { x -> (0 until 28).any { y -> input[y * 28 + x] > 0.5f } }
        assertEquals(28, filledRows)
        assertEquals(14, filledColumns)
        assertEquals(0f, input[0], 0f)
        assertEquals(1f, input[14 * 28 + 14], 1e-6f)
    }

    @Test
    fun `texture keeps values between the silhouette floor and one`() {
        val mask = FloatArray(64 * 64) { 1f }
        val luma = FloatArray(64 * 64) { i -> (i % 64) / 63f }
        val input = assertNotNullValue(ShapeInputBuilder.build(mask, luma, 64, 64))
        assertTrue(input.all { it in (ShapeInputBuilder.SILHOUETTE_FLOOR - 1e-4f)..1.0001f })
        assertTrue("dark side should be brighter, as in the training data", input[0] > input[27])
    }

    @Test
    fun `empty or tiny masks produce no input`() {
        assertNull(ShapeInputBuilder.build(FloatArray(50 * 50), null, 50, 50))
        val speck = FloatArray(50 * 50).also { it[25 * 50 + 25] = 1f }
        assertNull(ShapeInputBuilder.build(speck, null, 50, 50))
    }

    private fun assertNotNullValue(value: FloatArray?): FloatArray {
        assertNotNull(value)
        return value!!
    }
}
