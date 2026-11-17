package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Optional, user-approved details for one clothing item (schema v18).
 *
 * Kept in its own table so existing `clothing_items` rows, backups and the v17 schema are
 * untouched: an item without a row here simply has no extra details. Enum values are stored
 * as names; unknown names read back as "not set" rather than failing.
 */
@Entity(tableName = "clothing_item_attributes")
data class ClothingAttributesEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "garment_type") val garmentType: String?,
    @ColumnInfo(name = "secondary_color_hex") val secondaryColorHex: String?,
    @ColumnInfo(name = "pattern") val pattern: String?,
    @ColumnInfo(name = "material") val material: String?,
    @ColumnInfo(name = "fit") val fit: String?,
    @ColumnInfo(name = "warmth") val warmth: String?,
    /** Comma-separated [com.dayloop.app.core.model.Occasion] names; null when none chosen. */
    @ColumnInfo(name = "occasions") val occasions: String?,
    @ColumnInfo(name = "model_id") val modelId: String?,
    @ColumnInfo(name = "model_version") val modelVersion: String?,
    @ColumnInfo(name = "type_confidence") val typeConfidence: Double?,
    @ColumnInfo(name = "user_corrected") val userCorrected: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface ClothingAttributesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attributes: ClothingAttributesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ClothingItemEntity)

    @Query("SELECT * FROM clothing_item_attributes WHERE item_id = :itemId LIMIT 1")
    suspend fun getByItemId(itemId: String): ClothingAttributesEntity?

    @Query("SELECT * FROM clothing_item_attributes")
    fun observeAll(): Flow<List<ClothingAttributesEntity>>

    @Query("DELETE FROM clothing_item_attributes WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: String)

    /** The item and its details are saved together or not at all. */
    @Transaction
    suspend fun saveItemWithAttributes(item: ClothingItemEntity, attributes: ClothingAttributesEntity?) {
        upsertItem(item)
        if (attributes == null) deleteByItemId(item.id) else upsert(attributes)
    }
}
