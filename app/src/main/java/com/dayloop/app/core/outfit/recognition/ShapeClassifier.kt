package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.GarmentType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The ten Fashion-MNIST classes, in training order, with the wardrobe type each maps to. */
enum class ShapeClass(val garmentType: GarmentType) {
    T_SHIRT_TOP(GarmentType.T_SHIRT),
    TROUSER(GarmentType.TROUSERS),
    PULLOVER(GarmentType.SWEATER),
    DRESS(GarmentType.DRESS),
    COAT(GarmentType.COAT),
    SANDAL(GarmentType.SANDALS),
    SHIRT(GarmentType.SHIRT),
    SNEAKER(GarmentType.SNEAKERS),
    BAG(GarmentType.BAG),
    ANKLE_BOOT(GarmentType.BOOTS),
}

/**
 * Weights for the 106k-parameter garment-shape CNN (conv3x3x16, pool, conv3x3x32, pool,
 * dense 64, dense 10). File layout, little-endian: "DLGS", int version, float temperature,
 * int tensorCount, then per tensor: int rank, int dims[rank], float data.
 */
class ShapeModelWeights private constructor(
    val temperature: Float,
    internal val conv1: FloatArray, internal val conv1Bias: FloatArray,
    internal val conv2: FloatArray, internal val conv2Bias: FloatArray,
    internal val fc1: FloatArray, internal val fc1Bias: FloatArray,
    internal val fc2: FloatArray, internal val fc2Bias: FloatArray,
) {
    companion object {
        const val MODEL_ID = "dayloop-garment-shape"
        const val MODEL_VERSION = "1.0.0"
        const val ASSET_PATH = "wardrobe/garment_shape_v1.bin"
        private const val MAX_BYTES = 2 * 1024 * 1024
        private val shapes = listOf(
            intArrayOf(16, 1, 3, 3), intArrayOf(16),
            intArrayOf(32, 16, 3, 3), intArrayOf(32),
            intArrayOf(64, 32 * 7 * 7), intArrayOf(64),
            intArrayOf(10, 64), intArrayOf(10),
        )

        /** Reads and validates the whole file; any deviation throws [IllegalArgumentException]. */
        fun read(stream: InputStream): ShapeModelWeights {
            // InputStream.readNBytes needs API 33; DayLoop supports API 26.
            val collected = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            while (true) {
                val read = stream.read(chunk)
                if (read < 0) break
                collected.write(chunk, 0, read)
                require(collected.size() <= MAX_BYTES) { "Shape model file is larger than expected." }
            }
            val bytes = collected.toByteArray()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            try {
                val magic = ByteArray(4).also { buffer.get(it) }
                require(String(magic, Charsets.US_ASCII) == "DLGS") { "Not a DayLoop shape model." }
                require(buffer.int == 1) { "Unsupported shape model format." }
                val temperature = buffer.float
                require(temperature.isFinite() && temperature > 0f) { "Invalid temperature." }
                require(buffer.int == shapes.size) { "Unexpected tensor count." }
                val tensors = shapes.map { expected ->
                    require(buffer.int == expected.size) { "Unexpected tensor rank." }
                    expected.forEach { require(buffer.int == it) { "Unexpected tensor shape." } }
                    FloatArray(expected.fold(1) { a, b -> a * b }).also { values ->
                        buffer.asFloatBuffer().get(values)
                        buffer.position(buffer.position() + values.size * 4)
                        require(values.all { it.isFinite() }) { "Model contains non-finite weights." }
                    }
                }
                require(!buffer.hasRemaining()) { "Trailing bytes after model." }
                return ShapeModelWeights(
                    temperature, tensors[0], tensors[1], tensors[2], tensors[3],
                    tensors[4], tensors[5], tensors[6], tensors[7],
                )
            } catch (underflow: java.nio.BufferUnderflowException) {
                throw IllegalArgumentException("Shape model file is truncated.", underflow)
            }
        }
    }
}

/**
 * Plain-Kotlin forward pass: no native runtime, about 1.1M multiply-adds per image.
 * Instances are immutable and safe to share; each call allocates its own scratch buffers.
 */
class ShapeClassifier(private val weights: ShapeModelWeights) {

    /** [input] is 28x28 row-major in 0..1, as produced by [ShapeInputBuilder]. */
    fun logits(input: FloatArray): FloatArray {
        require(input.size == SIDE * SIDE) { "Shape input must be 28x28." }
        val a = conv3x3Relu(input, 1, SIDE, weights.conv1, weights.conv1Bias, 16)
        val b = maxPool2(a, 16, SIDE)
        val c = conv3x3Relu(b, 16, SIDE / 2, weights.conv2, weights.conv2Bias, 32)
        val d = maxPool2(c, 32, SIDE / 2)
        val e = dense(d, weights.fc1, weights.fc1Bias, 64, relu = true)
        return dense(e, weights.fc2, weights.fc2Bias, 10, relu = false)
    }

    /** Temperature-scaled class probabilities indexed by [ShapeClass.ordinal]. */
    fun probabilities(input: FloatArray): FloatArray {
        val scaled = logits(input)
        var peak = Float.NEGATIVE_INFINITY
        for (i in scaled.indices) {
            scaled[i] = scaled[i] / weights.temperature
            peak = max(peak, scaled[i])
        }
        var sum = 0f
        for (i in scaled.indices) {
            scaled[i] = exp(scaled[i] - peak)
            sum += scaled[i]
        }
        for (i in scaled.indices) scaled[i] /= sum
        return scaled
    }

    private fun conv3x3Relu(
        input: FloatArray, inChannels: Int, side: Int,
        kernel: FloatArray, bias: FloatArray, outChannels: Int,
    ): FloatArray {
        val plane = side * side
        val out = FloatArray(outChannels * plane)
        for (o in 0 until outChannels) {
            val outBase = o * plane
            for (y in 0 until side) for (x in 0 until side) {
                var acc = bias[o]
                for (i in 0 until inChannels) {
                    val inBase = i * plane
                    val kBase = (o * inChannels + i) * 9
                    for (ky in 0..2) {
                        val yy = y + ky - 1
                        if (yy < 0 || yy >= side) continue
                        for (kx in 0..2) {
                            val xx = x + kx - 1
                            if (xx < 0 || xx >= side) continue
                            acc += input[inBase + yy * side + xx] * kernel[kBase + ky * 3 + kx]
                        }
                    }
                }
                out[outBase + y * side + x] = if (acc > 0f) acc else 0f
            }
        }
        return out
    }

    private fun maxPool2(input: FloatArray, channels: Int, side: Int): FloatArray {
        val half = side / 2
        val out = FloatArray(channels * half * half)
        for (c in 0 until channels) for (y in 0 until half) for (x in 0 until half) {
            val base = c * side * side + (2 * y) * side + 2 * x
            out[c * half * half + y * half + x] = max(
                max(input[base], input[base + 1]),
                max(input[base + side], input[base + side + 1]),
            )
        }
        return out
    }

    private fun dense(input: FloatArray, weight: FloatArray, bias: FloatArray, outSize: Int, relu: Boolean): FloatArray {
        val inSize = input.size
        val out = FloatArray(outSize)
        for (o in 0 until outSize) {
            var acc = bias[o]
            val base = o * inSize
            for (i in 0 until inSize) acc += input[i] * weight[base + i]
            out[o] = if (relu && acc < 0f) 0f else acc
        }
        return out
    }

    companion object {
        const val SIDE = 28
    }
}

/**
 * Turns a segmented garment into the 28x28 image the classifier was trained on: the
 * silhouette scaled so its longer edge fills the frame, centred, with a little interior
 * texture. Must stay in step with `lift(..., DEVICE_LIFT)` in tools/wardrobe-model.
 */
object ShapeInputBuilder {
    const val SILHOUETTE_FLOOR = 0.6f
    private const val MASK_THRESHOLD = 0.5f

    /**
     * @param mask per-pixel foreground confidence 0..1, row-major, [width] x [height]
     * @param luma per-pixel brightness 0..1 of the same size, or null for a pure silhouette
     * @return the 28x28 input, or null when the mask holds no usable garment
     */
    fun build(mask: FloatArray, luma: FloatArray?, width: Int, height: Int): FloatArray? {
        require(width > 0 && height > 0 && mask.size == width * height) { "Mask size mismatch." }
        require(luma == null || luma.size == mask.size) { "Luma size mismatch." }
        var left = width; var top = height; var right = -1; var bottom = -1
        var lo = 1f; var hi = 0f
        for (y in 0 until height) for (x in 0 until width) {
            val index = y * width + x
            if (mask[index] < MASK_THRESHOLD) continue
            left = min(left, x); right = max(right, x); top = min(top, y); bottom = max(bottom, y)
            if (luma != null) { lo = min(lo, luma[index]); hi = max(hi, luma[index]) }
        }
        if (right < left || bottom < top) return null
        val boxWidth = right - left + 1
        val boxHeight = bottom - top + 1
        if (boxWidth < 8 || boxHeight < 8) return null

        val side = ShapeClassifier.SIDE
        val scale = side.toFloat() / max(boxWidth, boxHeight)
        val outWidth = max(1, (boxWidth * scale).roundToInt()).coerceAtMost(side)
        val outHeight = max(1, (boxHeight * scale).roundToInt()).coerceAtMost(side)
        val offsetX = (side - outWidth) / 2
        val offsetY = (side - outHeight) / 2
        val range = if (hi - lo > 0.05f) hi - lo else 0f
        val out = FloatArray(side * side)
        // Box-filter each output cell over the source pixels it covers.
        for (oy in 0 until outHeight) {
            val y0 = top + oy * boxHeight / outHeight
            val y1 = max(y0 + 1, top + (oy + 1) * boxHeight / outHeight)
            for (ox in 0 until outWidth) {
                val x0 = left + ox * boxWidth / outWidth
                val x1 = max(x0 + 1, left + (ox + 1) * boxWidth / outWidth)
                var sum = 0f
                for (y in y0 until y1) for (x in x0 until x1) {
                    val index = y * width + x
                    val inside = if (mask[index] >= MASK_THRESHOLD) 1f else 0f
                    // Fashion-MNIST stores inverted brightness: dark garments are bright.
                    val texture = if (luma != null && range > 0f) 1f - (luma[index] - lo) / range else 1f
                    sum += inside * (SILHOUETTE_FLOOR + (1f - SILHOUETTE_FLOOR) * texture)
                }
                out[(offsetY + oy) * side + offsetX + ox] = sum / ((y1 - y0) * (x1 - x0))
            }
        }
        return out
    }
}
