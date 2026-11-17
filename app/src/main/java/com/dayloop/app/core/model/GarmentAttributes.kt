package com.dayloop.app.core.model

/** The name shown to people for a wardrobe category. */
val ClothingCategory.label: String
    get() = when (this) {
        ClothingCategory.TOP -> "Tops"
        ClothingCategory.BOTTOM -> "Bottoms"
        ClothingCategory.FOOTWEAR -> "Shoes"
        ClothingCategory.HEADWEAR -> "Headwear"
        ClothingCategory.OUTERWEAR -> "Outerwear"
        ClothingCategory.ACCESSORY -> "Accessories"
    }

/**
 * Finer garment kind inside one of the six wardrobe categories.
 *
 * [fullBody] marks one-piece garments (a dress fills the top and bottom slots of an outfit)
 * so the category enum, backups and older rows stay unchanged.
 */
enum class GarmentType(
    val category: ClothingCategory,
    val label: String,
    val fullBody: Boolean = false,
) {
    T_SHIRT(ClothingCategory.TOP, "T-shirt"),
    SHIRT(ClothingCategory.TOP, "Shirt"),
    POLO(ClothingCategory.TOP, "Polo"),
    SWEATER(ClothingCategory.TOP, "Sweater"),
    DRESS(ClothingCategory.TOP, "Dress", fullBody = true),
    TROUSERS(ClothingCategory.BOTTOM, "Trousers"),
    JEANS(ClothingCategory.BOTTOM, "Jeans"),
    SHORTS(ClothingCategory.BOTTOM, "Shorts"),
    SKIRT(ClothingCategory.BOTTOM, "Skirt"),
    LEGGINGS(ClothingCategory.BOTTOM, "Leggings"),
    COAT(ClothingCategory.OUTERWEAR, "Coat"),
    JACKET(ClothingCategory.OUTERWEAR, "Jacket"),
    BLAZER(ClothingCategory.OUTERWEAR, "Blazer"),
    SNEAKERS(ClothingCategory.FOOTWEAR, "Sneakers"),
    SANDALS(ClothingCategory.FOOTWEAR, "Sandals"),
    BOOTS(ClothingCategory.FOOTWEAR, "Boots"),
    SHOES(ClothingCategory.FOOTWEAR, "Shoes"),
    HAT(ClothingCategory.HEADWEAR, "Hat"),
    CAP(ClothingCategory.HEADWEAR, "Cap"),
    BEANIE(ClothingCategory.HEADWEAR, "Beanie"),
    BAG(ClothingCategory.ACCESSORY, "Bag"),
    TIE(ClothingCategory.ACCESSORY, "Tie"),
    SCARF(ClothingCategory.ACCESSORY, "Scarf"),
    EYEWEAR(ClothingCategory.ACCESSORY, "Eyewear"),
    JEWELLERY(ClothingCategory.ACCESSORY, "Jewellery"),
}

/** The photo heuristic only separates SOLID from PATTERNED; finer values are user-entered. */
enum class ClothingPattern(val label: String) {
    SOLID("Solid"),
    PATTERNED("Patterned"),
    STRIPED("Striped"),
    CHECKED("Checked"),
    FLORAL("Floral"),
    PRINT("Print or graphic"),
}

/** Never asserted from a photo alone. A label hint may be offered; the user decides. */
enum class ClothingMaterial(val label: String) {
    COTTON("Cotton"),
    DENIM("Denim"),
    WOOL("Wool"),
    LEATHER("Leather"),
    LINEN("Linen"),
    SILK("Silk"),
    KNIT("Knit"),
    SYNTHETIC("Synthetic"),
    OTHER("Other"),
}

/** User-entered only; cut cannot be judged reliably from one flat photo. */
enum class ClothingFit(val label: String) {
    SLIM("Slim"),
    REGULAR("Regular"),
    LOOSE("Loose"),
}

/**
 * How warm a piece is to wear. Used instead of four calendar seasons, which do not
 * describe every climate.
 */
enum class Warmth(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    WARM("Warm"),
}

enum class Occasion(val label: String) {
    CASUAL("Casual"),
    WORK("Work / smart"),
    FORMAL("Formal"),
    ACTIVE("Active"),
}

/** Which recognition model produced the draft the user reviewed, kept for later model improvement. */
data class RecognitionStamp(
    val modelId: String,
    val modelVersion: String,
    /** Confidence the model gave its type guess, 0..1, or null when it offered none. */
    val typeConfidence: Float?,
    /** True when the user changed the predicted type or category before saving. */
    val userCorrected: Boolean,
)

/**
 * User-approved descriptive details for one clothing item. Every field is optional:
 * "unknown" is a valid, honest state and the recommender treats it as neutral.
 */
data class GarmentAttributes(
    val type: GarmentType? = null,
    val secondaryColorHex: String? = null,
    val pattern: ClothingPattern? = null,
    val material: ClothingMaterial? = null,
    val fit: ClothingFit? = null,
    val warmth: Warmth? = null,
    val occasions: Set<Occasion> = emptySet(),
    val recognition: RecognitionStamp? = null,
) {
    init {
        require(secondaryColorHex == null || HexColor.isValid(secondaryColorHex)) {
            "Secondary colour must be #RRGGBB."
        }
    }

    val isEmpty: Boolean
        get() = type == null && secondaryColorHex == null && pattern == null && material == null &&
            fit == null && warmth == null && occasions.isEmpty() && recognition == null
}

object HexColor {
    private val pattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun isValid(value: String): Boolean = pattern.matches(value)

    fun of(red: Int, green: Int, blue: Int): String =
        "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))

    /** Returns 0xRRGGBB, or null for anything that is not #RRGGBB. */
    fun parse(value: String?): Int? = value?.takeIf(::isValid)?.substring(1)?.toInt(16)
}
