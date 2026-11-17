package com.dayloop.app.core.outfit.styling

import com.dayloop.app.core.model.ClothingItemId
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FeedbackVerdict { WORN, NOT_FOR_ME }

/** Why a suggestion was turned down. Stored on the device only. */
enum class FeedbackReason(val label: String) {
    COLOURS("The colours don't work for me"),
    TOO_FORMAL("Too dressed up"),
    TOO_CASUAL("Too casual"),
    WEATHER("Wrong for the weather"),
    WORN_RECENTLY("I wore part of this recently"),
    ONE_PIECE("I don't want one of these pieces today"),
    OTHER("Something else"),
}

/** What the user did with one suggested outfit. */
data class OutfitFeedback(
    val itemIds: Set<ClothingItemId>,
    val verdict: FeedbackVerdict,
    val reason: FeedbackReason?,
    val date: LocalDate,
)

object OutfitFeedbackPolicy {
    /** A turned-down combination stays hidden this long; tastes and seasons change. */
    const val REJECTION_DAYS = 30L

    /**
     * Combinations to keep out of suggestions on [today]. "Wrong for the weather", "worn
     * recently" and "not this piece today" describe the day, not the outfit, so they only
     * hide it on the day they were given.
     */
    fun rejectedCombinations(feedback: List<OutfitFeedback>, today: LocalDate): Set<Set<ClothingItemId>> =
        feedback.asSequence()
            .filter { it.verdict == FeedbackVerdict.NOT_FOR_ME && it.itemIds.isNotEmpty() }
            .filter {
                val age = ChronoUnit.DAYS.between(it.date, today)
                val window = if (it.reason in dayBound) 0L else REJECTION_DAYS
                age in 0..window
            }
            .map { it.itemIds }
            .toSet()

    private val dayBound = setOf(FeedbackReason.WEATHER, FeedbackReason.WORN_RECENTLY, FeedbackReason.ONE_PIECE)
}
