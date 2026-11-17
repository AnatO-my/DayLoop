package com.dayloop.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Query("SELECT * FROM clothing_items")
    suspend fun wardrobeForExport(): List<ClothingItemEntity>

    @Transaction
    @Query("SELECT * FROM outfit_records")
    suspend fun outfitsForExport(): List<OutfitWithItems>

    @Transaction
    suspend fun exportWardrobeDetails(): Pair<List<ClothingItemEntity>, List<OutfitWithItems>> =
        wardrobeForExport() to outfitsForExport()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingClothing(items: List<ClothingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingOutfit(outfit: OutfitEntity): Long

    /** Keep local edits and all photo links; a failed import rolls back as one unit. */
    @Transaction
    suspend fun importWardrobeDetails(
        items: List<ClothingItemEntity>,
        outfits: Map<String, List<OutfitItemCrossRef>>,
    ) {
        insertMissingClothing(items)
        outfits.forEach { (date, refs) ->
            if (insertMissingOutfit(OutfitEntity(date)) != -1L) insertCrossRefs(refs)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: OutfitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<OutfitItemCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCrossRefs(crossRefs: List<OutfitMediaEntity>)

    @Query("DELETE FROM outfit_items WHERE date = :date")
    suspend fun deleteCrossRefsForDate(date: String)

    @Query("DELETE FROM outfit_media WHERE date = :date")
    suspend fun deleteMediaCrossRefsForDate(date: String)

    @Query("DELETE FROM outfit_records WHERE date = :date")
    suspend fun deleteOutfitRecord(date: String)

    @Query("DELETE FROM outfit_records")
    suspend fun deleteAllOutfits()

    @Query("DELETE FROM outfit_items")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM outfit_media")
    suspend fun deleteAllMediaCrossRefs()

    @Transaction
    @Query("SELECT * FROM outfit_records WHERE date = :date LIMIT 1")
    suspend fun getOutfitWithItemsByDate(date: String): OutfitWithItems?

    @Transaction
    @Query("SELECT * FROM outfit_records WHERE date = :date LIMIT 1")
    fun observeOutfitWithItemsByDate(date: String): Flow<OutfitWithItems?>

    @Transaction
    @Query("SELECT * FROM outfit_records")
    fun observeAllOutfits(): Flow<List<OutfitWithItems>>

    @Transaction
    suspend fun recordOutfit(date: String, items: List<String>, mediaIds: List<String> = emptyList()) {
        deleteCrossRefsForDate(date)
        deleteMediaCrossRefsForDate(date)
        if (items.isNotEmpty() || mediaIds.isNotEmpty()) {
            insertOutfit(OutfitEntity(date))
            if (items.isNotEmpty()) {
                val refs = items.map { OutfitItemCrossRef(date, it) }
                insertCrossRefs(refs)
            }
            if (mediaIds.isNotEmpty()) {
                val mediaRefs = mediaIds.map { OutfitMediaEntity(date, it) }
                insertMediaCrossRefs(mediaRefs)
            }
        } else {
            deleteOutfitRecord(date)
        }
    }
}
