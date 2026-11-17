package com.dayloop.app.repository

import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.OutfitRecord
import com.dayloop.app.core.outfit.SuggestibleItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface OutfitRepository {
    fun observeActiveWardrobe(): Flow<List<ClothingItem>>
    fun observeActiveWithMedia(): Flow<List<SuggestibleItem>>
    fun observeAllWardrobe(): Flow<List<ClothingItem>>
    fun observeAllOutfits(): Flow<List<OutfitRecord>>
    fun observeOutfit(date: LocalDate): Flow<OutfitRecord?>
    
    suspend fun getOutfitForDate(date: LocalDate): OutfitRecord?
    suspend fun recordOutfit(date: LocalDate, itemIds: List<String>, mediaIds: List<String> = emptyList())
    suspend fun clearOutfit(date: LocalDate)
    
    suspend fun saveClothingItem(item: ClothingItem)
    suspend fun archiveClothingItem(id: String)
    suspend fun unarchiveClothingItem(id: String)
    suspend fun deleteClothingItem(id: String)
    
    suspend fun deleteAllData()
    suspend fun restoreAll(items: List<ClothingItem>, outfits: List<OutfitRecord>)
}
