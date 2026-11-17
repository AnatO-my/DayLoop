package com.dayloop.app.repository

import com.dayloop.app.core.database.ClothingAttributesDao
import com.dayloop.app.core.database.ClothingAttributesEntity
import com.dayloop.app.core.model.ClothingFit
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingMaterial
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.HexColor
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.RecognitionStamp
import com.dayloop.app.core.model.Warmth
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the optional details attached to wardrobe items. */
interface WardrobeAttributesRepository {
    fun observeAll(): Flow<Map<ClothingItemId, GarmentAttributes>>
    suspend fun get(id: ClothingItemId): GarmentAttributes?

    /** Saves the item and its details atomically. Empty details remove the details row. */
    suspend fun saveItem(item: ClothingItem, attributes: GarmentAttributes)
}

class RoomWardrobeAttributesRepository(
    private val dao: ClothingAttributesDao,
    private val timeProvider: TimeProvider,
) : WardrobeAttributesRepository {

    override fun observeAll(): Flow<Map<ClothingItemId, GarmentAttributes>> =
        dao.observeAll().map { rows -> rows.associate { ClothingItemId(it.itemId) to it.toDomain() } }

    override suspend fun get(id: ClothingItemId): GarmentAttributes? = dao.getByItemId(id.value)?.toDomain()

    override suspend fun saveItem(item: ClothingItem, attributes: GarmentAttributes) {
        dao.saveItemWithAttributes(
            item.toEntity(),
            if (attributes.isEmpty) null else attributes.toEntity(item.id, timeProvider.now().toEpochMilli()),
        )
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

internal fun ClothingAttributesEntity.toDomain(): GarmentAttributes = GarmentAttributes(
    type = enumOrNull<GarmentType>(garmentType),
    secondaryColorHex = secondaryColorHex?.takeIf(HexColor::isValid),
    pattern = enumOrNull<ClothingPattern>(pattern),
    material = enumOrNull<ClothingMaterial>(material),
    fit = enumOrNull<ClothingFit>(fit),
    warmth = enumOrNull<Warmth>(warmth),
    occasions = occasions.orEmpty().split(',').mapNotNull { enumOrNull<Occasion>(it.trim()) }.toSet(),
    recognition = modelId?.let { id ->
        RecognitionStamp(id, modelVersion.orEmpty(), typeConfidence?.toFloat(), userCorrected)
    },
)

internal fun GarmentAttributes.toEntity(itemId: ClothingItemId, updatedAt: Long): ClothingAttributesEntity =
    ClothingAttributesEntity(
        itemId = itemId.value,
        garmentType = type?.name,
        secondaryColorHex = secondaryColorHex,
        pattern = pattern?.name,
        material = material?.name,
        fit = fit?.name,
        warmth = warmth?.name,
        occasions = occasions.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }?.joinToString(",") { it.name },
        modelId = recognition?.modelId,
        modelVersion = recognition?.modelVersion,
        typeConfidence = recognition?.typeConfidence?.toDouble(),
        userCorrected = recognition?.userCorrected ?: false,
        updatedAt = updatedAt,
    )
