package com.dayloop.app.core.model

import java.time.LocalDate

data class OutfitRecord(
    val date: LocalDate,
    val items: List<ClothingItem>,
    val photos: List<WardrobeMedia> = emptyList()
)
