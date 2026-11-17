package com.dayloop.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ClothingItemEntity)

    @Query("SELECT * FROM clothing_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ClothingItemEntity?

    @Query("SELECT * FROM clothing_items ORDER BY name ASC")
    fun observeAll(): Flow<List<ClothingItemEntity>>

    @Transaction
    @Query("SELECT * FROM clothing_items WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActiveWithMedia(): Flow<List<ClothingWithMedia>>

    @Query("SELECT * FROM clothing_items WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<ClothingItemEntity>>

    @Query("UPDATE clothing_items SET is_archived = 1 WHERE id = :id")
    suspend fun archiveItem(id: String)

    @Query("UPDATE clothing_items SET is_archived = 0 WHERE id = :id")
    suspend fun unarchiveItem(id: String)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM outfit_items WHERE clothing_item_id = :id")
    suspend fun deleteFromOutfits(id: String)

    @Query("DELETE FROM clothing_item_attributes WHERE item_id = :id")
    suspend fun deleteAttributes(id: String)

    @Query("DELETE FROM clothing_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteClothingItem(id: String) {
        deleteFromOutfits(id)
        deleteAttributes(id)
        deleteById(id)
    }
}
