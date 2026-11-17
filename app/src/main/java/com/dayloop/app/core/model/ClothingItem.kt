package com.dayloop.app.core.model

import java.util.UUID

@JvmInline
value class ClothingItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ClothingItemId must not be blank." }
    }
    companion object {
        fun generate(): ClothingItemId = ClothingItemId(UUID.randomUUID().toString())
    }
}

enum class ClothingCategory {
    HEADWEAR,
    TOP,
    BOTTOM,
    FOOTWEAR,
    OUTERWEAR,
    ACCESSORY
}

data class ClothingItem(
    val id: ClothingItemId,
    val name: String,
    val category: ClothingCategory,
    val colorHex: String? = null,
    val isArchived: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Clothing item name must not be blank." }
    }
}
