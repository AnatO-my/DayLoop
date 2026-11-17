package com.dayloop.app.core.outfit.styling

import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingFit
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.HexColor
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.Warmth
import com.dayloop.app.core.outfit.recognition.ColorFamily
import com.dayloop.app.core.outfit.recognition.ColorMath
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Explainable, deterministic outfit recommender.
 *
 * It is a rule-based scorer on purpose: DayLoop has no licensed outfit-compatibility data
 * to learn from (docs/wardrobe/MODEL_AND_DATA.md), every score can be traced to a stated
 * guideline, and it runs offline in microseconds. The same request always gives the same
 * answer; variety comes from returning several distinct outfits, not from randomness.
 *
 * Unknown attributes are neutral: they neither help nor hurt, and are never guessed.
 */
object OutfitRecommender {
    private const val SLOT_LIMIT = 10
    private const val ALTERNATIVE_WINDOW = 25

    fun recommend(wardrobe: List<WardrobeEntry>, request: StylingRequest): StylingResult {
        val available = wardrobe
            .filter { !it.item.isArchived && it.item.id !in request.excludedItemIds }
            .sortedBy { it.item.id.value }
            .map { Candidate(it) }
        if (available.isEmpty()) {
            return StylingResult(emptyList(), emptyList(), listOf("Your wardrobe has no active items to style yet."))
        }
        val anchor = request.anchorItemId?.let { id -> available.firstOrNull { it.entry.item.id == id } }
        val notes = mutableListOf<String>()
        if (request.anchorItemId != null && anchor == null) {
            notes += "The item you chose to style is archived or unavailable, so these are general suggestions."
        }

        fun slot(category: ClothingCategory, fullBody: Boolean? = null): List<Candidate> {
            val pool = available.filter { it.category == category && (fullBody == null || it.fullBody == fullBody) }
            if (anchor != null && anchor.category == category && (fullBody == null || anchor.fullBody == fullBody)) return listOf(anchor)
            return pool.sortedWith(compareByDescending<Candidate> { unary(it, request) }.thenBy { it.id }).take(SLOT_LIMIT)
        }
        val anchorIsDress = anchor?.fullBody == true
        val anchorIsSeparate = anchor != null && !anchor.fullBody && anchor.category in setOf(ClothingCategory.TOP, ClothingCategory.BOTTOM)
        val tops = if (anchorIsDress) emptyList() else slot(ClothingCategory.TOP, fullBody = false)
        val bottoms = if (anchorIsDress) emptyList() else slot(ClothingCategory.BOTTOM)
        val dresses = if (anchorIsSeparate) emptyList() else slot(ClothingCategory.TOP, fullBody = true)
        val footwear = slot(ClothingCategory.FOOTWEAR)
        // A layer is only proposed when the request gives a reason for one.
        val layerRelevant = request.weather == Weather.COLD || request.weather == Weather.MILD || request.rainExpected ||
            request.occasion == Occasion.WORK || request.occasion == Occasion.FORMAL
        val outerwear: List<Candidate?> = when {
            anchor?.category == ClothingCategory.OUTERWEAR -> listOf(anchor)
            layerRelevant && request.weather != Weather.HOT -> listOf<Candidate?>(null) + slot(ClothingCategory.OUTERWEAR)
            else -> listOf(null)
        }
        val extra: Candidate? = anchor?.takeIf { it.category == ClothingCategory.HEADWEAR || it.category == ClothingCategory.ACCESSORY }

        val bodies: List<List<Candidate>> = buildList {
            for (dress in dresses) add(listOf(dress))
            when {
                tops.isNotEmpty() && bottoms.isNotEmpty() -> for (t in tops) for (b in bottoms) add(listOf(t, b))
                dresses.isEmpty() && tops.isNotEmpty() -> for (t in tops) add(listOf(t))
                dresses.isEmpty() && bottoms.isNotEmpty() -> for (b in bottoms) add(listOf(b))
            }
        }.ifEmpty { listOf(emptyList()) }
        val feet: List<Candidate?> = footwear.ifEmpty { listOf(null) }

        val scored = buildList {
            for (body in bodies) for (shoe in feet) for (layer in outerwear) {
                val pieces = body + listOfNotNull(layer, shoe, extra)
                if (pieces.isEmpty()) continue
                if (pieces.map { it.entry.item.id }.toSet() in request.rejectedCombinations) continue
                val bodyComplete = body.any { it.fullBody } || body.size == 2
                add(score(pieces, complete = bodyComplete && shoe != null, request = request, hasOuterwearOption = outerwear.size > 1))
            }
        // Stated occasion/weather constraints outrank everything else: an outfit that breaks
        // fewer of them always comes first, however well its colours score.
        }.sortedWith(compareByDescending<ScoredOutfit> { it.complete }.thenBy { it.unmet.size }.thenByDescending { it.raw }.thenBy { it.key })

        val chosen = diversify(scored, request.maxResults, anchor)
        if (chosen.any { it.usedAssumption }) {
            notes += "Some warmth or occasion details were assumed from the garment type because the items do not specify them. Edit an item to correct this."
        }
        if (chosen.isNotEmpty() && chosen.all { it.evidence < 2 }) {
            notes += "Few details are saved for these items, so the suggestions are only loosely informed. Adding colours and types improves them."
        }
        return StylingResult(
            outfits = chosen.map { it.toRecommendation() },
            suggestedAdditions = additions(available, anchor, request, tops, bottoms, dresses, footwear),
            notes = notes,
        )
    }

    // ---------------------------------------------------------------- candidates

    private class Candidate(val entry: WardrobeEntry) {
        val id: String = entry.item.id.value
        val category: ClothingCategory = entry.item.category
        val type: GarmentType? = entry.attributes?.type?.takeIf { it.category == category }
        val fullBody: Boolean = type?.fullBody == true
        val rgb: Int? = HexColor.parse(entry.item.colorHex)
        val family: ColorFamily? = rgb?.let(ColorFamily::of)
        val hue: Float? = rgb?.let { ColorMath.hsl(it)[0] }
        val patterned: Boolean? = entry.attributes?.pattern?.let { it != ClothingPattern.SOLID }
        val statedWarmth: Warmth? = entry.attributes?.warmth
        val warmth: Warmth? = statedWarmth ?: type?.let(GarmentDefaults::warmth)
        val statedOccasions: Set<Occasion> = entry.attributes?.occasions.orEmpty()
        val occasions: Set<Occasion> = statedOccasions.ifEmpty { type?.let(GarmentDefaults::occasions).orEmpty() }
        val assumedWarmth: Boolean get() = statedWarmth == null && warmth != null
        val assumedOccasions: Boolean get() = statedOccasions.isEmpty() && occasions.isNotEmpty()
        val name: String get() = entry.item.name
    }

    private fun wornRecently(candidate: Candidate, request: StylingRequest): Boolean? {
        if (request.avoidWornWithinDays == 0) return null
        val last = candidate.entry.lastWorn ?: return false
        val days = ChronoUnit.DAYS.between(last, request.today)
        return days in 0 until request.avoidWornWithinDays.toLong()
    }

    private fun occasionFit(candidate: Candidate, occasion: Occasion): Float? =
        if (candidate.occasions.isEmpty()) null else if (occasion in candidate.occasions) 1f else -1f

    private fun weatherFit(candidate: Candidate, weather: Weather): Float? {
        val warmth = candidate.warmth ?: return null
        return when (weather) {
            Weather.HOT -> when (warmth) { Warmth.LIGHT -> 1f; Warmth.MEDIUM -> 0f; Warmth.WARM -> -1f }
            Weather.MILD -> when (warmth) { Warmth.LIGHT -> 0.3f; Warmth.MEDIUM -> 1f; Warmth.WARM -> -0.3f }
            Weather.COLD -> when (warmth) { Warmth.LIGHT -> -0.6f; Warmth.MEDIUM -> 0.3f; Warmth.WARM -> 1f }
        }
    }

    /** Per-item score used only to shortlist a slot before combinations are enumerated. */
    private fun unary(candidate: Candidate, request: StylingRequest): Float {
        var total = 0f
        request.occasion?.let { total += 3f * (occasionFit(candidate, it) ?: 0f) }
        request.weather?.let { total += 3f * (weatherFit(candidate, it) ?: 0f) }
        if (wornRecently(candidate, request) == true) total -= 1.5f
        if (candidate.family?.neutral == true) total += 0.25f
        return total
    }

    // ---------------------------------------------------------------- scoring

    private class Component(val weight: Float, val value: Float)

    private class ScoredOutfit(
        val pieces: List<Candidate>,
        val raw: Float,
        val evidence: Int,
        val complete: Boolean,
        val reasons: List<String>,
        val cautions: List<String>,
        val unmet: List<String>,
        val usedAssumption: Boolean,
    ) {
        val key: String = pieces.joinToString("|") { it.id }

        fun toRecommendation() = OutfitRecommendation(
            pieces = pieces.map { OutfitPiece(it.entry.item, it.category) },
            matchScore = (50f + 50f * raw).roundToInt().coerceIn(0, 100),
            reasons = reasons,
            cautions = cautions,
            unmetConstraints = unmet,
            complete = complete,
        )
    }

    private fun score(pieces: List<Candidate>, complete: Boolean, request: StylingRequest, hasOuterwearOption: Boolean): ScoredOutfit {
        val components = mutableListOf<Component>()
        val reasons = mutableListOf<String>()
        val cautions = mutableListOf<String>()
        val unmet = mutableListOf<String>()
        var assumed = false
        val layer = pieces.firstOrNull { it.category == ClothingCategory.OUTERWEAR }

        // Occasion: a requested constraint. Mismatches are reported, never hidden.
        request.occasion?.let { occasion ->
            val fits = pieces.mapNotNull { piece -> occasionFit(piece, occasion)?.let { piece to it } }
            if (fits.isNotEmpty()) {
                components += Component(3f, fits.map { it.second }.average().toFloat())
                val wrong = fits.filter { it.second < 0f }.map { it.first }
                if (wrong.isEmpty()) reasons += "Every piece with known details suits ${occasion.label.lowercase()}."
                else unmet += "${wrong.joinToString { it.name }} ${if (wrong.size == 1) "is" else "are"} not marked for ${occasion.label.lowercase()}."
                assumed = assumed || fits.any { it.first.assumedOccasions }
            }
        } ?: run {
            val known = pieces.filter { it.occasions.isNotEmpty() }
            if (known.size >= 2) {
                val shared = known.map { it.occasions }.reduce { a, b -> a intersect b }
                components += Component(2f, if (shared.isNotEmpty()) 0.6f else -0.6f)
                if (shared.isEmpty()) cautions += "These pieces are usually worn for different occasions."
                assumed = assumed || known.any { it.assumedOccasions }
            }
        }

        // Weather.
        request.weather?.let { weather ->
            val fits = pieces.mapNotNull { piece -> weatherFit(piece, weather)?.let { piece to it } }
            val parts = fits.map { it.second }.toMutableList()
            when (weather) {
                Weather.HOT -> if (layer != null) { parts += -1f; cautions += "An outer layer may be too much for hot weather." }
                Weather.COLD -> if (layer != null) parts += 1f else if (hasOuterwearOption) parts += -0.5f
                Weather.MILD -> Unit
            }
            if (parts.isNotEmpty()) {
                val value = parts.average().toFloat()
                components += Component(3f, value)
                val poor = fits.filter { it.second < 0f }.map { it.first }
                when {
                    poor.isNotEmpty() -> unmet += "${poor.joinToString { it.name }} may not suit ${weather.label.lowercase()} weather."
                    weather == Weather.COLD && layer != null -> reasons += "Includes ${layer.name} as a layer for cold weather."
                    value > 0.4f -> reasons += "The pieces are a sensible weight for ${weather.label.lowercase()} weather."
                }
                assumed = assumed || fits.any { it.first.assumedWarmth }
            }
        }
        if (request.rainExpected) {
            val sandals = pieces.firstOrNull { it.type == GarmentType.SANDALS }
            val value = (if (sandals != null) -1f else 0f) + (if (layer != null) 0.5f else 0f)
            components += Component(1f, value.coerceIn(-1f, 1f))
            if (sandals != null) unmet += "${sandals.name} may not suit rain."
            else if (layer != null) reasons += "${layer.name} gives some cover if it rains."
        }

        // Colour harmony across every pair with a known colour.
        val coloured = pieces.filter { it.family != null }
        if (coloured.size >= 2) {
            val pairs = buildList {
                for (i in coloured.indices) for (j in i + 1 until coloured.size) add(Triple(coloured[i], coloured[j], harmony(coloured[i], coloured[j])))
            }
            var value = pairs.map { it.third.score }.average().toFloat()
            val strong = coloured.filter { it.family?.neutral == false }
                .fold(mutableListOf<ColorFamily>()) { seen, c -> if (seen.none { ColorFamily.related(it, c.family!!) }) seen += c.family!!; seen }
            if (strong.size >= 3) {
                value -= 0.5f
                cautions += "Three or more strong colours together can look busy."
            }
            components += Component(3f, value.coerceIn(-1f, 1f))
            val best = pairs.maxByOrNull { it.third.score }!!
            val worst = pairs.minByOrNull { it.third.score }!!
            if (best.third.score > 0f) reasons += "${best.first.name} with ${best.second.name}: ${best.third.explanation}."
            if (worst.third.score < 0f) cautions += "${worst.first.name} with ${worst.second.name}: ${worst.third.explanation}."
        }

        // Pattern mixing.
        val patternKnown = pieces.filter { it.patterned != null }
        if (patternKnown.isNotEmpty()) {
            val patterned = patternKnown.filter { it.patterned == true }
            when {
                patterned.size >= 2 -> {
                    components += Component(1f, -0.6f)
                    cautions += "${patterned.joinToString(" and ") { it.name }} are both patterned; mixing patterns is harder to balance."
                }
                patterned.size == 1 -> {
                    components += Component(1f, 0.5f)
                    reasons += "${patterned.first().name} is the only patterned piece, so it has room to stand out."
                }
                else -> components += Component(1f, 0.2f)
            }
        }

        // Recent wear.
        if (request.avoidWornWithinDays > 0) {
            val recent = pieces.filter { wornRecently(it, request) == true }
            val tracked = pieces.any { it.entry.lastWorn != null }
            if (recent.isNotEmpty()) {
                components += Component(1.5f, -recent.size.toFloat() / pieces.size)
                cautions += "${recent.joinToString { it.name }} ${if (recent.size == 1) "was" else "were"} worn in the last ${request.avoidWornWithinDays} days."
            } else if (tracked) {
                components += Component(1.5f, 0.3f)
                reasons += "None of these were worn in the last ${request.avoidWornWithinDays} days."
            }
        }

        // Style preference.
        request.style?.let { style ->
            if (coloured.size >= 2) {
                val accents = coloured.count { it.family?.neutral == false }
                val busy = pieces.count { it.patterned == true } >= 2
                val value = when (style) {
                    StylePreference.MINIMAL -> when (accents) { 0 -> 1f; 1 -> 0f; else -> -1f }
                    StylePreference.CLASSIC -> if (accents <= 1 && !busy) 1f else -0.5f
                    StylePreference.BOLD -> if (accents >= 1) 1f else -0.5f
                }
                components += Component(1.5f, value)
                if (value > 0f) reasons += "Fits the ${style.label.lowercase()} style you asked for."
                else if (value < 0f) cautions += "Not a close match for the ${style.label.lowercase()} style you asked for."
            }
        }

        // Comfort, only from details the user saved.
        if (request.comfortFirst) {
            val parts = pieces.mapNotNull { piece ->
                when {
                    piece.entry.attributes?.fit == ClothingFit.SLIM -> -0.5f
                    piece.entry.attributes?.fit != null -> 0.6f
                    piece.type == GarmentType.SNEAKERS || piece.type == GarmentType.SANDALS -> 0.6f
                    else -> null
                }
            }
            if (parts.isNotEmpty()) {
                components += Component(1f, parts.average().toFloat())
                if (parts.average() > 0.3) reasons += "Leans towards relaxed fits and easy footwear."
            }
        }

        if (!complete) cautions += "This is a partial outfit: your wardrobe is missing a piece for at least one core slot."
        val weight = components.sumOf { it.weight.toDouble() }.toFloat()
        val raw = if (weight == 0f) 0f else components.sumOf { (it.weight * it.value).toDouble() }.toFloat() / weight
        return ScoredOutfit(pieces, raw, components.size, complete, reasons.distinct(), cautions.distinct(), unmet.distinct(), assumed)
    }

    private class Harmony(val score: Float, val explanation: String)

    private fun harmony(a: Candidate, b: Candidate): Harmony {
        val fa = a.family!!; val fb = b.family!!
        return when {
            fa.neutral && fb.neutral ->
                Harmony(0.7f, "two neutrals (${fa.label.lowercase()} and ${fb.label.lowercase()}), which most guidelines treat as easy to combine")
            fa.neutral || fb.neutral -> {
                val (neutral, colour) = if (fa.neutral) fa to fb else fb to fa
                Harmony(0.8f, "${neutral.label.lowercase()} is a neutral that lets the ${colour.label.lowercase()} stand out")
            }
            ColorFamily.related(fa, fb) -> Harmony(0.7f, "shades of one colour family, a tonal look")
            else -> {
                val distance = abs(a.hue!! - b.hue!!).let { min(it, 360f - it) }
                when {
                    distance <= 45f -> Harmony(0.5f, "neighbouring colours on the colour wheel")
                    distance in 150f..210f -> Harmony(0.3f, "opposite colours, a deliberate contrast that some find bold")
                    else -> Harmony(-0.5f, "two strong, unrelated colours (${fa.label.lowercase()} and ${fb.label.lowercase()}) that many guidelines advise against pairing")
                }
            }
        }
    }

    // ---------------------------------------------------------------- selection

    private fun diversify(ranked: List<ScoredOutfit>, limit: Int, anchor: Candidate?): List<ScoredOutfit> {
        if (ranked.isEmpty()) return emptyList()
        val chosen = mutableListOf(ranked.first())
        val floor = ranked.first().raw - ALTERNATIVE_WINDOW / 50f
        fun difference(a: ScoredOutfit, b: ScoredOutfit): Int {
            val ids = b.pieces.map { it.id }.toSet()
            return a.pieces.count { it.id != anchor?.id && it.id !in ids } + (b.pieces.size - a.pieces.size).coerceAtLeast(0)
        }
        for (required in listOf(2, 1)) {
            for (candidate in ranked) {
                if (chosen.size >= limit) return chosen
                if (candidate in chosen || candidate.raw < floor || candidate.complete != ranked.first().complete) continue
                if (candidate.unmet.size > ranked.first().unmet.size) continue
                if (chosen.all { difference(candidate, it) >= required }) chosen += candidate
            }
        }
        return chosen
    }

    private fun additions(
        available: List<Candidate>, anchor: Candidate?, request: StylingRequest,
        tops: List<Candidate>, bottoms: List<Candidate>, dresses: List<Candidate>, footwear: List<Candidate>,
    ): List<SuggestedAddition> = buildList {
        val anchorIsDress = anchor?.fullBody == true
        if (footwear.isEmpty()) add(SuggestedAddition(ClothingCategory.FOOTWEAR, "A pair of shoes", "No footwear is saved in your wardrobe, so outfits are incomplete."))
        if (!anchorIsDress && dresses.isEmpty()) {
            if (tops.isEmpty()) add(SuggestedAddition(ClothingCategory.TOP, "A top", "No tops are saved in your wardrobe."))
            if (bottoms.isEmpty()) add(SuggestedAddition(ClothingCategory.BOTTOM, "Trousers, a skirt or shorts", "No bottoms are saved in your wardrobe."))
        }
        if (request.weather == Weather.COLD && available.none { it.category == ClothingCategory.OUTERWEAR || it.warmth == Warmth.WARM }) {
            add(SuggestedAddition(ClothingCategory.OUTERWEAR, "A warm layer such as a coat or sweater", "Nothing saved is marked or typed as warm."))
        }
        request.occasion?.let { occasion ->
            for ((category, pool, words) in listOf(
                Triple(ClothingCategory.TOP, tops + dresses, "top"),
                Triple(ClothingCategory.BOTTOM, bottoms, "bottom"),
                Triple(ClothingCategory.FOOTWEAR, footwear, "pair of shoes"),
            )) {
                val known = pool.filter { it.occasions.isNotEmpty() }
                if (known.isNotEmpty() && known.none { occasion in it.occasions }) {
                    add(SuggestedAddition(category, "A $words suited to ${occasion.label.lowercase()}", "None of your saved ${category.name.lowercase()} items is marked for ${occasion.label.lowercase()}."))
                }
            }
        }
        if (anchor?.family?.neutral == false && anchor.category in setOf(ClothingCategory.TOP, ClothingCategory.BOTTOM) && !anchorIsDress) {
            val (other, pool, words) = if (anchor.category == ClothingCategory.TOP) Triple(ClothingCategory.BOTTOM, bottoms, "bottom") else Triple(ClothingCategory.TOP, tops, "top")
            if (pool.isNotEmpty() && pool.none { it.family?.neutral == true }) {
                add(SuggestedAddition(other, "A neutral $words (black, navy, grey, white or beige)", "A neutral piece would give ${anchor.name} more pairings; none is saved with a neutral colour."))
            }
        }
    }.distinctBy { it.category to it.description }.take(3)
}
