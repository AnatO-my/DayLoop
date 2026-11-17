package com.dayloop.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.dayloop.app.core.database.ClothingMediaEntity
import com.dayloop.app.core.database.ClothingWithMedia
import com.dayloop.app.core.database.WardrobeMediaDao
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.WardrobeMedia
import com.dayloop.app.core.model.WardrobeMediaId
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RoomWardrobeMediaRepository(
    private val context: Context,
    private val dao: WardrobeMediaDao,
    private val timeProvider: TimeProvider
) : WardrobeMediaRepository {

    private val storageDir = File(context.filesDir, "wardrobe").apply { if (!exists()) mkdirs() }

    override suspend fun importImage(uri: Uri): WardrobeMediaId? = withContext(Dispatchers.IO) {
        try {
            val id = WardrobeMediaId.generate()
            val fileName = "${id.value}.jpg"
            val destFile = File(storageDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            // Extract dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destFile.absolutePath, options)

            val media = WardrobeMedia(
                id = id,
                fileName = fileName,
                mimeType = "image/jpeg",
                width = options.outWidth,
                height = options.outHeight,
                createdAt = timeProvider.now()
            )

            dao.insertMedia(media.toEntity())
            id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun importBitmap(bitmap: Bitmap, labels: Map<String, String>): WardrobeMediaId? = withContext(Dispatchers.IO) {
        try {
            val id = WardrobeMediaId.generate()
            val fileName = "${id.value}.png"
            val destFile = File(storageDir, fileName)

            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            val media = WardrobeMedia(
                id = id,
                fileName = fileName,
                mimeType = "image/png",
                width = bitmap.width,
                height = bitmap.height,
                createdAt = timeProvider.now(),
                labels = labels
            )

            dao.insertMedia(media.toEntity())
            id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun linkMediaToItem(clothingItemId: ClothingItemId, mediaId: WardrobeMediaId, isPrimary: Boolean) {
        if (isPrimary) {
            dao.setPrimary(clothingItemId.value, mediaId.value)
        } else {
            dao.linkMedia(ClothingMediaEntity(clothingItemId.value, mediaId.value, isPrimary))
        }
    }

    override suspend fun deleteMedia(id: WardrobeMediaId) {
        dao.deleteAndUnlinkMedia(id.value)
        File(storageDir, "${id.value}.jpg").takeIf { it.exists() }?.delete()
        File(storageDir, "${id.value}.png").takeIf { it.exists() }?.delete()
    }

    override fun getFile(id: WardrobeMediaId): File? {
        val jpg = File(storageDir, "${id.value}.jpg")
        if (jpg.exists()) return jpg
        val png = File(storageDir, "${id.value}.png")
        if (png.exists()) return png
        return null
    }

    override fun observeMediaForItem(clothingItemId: String): Flow<List<WardrobeMedia>> {
        return combine(
            dao.observeClothingWithMedia(clothingItemId),
            dao.observeAssociations(clothingItemId)
        ) { clothing, associations ->
            clothing?.sortedMedia(associations)?.map { it.toDomain() } ?: emptyList()
        }
    }
}
