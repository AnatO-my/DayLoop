package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.HexColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Colour names coarse enough to survive lighting changes. [neutral] colours pair with anything. */
enum class ColorFamily(val label: String, val neutral: Boolean, val swatchHex: String) {
    BLACK("Black", true, "#1A1A1A"),
    WHITE("White", true, "#F5F5F5"),
    GREY("Grey", true, "#8A8A8A"),
    BEIGE("Beige", true, "#D8C3A5"),
    BROWN("Brown", true, "#6B4423"),
    NAVY("Navy", true, "#1F2A44"),
    BLUE("Blue", false, "#2F6FD0"),
    LIGHT_BLUE("Light blue", false, "#A9CCEF"),
    TEAL("Teal", false, "#1F8A8A"),
    GREEN("Green", false, "#2E8B57"),
    OLIVE("Olive", false, "#6B6B2E"),
    YELLOW("Yellow", false, "#E8C52A"),
    ORANGE("Orange", false, "#E07B28"),
    RED("Red", false, "#C8322B"),
    BURGUNDY("Burgundy", false, "#6E1F2B"),
    PINK("Pink", false, "#E79AB5"),
    PURPLE("Purple", false, "#7A4BA8");

    companion object {
        /** Families that are usually the same cloth under different light, not a second colour. */
        private val relatedPairs: Set<Set<ColorFamily>> = setOf(
            setOf(WHITE, GREY), setOf(GREY, BLACK), setOf(BLUE, NAVY), setOf(BLUE, LIGHT_BLUE),
            setOf(RED, BURGUNDY), setOf(RED, PINK), setOf(BROWN, BEIGE), setOf(BROWN, ORANGE),
            setOf(GREEN, OLIVE), setOf(NAVY, BLACK), setOf(BEIGE, WHITE), setOf(TEAL, GREEN),
            setOf(PURPLE, BURGUNDY), setOf(YELLOW, OLIVE), setOf(LIGHT_BLUE, WHITE), setOf(LIGHT_BLUE, GREY),
        )

        fun related(a: ColorFamily, b: ColorFamily): Boolean = a == b || setOf(a, b) in relatedPairs

        /** Classifies 0xRRGGBB. */
        fun of(rgb: Int): ColorFamily {
            val hsl = ColorMath.hsl(rgb)
            val h = hsl[0]; val s = hsl[1]; val l = hsl[2]
            return when {
                l < 0.12f -> BLACK
                s < 0.12f -> when { l > 0.82f -> WHITE; l < 0.24f -> BLACK; else -> GREY }
                l > 0.93f -> WHITE
                s < 0.22f && l < 0.18f -> BLACK
                h < 15f || h >= 345f -> when { l < 0.30f -> BURGUNDY; l > 0.74f -> PINK; else -> RED }
                h < 45f -> when {
                    l < 0.42f -> BROWN
                    s < 0.55f && l > 0.58f -> BEIGE
                    else -> ORANGE
                }
                h < 70f -> when {
                    s < 0.45f && l > 0.62f -> BEIGE
                    l < 0.40f -> OLIVE
                    else -> YELLOW
                }
                h < 165f -> if (h < 100f && s < 0.55f && l < 0.50f) OLIVE else GREEN
                h < 195f -> TEAL
                h < 255f -> when { l < 0.28f -> NAVY; l > 0.74f -> LIGHT_BLUE; else -> BLUE }
                h < 290f -> PURPLE
                else -> if (l < 0.30f) PURPLE else PINK
            }
        }

        fun ofHex(hex: String?): ColorFamily? = HexColor.parse(hex)?.let(::of)
    }
}

object ColorMath {
    /** Returns hue 0..360, saturation 0..1, lightness 0..1 for 0xRRGGBB. */
    fun hsl(rgb: Int): FloatArray {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val hi = max(r, max(g, b)); val lo = min(r, min(g, b))
        val l = (hi + lo) / 2f
        val delta = hi - lo
        if (delta < 1e-6f) return floatArrayOf(0f, 0f, l)
        val s = delta / (1f - abs(2f * l - 1f)).coerceAtLeast(1e-6f)
        val h = when (hi) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return floatArrayOf(if (h < 0f) h + 360f else h, s.coerceIn(0f, 1f), l)
    }

    /** Perceptual brightness 0..1 (Rec. 601). */
    fun luma(rgb: Int): Float =
        (0.299f * ((rgb shr 16) and 0xFF) + 0.587f * ((rgb shr 8) and 0xFF) + 0.114f * (rgb and 0xFF)) / 255f
}

data class ColorReading(
    val primary: Scored<NamedColor>?,
    val secondary: Scored<NamedColor>?,
    val pattern: Scored<ClothingPattern>?,
)

/**
 * Deterministic colour summary of the garment's own pixels. No model is involved, so the
 * result is explainable: "78% of the item's pixels are navy".
 */
object ColorAnalyzer {
    private const val MAX_SAMPLES = 20_000
    private const val MIN_SAMPLES = 64
    private const val INTERIOR = 0.8f
    private const val SOLID_SHARE = 0.85f
    private const val SECOND_COLOR_SHARE = 0.18f

    /**
     * @param pixels ARGB or RGB pixels, row-major
     * @param mask optional foreground confidence per pixel; pixels under 0.8 are skipped so
     *   soft edges and background do not tint the result
     */
    fun analyze(pixels: IntArray, mask: FloatArray? = null): ColorReading {
        require(mask == null || mask.size == pixels.size) { "Mask size mismatch." }
        val stride = max(1, pixels.size / MAX_SAMPLES)
        val families = ColorFamily.entries
        val counts = IntArray(families.size)
        val red = LongArray(families.size); val green = LongArray(families.size); val blue = LongArray(families.size)
        var total = 0
        var index = 0
        while (index < pixels.size) {
            if (mask == null || mask[index] >= INTERIOR) {
                val rgb = pixels[index] and 0xFFFFFF
                val family = ColorFamily.of(rgb).ordinal
                counts[family]++
                red[family] += (rgb shr 16) and 0xFF
                green[family] += (rgb shr 8) and 0xFF
                blue[family] += rgb and 0xFF
                total++
            }
            index += stride
        }
        if (total < MIN_SAMPLES) return ColorReading(null, null, null)

        val ranked = families.filter { counts[it.ordinal] > 0 }.sortedByDescending { counts[it.ordinal] }
        val dominant = ranked.first()
        fun share(family: ColorFamily) = counts[family.ordinal].toFloat() / total
        fun named(family: ColorFamily): NamedColor {
            val n = counts[family.ordinal]
            return NamedColor(
                family,
                HexColor.of((red[family.ordinal] / n).toInt(), (green[family.ordinal] / n).toInt(), (blue[family.ordinal] / n).toInt()),
            )
        }
        val solidShare = ranked.filter { ColorFamily.related(dominant, it) }.sumOf { share(it).toDouble() }.toFloat()
        val second = ranked.firstOrNull { !ColorFamily.related(dominant, it) && share(it) >= SECOND_COLOR_SHARE }

        // Lighting and white balance shift colour, so even a uniform item is capped below certainty.
        val primaryConfidence = min(0.9f, solidShare)
        val primary = Scored(named(dominant), primaryConfidence, RecognitionPolicy.band(primaryConfidence), EvidenceSource.COLOR_ANALYSIS)
        val secondary = second?.let {
            val confidence = min(0.75f, 0.4f + share(it))
            Scored(named(it), confidence, RecognitionPolicy.band(confidence), EvidenceSource.COLOR_ANALYSIS)
        }
        val pattern = when {
            solidShare >= SOLID_SHARE ->
                Scored(ClothingPattern.SOLID, min(0.9f, solidShare), RecognitionPolicy.band(min(0.9f, solidShare)), EvidenceSource.COLOR_ANALYSIS)
            // Two unrelated colours could be a pattern, colour blocking or leaked background: never CONFIDENT.
            second != null ->
                Scored(ClothingPattern.PATTERNED, min(0.75f, 0.45f + share(second)), ConfidenceBand.TENTATIVE, EvidenceSource.COLOR_ANALYSIS)
            else -> null
        }
        return ColorReading(primary, secondary, pattern)
    }
}
