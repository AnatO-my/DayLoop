package com.dayloop.app.repository

import android.graphics.Bitmap
import android.net.Uri
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.WardrobeMedia
import com.dayloop.app.core.model.WardrobeMediaId
import kotlinx.coroutines.flow.Flow
import java.io.File

interface WardrobeMediaRepository {
    suspend fun importImage(uri: Uri): WardrobeMediaId?
    suspend fun importBitmap(bitmap: Bitmap, labels: Map<String, String> = emptyMap()): WardrobeMediaId?
    suspend fun linkMediaToItem(clothingItemId: ClothingItemId, mediaId: WardrobeMediaId, isPrimary: Boolean = false)
    suspend fun deleteMedia(id: WardrobeMediaId)
    fun getFile(id: WardrobeMediaId): File?
    fun observeMediaForItem(clothingItemId: String): Flow<List<WardrobeMedia>>
}
