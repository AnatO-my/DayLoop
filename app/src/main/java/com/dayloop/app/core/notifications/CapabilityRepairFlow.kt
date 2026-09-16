package com.dayloop.app.core.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class CapabilityRepairFlow(private val context: Context) {

    fun notificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            applicationDetailsIntent()
        }
    }

    fun exactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= 31) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }
    }

    fun fullScreenIntentSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= 34) {
            // There isn't a direct "request full screen" intent like exact alarm, 
            // usually it's managed via Notification Channel importance or 
            // the "Special app access" section.
            applicationDetailsIntent()
        } else {
            null
        }
    }

    fun applicationDetailsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
