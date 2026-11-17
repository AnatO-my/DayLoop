package com.dayloop.app.core.outfit.recognition

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.GarmentType
import java.util.Locale
import kotlin.math.max

/** One generic image label, e.g. from ML Kit's bundled base model. */
data class ImageLabel(val text: String, val confidence: Float)

data class LabelEvidence(
    val types: Map<GarmentType, Float>,
    /** Includes categories implied by [types] and category-only labels such as "Outerwear". */
    val categories: Map<ClothingCategory, Float>,
    val material: Pair<ClothingMaterial, Float>?,
    val patterned: Float?,
    /** Strongest sign that a person is in the frame, 0 when none. */
    val personCue: Float,
) {
    companion object {
        val NONE = LabelEvidence(emptyMap(), emptyMap(), null, null, 0f)
    }
}

/**
 * Maps ML Kit base-model label text to wardrobe concepts. The table covers every
 * clothing-related entry in the published label map (see docs/wardrobe/MODEL_AND_DATA.md);
 * labels that have no honest wardrobe equivalent (e.g. "Sari", "Wetsuit") are left out
 * rather than forced into the nearest type.
 */
object LabelEvidenceMapper {
    const val MODEL_ID = "mlkit-image-labeling-base"

    private val typeLabels: Map<String, GarmentType> = mapOf(
        "dress" to GarmentType.DRESS, "gown" to GarmentType.DRESS,
        "shorts" to GarmentType.SHORTS, "jeans" to GarmentType.JEANS,
        "leggings" to GarmentType.LEGGINGS, "tights" to GarmentType.LEGGINGS,
        "jacket" to GarmentType.JACKET, "blazer" to GarmentType.BLAZER, "tuxedo" to GarmentType.BLAZER,
        "polo" to GarmentType.POLO, "jersey" to GarmentType.T_SHIRT,
        "shoe" to GarmentType.SHOES, "sneakers" to GarmentType.SNEAKERS,
        "hat" to GarmentType.HAT, "cap" to GarmentType.CAP, "beanie" to GarmentType.BEANIE,
        "tie" to GarmentType.TIE, "scarf" to GarmentType.SCARF,
        "sunglasses" to GarmentType.EYEWEAR, "glasses" to GarmentType.EYEWEAR, "goggles" to GarmentType.EYEWEAR,
        "bag" to GarmentType.BAG, "handbag" to GarmentType.BAG,
        "necklace" to GarmentType.JEWELLERY, "bracelet" to GarmentType.JEWELLERY,
        "bangle" to GarmentType.JEWELLERY, "jewellery" to GarmentType.JEWELLERY,
    )
    private val categoryLabels: Map<String, ClothingCategory> = mapOf(
        "outerwear" to ClothingCategory.OUTERWEAR,
        "helmet" to ClothingCategory.HEADWEAR,
    )
    private val materialLabels: Map<String, ClothingMaterial> = mapOf(
        "cotton" to ClothingMaterial.COTTON, "wool" to ClothingMaterial.WOOL,
        "denim" to ClothingMaterial.DENIM, "leather" to ClothingMaterial.LEATHER,
    )
    private val personLabels: Set<String> = setOf(
        "selfie", "smile", "skin", "flesh", "hair", "bangs", "beard", "moustache", "person", "dude",
        "baby", "bride", "groom", "laugh", "eyelash", "ear", "foot", "hand", "mouth", "toe", "muscle",
        "sitting", "standing",
    )

    fun map(labels: List<ImageLabel>): LabelEvidence {
        val types = mutableMapOf<GarmentType, Float>()
        val categories = mutableMapOf<ClothingCategory, Float>()
        var material: Pair<ClothingMaterial, Float>? = null
        var patterned: Float? = null
        var person = 0f
        for (label in labels) {
            val confidence = label.confidence
            if (!confidence.isFinite() || confidence <= 0f) continue
            val key = label.text.trim().lowercase(Locale.US)
            if (key in personLabels) person = max(person, confidence)
            if (confidence < RecognitionPolicy.LABEL_FLOOR) continue
            typeLabels[key]?.let { type ->
                types[type] = max(types[type] ?: 0f, confidence)
                categories[type.category] = max(categories[type.category] ?: 0f, confidence)
            }
            categoryLabels[key]?.let { categories[it] = max(categories[it] ?: 0f, confidence) }
            materialLabels[key]?.let { found ->
                if (material == null || confidence > material!!.second) material = found to confidence
            }
            if (key == "pattern") patterned = max(patterned ?: 0f, confidence)
        }
        return LabelEvidence(types, categories, material, patterned, person.coerceAtMost(1f))
    }
}
