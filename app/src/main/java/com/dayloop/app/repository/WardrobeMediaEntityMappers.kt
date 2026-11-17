package com.dayloop.app.repository

import com.dayloop.app.core.database.WardrobeMediaEntity
import com.dayloop.app.core.model.WardrobeMedia
import com.dayloop.app.core.model.WardrobeMediaId
import org.json.JSONObject
import java.time.Instant

internal fun WardrobeMediaEntity.toDomain(): WardrobeMedia {
    val labels = mutableMapOf<String, String>()
    labelsJson?.let { json ->
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                labels[key] = obj.getString(key)
            }
        } catch (_: Exception) {}
    }
    return WardrobeMedia(
        id = WardrobeMediaId(id),
        fileName = fileName,
        mimeType = mimeType,
        width = width,
        height = height,
        createdAt = Instant.ofEpochMilli(createdAt),
        labels = labels
    )
}

internal fun WardrobeMedia.toEntity(): WardrobeMediaEntity {
    val json = if (labels.isEmpty()) null else {
        val obj = JSONObject()
        labels.forEach { (k, v) -> obj.put(k, v) }
        obj.toString()
    }
    return WardrobeMediaEntity(
        id = id.value,
        fileName = fileName,
        mimeType = mimeType,
        width = width,
        height = height,
        createdAt = createdAt.toEpochMilli(),
        labelsJson = json
    )
}
