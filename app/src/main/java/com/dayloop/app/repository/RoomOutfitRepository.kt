package com.dayloop.app.repository

import com.dayloop.app.core.database.ClothingDao
import com.dayloop.app.core.database.OutfitDao
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.OutfitRecord
import com.dayloop.app.core.outfit.SuggestibleItem
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class RoomOutfitRepository(
    private val clothingDao: ClothingDao,
    private val outfitDao: OutfitDao,
    private val timeProvider: TimeProvider
) : OutfitRepository {

    override fun observeActiveWardrobe(): Flow<List<ClothingItem>> {
        return clothingDao.observeActive().map { list -> list.map { it.toDomain() } }
    }

    override fun observeActiveWithMedia(): Flow<List<SuggestibleItem>> {
        return clothingDao.observeActiveWithMedia().map { list -> list.map { it.toSuggestible() } }
    }

    override fun observeAllWardrobe(): Flow<List<ClothingItem>> {
        return clothingDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllOutfits(): Flow<List<OutfitRecord>> {
        return outfitDao.observeAllOutfits().map { list -> list.map { it.toDomain() } }
    }

    override fun observeOutfit(date: LocalDate): Flow<OutfitRecord?> {
        return outfitDao.observeOutfitWithItemsByDate(date.toString()).map { it?.toDomain() }
    }

    override suspend fun getOutfitForDate(date: LocalDate): OutfitRecord? {
        return outfitDao.getOutfitWithItemsByDate(date.toString())?.toDomain()
    }

    override suspend fun recordOutfit(date: LocalDate, itemIds: List<String>, mediaIds: List<String>) {
        validateDate(date)
        outfitDao.recordOutfit(date.toString(), itemIds, mediaIds)
    }

    override suspend fun clearOutfit(date: LocalDate) {
        validateDate(date)
        outfitDao.recordOutfit(date.toString(), emptyList(), emptyList())
    }

    override suspend fun saveClothingItem(item: ClothingItem) {
        clothingDao.upsert(item.toEntity())
    }

    override suspend fun archiveClothingItem(id: String) {
        clothingDao.archiveItem(id)
    }

    override suspend fun unarchiveClothingItem(id: String) {
        clothingDao.unarchiveItem(id)
    }

    override suspend fun deleteClothingItem(id: String) {
        clothingDao.deleteClothingItem(id)
    }

    override suspend fun deleteAllData() {
        outfitDao.deleteAllCrossRefs()
        outfitDao.deleteAllOutfits()
        clothingDao.deleteAll()
    }

    override suspend fun restoreAll(items: List<ClothingItem>, outfits: List<OutfitRecord>) {
        deleteAllData()
        items.forEach { clothingDao.upsert(it.toEntity()) }
        outfits.forEach { record ->
            outfitDao.recordOutfit(record.date.toString(), record.items.map { it.id.value }, record.photos.map { it.id.value })
        }
    }

    private fun validateDate(date: LocalDate) {
        val today = timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate()
        if (date.isAfter(today)) {
            throw IllegalArgumentException("Cannot record an outfit for a future date.")
        }
    }
}
