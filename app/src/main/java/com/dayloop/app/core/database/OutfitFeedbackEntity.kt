package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * What the user did with a suggested outfit (schema v19). Kept on the device only; it hides
 * turned-down combinations and is the raw material for later tuning of the recommender.
 */
@Entity(tableName = "outfit_feedback")
data class OutfitFeedbackEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    /** ISO local date the feedback was given. */
    @ColumnInfo(name = "date") val date: String,
    /** Comma-separated clothing item ids, sorted. */
    @ColumnInfo(name = "item_ids") val itemIds: String,
    /** [com.dayloop.app.core.outfit.styling.FeedbackVerdict] name. */
    @ColumnInfo(name = "verdict") val verdict: String,
    /** [com.dayloop.app.core.outfit.styling.FeedbackReason] name, or null. */
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface OutfitFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: OutfitFeedbackEntity)

    @Query("SELECT * FROM outfit_feedback WHERE date >= :sinceDate ORDER BY created_at ASC")
    fun observeSince(sinceDate: String): Flow<List<OutfitFeedbackEntity>>

    @Query("DELETE FROM outfit_feedback")
    suspend fun deleteAll()
}
