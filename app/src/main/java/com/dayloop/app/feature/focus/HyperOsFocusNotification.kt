package com.dayloop.app.feature.focus

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.dayloop.app.R
import org.json.JSONObject

/** Optional OEM presentation; never a prerequisite for running the timer. */
internal object HyperOsFocusNotification {
    fun allowedProtocol(context: Context): Int = try {
        val version = Settings.System.getInt(context.contentResolver, "notification_focus_protocol", 0)
        val permission = if (version >= 2) context.contentResolver.call(
            Uri.parse("content://miui.statusbar.notification.public"),
            "canShowFocus", null, Bundle().apply { putString("package", context.packageName) }
        ) else null
        if (permission?.getBoolean("canShowFocus", false) == true) version else 0
    } catch (_: Exception) {
        0
    }

    fun extras(protocol: Int, label: String, remaining: String): Bundle {
        if (protocol < 2) return Bundle()
        val iconKey = "miui.focus.pic_timer"
        val iconInfo = JSONObject().put("type", 1).put("pic", iconKey)
        val params = JSONObject()
            .put("protocol", 1)
            .put("business", "focus_timer")
            .put("updatable", true)
            .put("enableFloat", false)
            .put("islandFirstFloat", false)
            .put("filterWhenNoPermission", false)
            .put("ticker", remaining)
            .put("tickerPic", iconKey)
            .put("baseInfo", JSONObject().put("type", 2)
                .put("title", label).put("content", "$remaining remaining"))
        if (protocol >= 3) {
            params.put("param_island", JSONObject()
                .put("bigIslandArea", JSONObject()
                    .put("imageTextInfoLeft", JSONObject().put("type", 1)
                        .put("picInfo", iconInfo)
                        .put("textInfo", JSONObject().put("title", remaining).put("content", label)))
                    .put("picInfo", iconInfo))
                .put("smallIslandArea", JSONObject().put("picInfo", iconInfo)))
        }
        return Bundle().apply {
            putString("miui.focus.param", JSONObject().put("param_v2", params).toString())
            putBundle("miui.focus.pics", Bundle().apply {
                putParcelable(iconKey, Icon.createWithResource("com.dayloop.app", R.drawable.ic_nav_focus))
            })
        }
    }
}
