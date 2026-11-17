package com.dayloop.app.core.database

import androidx.room.*

@Entity(tableName = "wardrobe_media")
data class WardrobeMediaEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "width") val width: Int,
    @ColumnInfo(name = "height") val height: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "labels_json") val labelsJson: String? = null
)

@Entity(
    tableName = "clothing_item_media",
    primaryKeys = ["clothing_item_id", "media_id"]
)
data class ClothingMediaEntity(
    @ColumnInfo(name = "clothing_item_id") val clothingItemId: String,
    @ColumnInfo(name = "media_id") val mediaId: String,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean = false
)

@Entity(
    tableName = "outfit_media",
    primaryKeys = ["date", "media_id"]
)
data class OutfitMediaEntity(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "media_id") val mediaId: String
)

data class ClothingWithMedia(
    @Embedded val item: ClothingItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(ClothingMediaEntity::class, parentColumn = "clothing_item_id", entityColumn = "media_id")
    )
    val media: List<WardrobeMediaEntity>
) {
    /** Returns media sorted by primary first, then creation date. */
    fun sortedMedia(associations: List<ClothingMediaEntity>): List<WardrobeMediaEntity> {
        val primaryId = associations.find { it.clothingItemId == item.id && it.isPrimary }?.mediaId
        return media.sortedWith(compareByDescending<WardrobeMediaEntity> { it.id == primaryId }.thenByDescending { it.createdAt })
    }
}
