package com.dayloop.app.feature.focus

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import org.json.JSONObject
import org.json.JSONArray

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

    const val TOGGLE_ACTION = "miui.focus.action_timer"
    const val END_ACTION = "miui.focus.action_end_timer"

    fun extras(context: Context, protocol: Int, presentation: FocusNotificationPresentation): Bundle {
        if (protocol < 2) return Bundle()
        val iconKey = "miui.focus.pic_timer"
        val iconInfo = JSONObject().put("type", 1).put("pic", iconKey)
        val params = JSONObject()
            .put("protocol", 1)
            .put("business", "focus_timer")
            .put("updatable", true)
            .put("reopen", "reopen")
            .put("enableFloat", false)
            .put("islandFirstFloat", false)
            .put("filterWhenNoPermission", false)
            .put("ticker", presentation.remaining)
            .put("tickerPic", iconKey)
            // Template 13: highlighted countdown + ordinary action buttons (OS2/OS3).
            .put("highlightInfo", JSONObject()
                .put("title", presentation.remaining)
                .put("content", presentation.mode.label)
                .put("subContent", if (presentation.paused) "Paused" else "Remaining")
                .put("picFunction", iconKey)
                .put("colorTitle", "#405000").put("colorTitleDark", "#DDF36B"))
            .put("actions", JSONArray()
                .put(JSONObject().put("action", TOGGLE_ACTION))
                .put(JSONObject().put("action", END_ACTION)))
        if (protocol >= 3) {
            params.put("param_island", JSONObject()
                .put("bigIslandArea", JSONObject()
                    .put("imageTextInfoLeft", JSONObject().put("type", 1)
                        .put("picInfo", iconInfo))
                    // Summary template 6: icon in area A, equal-width digits in area B.
                    .put("sameWidthDigitInfo", JSONObject()
                        .put("digit", presentation.remaining).put("showHighlightColor", false)))
                .put("smallIslandArea", JSONObject().put("picInfo", iconInfo)))
        }
        return Bundle().apply {
            putString("miui.focus.param", JSONObject().put("param_v2", params).toString())
            putBundle("miui.focus.pics", Bundle().apply {
                putParcelable(iconKey, Icon.createWithResource(context, presentation.iconRes))
            })
        }
    }
}
