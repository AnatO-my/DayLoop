package com.dayloop.app.repository

import com.dayloop.app.core.database.OutfitFeedbackDao
import com.dayloop.app.core.database.OutfitFeedbackEntity
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.outfit.styling.FeedbackReason
import com.dayloop.app.core.outfit.styling.FeedbackVerdict
import com.dayloop.app.core.outfit.styling.OutfitFeedback
import com.dayloop.app.core.outfit.styling.OutfitFeedbackPolicy
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** Stores what the user did with suggested outfits. Nothing here leaves the device. */
interface OutfitFeedbackRepository {
    /** Feedback recent enough to affect suggestions on [today]. */
    fun observeRecent(today: LocalDate): Flow<List<OutfitFeedback>>
    suspend fun record(feedback: OutfitFeedback)

    /** Forgets all feedback, so previously turned-down outfits can be suggested again. */
    suspend fun clear()
}

class RoomOutfitFeedbackRepository(
    private val dao: OutfitFeedbackDao,
    private val timeProvider: TimeProvider,
) : OutfitFeedbackRepository {

    override fun observeRecent(today: LocalDate): Flow<List<OutfitFeedback>> =
        dao.observeSince(today.minusDays(OutfitFeedbackPolicy.REJECTION_DAYS).toString())
            .map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun record(feedback: OutfitFeedback) {
        dao.insert(
            OutfitFeedbackEntity(
                id = UUID.randomUUID().toString(),
                date = feedback.date.toString(),
                itemIds = feedback.itemIds.map { it.value }.sorted().joinToString(","),
                verdict = feedback.verdict.name,
                reason = feedback.reason?.name,
                createdAt = timeProvider.now().toEpochMilli(),
            )
        )
    }

    override suspend fun clear() = dao.deleteAll()
}

/** Rows written by a newer app version with unknown values are skipped, not fatal. */
internal fun OutfitFeedbackEntity.toDomain(): OutfitFeedback? {
    val verdict = FeedbackVerdict.entries.firstOrNull { it.name == verdict } ?: return null
    val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return OutfitFeedback(
        itemIds = itemIds.split(',').filter { it.isNotBlank() }.map(::ClothingItemId).toSet(),
        verdict = verdict,
        reason = FeedbackReason.entries.firstOrNull { it.name == reason },
        date = date,
    )
}
