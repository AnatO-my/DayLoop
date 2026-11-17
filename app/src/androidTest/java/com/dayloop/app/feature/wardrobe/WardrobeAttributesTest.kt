package com.dayloop.app.feature.wardrobe

import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayloop.app.core.database.DayLoopDatabase
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.core.model.ClothingPattern
import com.dayloop.app.core.model.GarmentAttributes
import com.dayloop.app.core.model.GarmentType
import com.dayloop.app.core.model.Occasion
import com.dayloop.app.core.model.RecognitionStamp
import com.dayloop.app.core.model.Warmth
import com.dayloop.app.core.outfit.recognition.ShapeClassifier
import com.dayloop.app.core.outfit.recognition.ShapeModelWeights
import com.dayloop.app.repository.RoomWardrobeAttributesRepository
import com.dayloop.app.time.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Authored 2026-11-10; not yet run on a device. */
@RunWith(AndroidJUnit4::class)
class WardrobeAttributesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: DayLoopDatabase
    private lateinit var repository: RoomWardrobeAttributesRepository
    private val item = ClothingItem(ClothingItemId("item-1"), "Navy jeans", ClothingCategory.BOTTOM, "#1F2A44")

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(context, DayLoopDatabase::class.java).build()
        repository = RoomWardrobeAttributesRepository(
            database.clothingAttributesDao(),
            object : TimeProvider { override fun now(): Instant = Instant.parse("2026-11-10T08:00:00Z") },
        )
    }

    @After
    fun close() = database.close()

    @Test
    fun itemAndDetailsRoundTripTogether() = runBlocking {
        val details = GarmentAttributes(
            type = GarmentType.JEANS, pattern = ClothingPattern.SOLID, warmth = Warmth.MEDIUM,
            occasions = setOf(Occasion.CASUAL, Occasion.WORK),
            recognition = RecognitionStamp("dayloop-garment-shape", "1.0.0", 0.72f, userCorrected = true),
        )
        repository.saveItem(item, details)
        assertEquals(details, repository.get(item.id))
        assertEquals("Navy jeans", database.clothingDao().getById("item-1")?.name)
        assertEquals(mapOf(item.id to details), repository.observeAll().first())
    }

    @Test
    fun clearingDetailsRemovesTheRowButKeepsTheItem() = runBlocking {
        repository.saveItem(item, GarmentAttributes(type = GarmentType.JEANS))
        repository.saveItem(item.copy(name = "Jeans"), GarmentAttributes())
        assertNull(repository.get(item.id))
        assertEquals("Jeans", database.clothingDao().getById("item-1")?.name)
    }

    @Test
    fun deletingAnItemDeletesItsDetails() = runBlocking {
        repository.saveItem(item, GarmentAttributes(type = GarmentType.JEANS))
        database.clothingDao().deleteClothingItem("item-1")
        assertNull(repository.get(item.id))
    }

    /** Prints on-device latency of the bundled shape model to Logcat (tag WardrobeBench). */
    @Test
    fun shapeModelLoadsFromAssetsAndReportsLatency() {
        val loadStart = SystemClock.elapsedRealtimeNanos()
        val classifier = ShapeClassifier(context.assets.open(ShapeModelWeights.ASSET_PATH).use(ShapeModelWeights::read))
        val loadMillis = (SystemClock.elapsedRealtimeNanos() - loadStart) / 1e6
        val input = FloatArray(28 * 28) { (it % 28) / 28f }
        repeat(50) { classifier.probabilities(input) }
        val times = LongArray(200) {
            val start = SystemClock.elapsedRealtimeNanos()
            classifier.probabilities(input)
            SystemClock.elapsedRealtimeNanos() - start
        }.sorted()
        Log.i("WardrobeBench", "shape model load %.1f ms; forward median %.2f ms; p95 %.2f ms".format(loadMillis, times[100] / 1e6, times[190] / 1e6))
        assertTrue(classifier.probabilities(input).sum() in 0.999f..1.001f)
    }
}
