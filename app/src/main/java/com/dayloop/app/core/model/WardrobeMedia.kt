package com.dayloop.app.core.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class WardrobeMediaId(val value: String) {
    init {
        require(value.isNotBlank()) { "WardrobeMediaId must not be blank." }
    }
    companion object {
        fun generate(): WardrobeMediaId = WardrobeMediaId(UUID.randomUUID().toString())
    }
}

data class WardrobeMedia(
    val id: WardrobeMediaId,
    val fileName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val createdAt: Instant,
    val labels: Map<String, String> = emptyMap()
)

data class ClothingMediaAssociation(
    val clothingItemId: ClothingItemId,
    val mediaId: WardrobeMediaId,
    val isPrimary: Boolean = false
)
