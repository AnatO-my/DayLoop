package com.dayloop.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: WardrobeMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkMedia(association: ClothingMediaEntity)

    @Query("SELECT * FROM wardrobe_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): WardrobeMediaEntity?

    @Transaction
    @Query("SELECT * FROM clothing_items WHERE id = :clothingItemId LIMIT 1")
    fun observeClothingWithMedia(clothingItemId: String): Flow<ClothingWithMedia?>

    @Query("SELECT * FROM clothing_item_media WHERE clothing_item_id = :clothingItemId")
    fun observeAssociations(clothingItemId: String): Flow<List<ClothingMediaEntity>>

    @Query("UPDATE clothing_item_media SET is_primary = 0 WHERE clothing_item_id = :clothingItemId")
    suspend fun clearPrimary(clothingItemId: String)

    @Transaction
    suspend fun setPrimary(clothingItemId: String, mediaId: String) {
        clearPrimary(clothingItemId)
        linkMedia(ClothingMediaEntity(clothingItemId, mediaId, true))
    }

    @Query("DELETE FROM wardrobe_media WHERE id = :id")
    suspend fun deleteMedia(id: String)

    @Query("DELETE FROM clothing_item_media WHERE media_id = :id")
    suspend fun unlinkMedia(id: String)
    
    @Transaction
    suspend fun deleteAndUnlinkMedia(id: String) {
        unlinkMedia(id)
        deleteMedia(id)
    }
}
