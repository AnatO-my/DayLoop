package com.dayloop.app.repository

import com.dayloop.app.core.database.ClothingItemEntity
import com.dayloop.app.core.database.ClothingWithMedia
import com.dayloop.app.core.database.OutfitWithItems
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.OutfitRecord
import com.dayloop.app.core.outfit.SuggestibleItem
import java.time.LocalDate

internal fun ClothingItem.toEntity(): ClothingItemEntity = ClothingItemEntity(
    id = id.value,
    name = name,
    category = category.name,
    colorHex = colorHex,
    isArchived = isArchived
)

internal fun ClothingItemEntity.toDomain(): ClothingItem = ClothingItem(
    id = ClothingItemId(id),
    name = name,
    category = ClothingCategory.valueOf(category),
    colorHex = colorHex,
    isArchived = isArchived
)

internal fun ClothingWithMedia.toSuggestible(): SuggestibleItem = SuggestibleItem(
    item = item.toDomain(),
    media = media.map { it.toDomain() }
)

internal fun OutfitWithItems.toDomain(): OutfitRecord = OutfitRecord(
    date = LocalDate.parse(outfit.date),
    items = items.map { it.toDomain() },
    photos = photos.map { it.toDomain() }
)
