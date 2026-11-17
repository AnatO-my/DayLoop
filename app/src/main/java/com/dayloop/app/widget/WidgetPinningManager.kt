package com.dayloop.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayloop.app.MainActivity

object WidgetPinningManager {

    internal fun isPinningSupported(apiLevel: Int, launcherSupportsPinning: Boolean): Boolean =
        apiLevel >= Build.VERSION_CODES.O && launcherSupportsPinning

    fun isPinningSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        return isPinningSupported(Build.VERSION.SDK_INT, appWidgetManager.isRequestPinAppWidgetSupported)
    }

    fun requestPinTodayWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false

        val provider = ComponentName(context, TodayWidgetReceiver::class.java)
        val successIntent = Intent(context, MainActivity::class.java)
        val successPendingIntent = PendingIntent.getActivity(
            context,
            0,
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return appWidgetManager.requestPinAppWidget(provider, null, successPendingIntent)
    }
}
