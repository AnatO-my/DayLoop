package com.dayloop.spike.exactalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class UiCommandReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (MainActivity.ACTION_CHECK_UI.equals(action)) {
            MainActivity.logUiCapabilities(context);
        } else if (MainActivity.ACTION_POST_HIGH.equals(action)) {
            MainActivity.postReminderNotification(context, false);
        } else if (MainActivity.ACTION_POST_FULL_SCREEN.equals(action)) {
            MainActivity.postReminderNotification(context, true);
        } else if (MainActivity.ACTION_SCHEDULE_EXACT.equals(action)) {
            long delayMs = intent.getLongExtra("delayMs", 300_000L);
            MainActivity.scheduleNew(context, true, delayMs, "ui-command");
        } else if (MainActivity.ACTION_SCHEDULE_FALLBACK.equals(action)) {
            long delayMs = intent.getLongExtra("delayMs", 300_000L);
            MainActivity.scheduleNew(context, false, delayMs, "ui-command");
        } else if (MainActivity.ACTION_SCHEDULE_INTERVAL.equals(action)) {
            long intervalMs = intent.getLongExtra("intervalMs", 30_000L);
            int windowStart = intent.getIntExtra("windowStartMinute", 0);
            int windowEnd = intent.getIntExtra("windowEndMinute", 1440);
            int maxDeliveries = intent.getIntExtra("maxDeliveries", 3);
            MainActivity.scheduleInterval(context, true, intervalMs, windowStart, windowEnd, maxDeliveries, "ui-command");
        } else if (MainActivity.ACTION_CANCEL.equals(action)) {
            MainActivity.cancelAlarm(context);
        } else if (MainActivity.ACTION_CLEAR.equals(action)) {
            context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(MainActivity.KEY_LOG, "")
                .apply();
        } else {
            MainActivity.appendLog(context, "UI_COMMAND_UNKNOWN action=" + action);
        }
    }
}