package com.dayloop.app.feature.focus

import android.os.Bundle
import android.content.Context
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HyperOsFocusNotificationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun extras(protocol: Int, mode: FocusMode = FocusMode.WORK, paused: Boolean = false) =
        HyperOsFocusNotification.extras(context, protocol, FocusNotificationPresentation(mode, paused, "24:59"))

    @Test fun unsupportedDevicesKeepStandardNotification() {
        assertTrue(extras(0).isEmpty)
        assertTrue(extras(1).isEmpty)
    }

    @Test fun os3UsesStructuredPayloadAndParcelableIcons() {
        val extras = extras(3)
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertEquals("24:59", params.getString("ticker"))
        assertTrue(params.getJSONObject("param_island").has("bigIslandArea"))
        assertTrue(params.getJSONObject("param_island").has("smallIslandArea"))
        assertTrue(extras.get("miui.focus.pics") is Bundle)
        assertTrue(extras.getBundle("miui.focus.pics")!!.containsKey("miui.focus.pic_timer"))
        assertFalse(extras.containsKey("miui.status_bar_show_pill"))
        val big = params.getJSONObject("param_island").getJSONObject("bigIslandArea")
        assertEquals("24:59", big.getJSONObject("sameWidthDigitInfo").getString("digit"))
        assertFalse(big.getJSONObject("imageTextInfoLeft").has("textInfo"))
        assertFalse(big.has("picInfo"))
        assertEquals(2, params.getJSONArray("actions").length())
        assertEquals(HyperOsFocusNotification.END_ACTION, params.getJSONArray("actions").getJSONObject(1).getString("action"))
    }

    @Test fun os2DoesNotReceiveOs3IslandLayout() {
        val extras = extras(2, FocusMode.SHORT_BREAK)
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertFalse(params.has("param_island"))
        assertEquals("Short break", params.getJSONObject("highlightInfo").getString("content"))
    }

    @Test fun eachModeHasDistinctIconAndPausedPresentation() {
        val ids = FocusMode.entries.map { mode ->
            val extras = extras(3, mode, paused = true)
            val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
            assertEquals("Paused", params.getJSONObject("highlightInfo").getString("subContent"))
            @Suppress("DEPRECATION")
            val icon = extras.getBundle("miui.focus.pics")!!.getParcelable<Icon>("miui.focus.pic_timer")!!
            icon.resId
        }
        assertEquals(3, ids.toSet().size)
    }
}
