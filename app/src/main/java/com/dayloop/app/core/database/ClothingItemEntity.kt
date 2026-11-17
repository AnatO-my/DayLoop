package com.dayloop.app.core.database

import androidx.room.*

@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false
)

@Entity(tableName = "outfit_records")
data class OutfitEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: String
)

@Entity(
    tableName = "outfit_items",
    primaryKeys = ["date", "clothing_item_id"]
)
data class OutfitItemCrossRef(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "clothing_item_id") val clothingItemId: String
)

data class OutfitWithItems(
    @Embedded val outfit: OutfitEntity,
    @Relation(
        parentColumn = "date",
        entityColumn = "id",
        associateBy = Junction(OutfitItemCrossRef::class, parentColumn = "date", entityColumn = "clothing_item_id")
    )
    val items: List<ClothingItemEntity>,
    @Relation(
        parentColumn = "date",
        entityColumn = "id",
        associateBy = Junction(OutfitMediaEntity::class, parentColumn = "date", entityColumn = "media_id")
    )
    val photos: List<WardrobeMediaEntity>
)
