package com.dayloop.app.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayloop.app.core.database.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WardrobeBackupTest {
    private lateinit var db: DayLoopDatabase
    private val item = ClothingItemEntity("shirt", "Shirt", "TOP")
    private val date = "2026-09-20"

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            DayLoopDatabase::class.java).build()
    }
    @After fun close() = db.close()

    @Test fun repeatImportKeepsExistingDetailsAndPhotoLinks() = runBlocking {
        db.clothingDao().upsert(item)
        db.outfitDao().recordOutfit(date, listOf(item.id), listOf("photo"))
        val newItem = item.copy(id = "shoe", name = "Shoe", category = "FOOTWEAR")
        repeat(2) {
            db.outfitDao().importWardrobeDetails(listOf(item.copy(name = "Old title"), newItem), mapOf(
                date to listOf(OutfitItemCrossRef(date, newItem.id)),
                "2026-09-19" to listOf(OutfitItemCrossRef("2026-09-19", newItem.id)),
            ))
        }
        assertEquals(item, db.clothingDao().getById(item.id))
        assertEquals(2, db.outfitDao().wardrobeForExport().size)
        assertEquals(listOf(item), db.outfitDao().getOutfitWithItemsByDate(date)?.items)
        db.openHelper.readableDatabase.query("SELECT media_id FROM outfit_media WHERE date = '$date'").use {
            assertTrue(it.moveToFirst())
            assertEquals("photo", it.getString(0))
        }
        assertEquals(2, db.outfitDao().outfitsForExport().size)
    }

    @Test fun writeFailureRollsBackNewClothingToo() = runBlocking {
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_import BEFORE INSERT ON outfit_records
            BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent())
        try {
            db.outfitDao().importWardrobeDetails(listOf(item), mapOf(date to listOf(OutfitItemCrossRef(date, item.id))))
            fail("Expected transaction failure")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(db.outfitDao().wardrobeForExport().isEmpty())
    }

    @Test fun parserRejectsUnknownReferencesAndVersions() {
        val empty = """{"version":1,"scope":"wardrobe-details","wardrobe":[],"outfits":[]}"""
        assertTrue(WardrobeBackupPayload.parse(empty).items.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            WardrobeBackupPayload.parse(empty.replace("\"version\":1", "\"version\":2"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WardrobeBackupPayload.parse(empty.replace("\"outfits\":[]",
                "\"outfits\":[{\"date\":\"2026-09-20\",\"itemIds\":[\"missing\"]}]"))
        }
    }
}
