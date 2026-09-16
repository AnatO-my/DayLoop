package com.dayloop.app.feature.focus

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HyperOsFocusNotificationTest {
    @Test fun unsupportedDevicesKeepStandardNotification() {
        assertTrue(HyperOsFocusNotification.extras(0, "Pomodoro", "25:00").isEmpty)
        assertTrue(HyperOsFocusNotification.extras(1, "Pomodoro", "25:00").isEmpty)
    }

    @Test fun os3UsesStructuredPayloadAndParcelableIcons() {
        val extras = HyperOsFocusNotification.extras(3, "Pomodoro", "24:59")
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertEquals("24:59", params.getString("ticker"))
        assertTrue(params.getJSONObject("param_island").has("bigIslandArea"))
        assertTrue(params.getJSONObject("param_island").has("smallIslandArea"))
        assertTrue(extras.get("miui.focus.pics") is Bundle)
        assertTrue(extras.getBundle("miui.focus.pics")!!.containsKey("miui.focus.pic_timer"))
        assertFalse(extras.containsKey("miui.status_bar_show_pill"))
    }

    @Test fun os2DoesNotReceiveOs3IslandLayout() {
        val extras = HyperOsFocusNotification.extras(2, "Short break", "05:00")
        val params = JSONObject(extras.getString("miui.focus.param")!!).getJSONObject("param_v2")
        assertFalse(params.has("param_island"))
        assertEquals("Short break", params.getJSONObject("baseInfo").getString("title"))
    }
}
