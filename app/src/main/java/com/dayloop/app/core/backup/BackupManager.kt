package com.dayloop.app.core.backup

import com.dayloop.app.core.database.ClothingItemEntity
import com.dayloop.app.core.database.OutfitItemCrossRef
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.di.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Wardrobe text details only: never advertised as a full app or photo backup. */
class BackupManager(private val graph: AppGraph) {
    suspend fun createBackup(password: CharArray): String = withContext(Dispatchers.IO) {
        val (items, outfits) = graph.outfitDao.exportWardrobeDetails()
        val data = JSONObject().apply {
            put("version", 1)
            put("scope", "wardrobe-details")
            put("wardrobe", JSONArray().apply {
                items.forEach { item -> put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("colorHex", item.colorHex ?: JSONObject.NULL)
                    put("isArchived", item.isArchived)
                }) }
            })
            put("outfits", JSONArray().apply {
                outfits.filter { it.items.isNotEmpty() }.forEach { record -> put(JSONObject().apply {
                    put("date", record.outfit.date)
                    put("itemIds", JSONArray(record.items.map { it.id }))
                }) }
            })
        }
        // Apply the same limits to exports and imports so every new export is readable.
        WardrobeBackupPayload.parse(data.toString())
        BackupCipher.encrypt(data.toString(), password)
    }

    suspend fun restoreBackup(encryptedData: String, password: CharArray): Unit = withContext(Dispatchers.IO) {
        val payload = WardrobeBackupPayload.parse(BackupCipher.decrypt(encryptedData, password))
        val today = graph.timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate()
        require(payload.outfits.keys.none { LocalDate.parse(it).isAfter(today) }) {
            "Future outfit dates cannot be imported."
        }
        graph.outfitDao.importWardrobeDetails(payload.items, payload.outfits)
    }
}

internal data class WardrobeBackupPayload(
    val items: List<ClothingItemEntity>,
    val outfits: Map<String, List<OutfitItemCrossRef>>,
) {
    companion object {
        fun parse(text: String): WardrobeBackupPayload {
            val json = JSONObject(text)
            require(json.getInt("version") == 1 && json.getString("scope") == "wardrobe-details")
            val wardrobe = json.getJSONArray("wardrobe")
            val history = json.getJSONArray("outfits")
            require(wardrobe.length() <= 10_000 && history.length() <= 20_000)
            val items = (0 until wardrobe.length()).map { index ->
                val item = wardrobe.getJSONObject(index)
                val id = item.getString("id")
                val name = item.getString("name")
                val category = item.getString("category")
                val color = if (item.isNull("colorHex")) null else item.getString("colorHex")
                require(id.isNotBlank() && id.length <= 128 && name.isNotBlank() && name.length <= 1000)
                ClothingCategory.valueOf(category)
                require(color == null || color.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")))
                ClothingItemEntity(id, name, category, color, item.getBoolean("isArchived"))
            }
            val ids = items.map { it.id }.toSet()
            require(ids.size == items.size) { "Duplicate wardrobe IDs." }
            val outfits = linkedMapOf<String, List<OutfitItemCrossRef>>()
            for (index in 0 until history.length()) {
                val record = history.getJSONObject(index)
                val date = record.getString("date")
                require(LocalDate.parse(date).toString() == date && date !in outfits)
                val refs = record.getJSONArray("itemIds")
                require(refs.length() in 1..100)
                val linkedIds = (0 until refs.length()).map { refs.getString(it) }
                require(linkedIds.toSet().size == linkedIds.size && linkedIds.all { it in ids })
                outfits[date] = linkedIds.map { OutfitItemCrossRef(date, it) }
            }
            return WardrobeBackupPayload(items, outfits)
        }
    }
}
