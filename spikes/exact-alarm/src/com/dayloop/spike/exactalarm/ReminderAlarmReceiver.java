package com.dayloop.spike.exactalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class ReminderAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String occurrenceId = intent.getStringExtra(MainActivity.KEY_OCCURRENCE_ID);
        int generation = intent.getIntExtra(MainActivity.KEY_GENERATION, -1);
        long expectedFireAt = intent.getLongExtra(MainActivity.KEY_EXPECTED_FIRE_AT, -1L);
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String currentOccurrenceId = prefs.getString(MainActivity.KEY_OCCURRENCE_ID, null);
        int currentGeneration = prefs.getInt(MainActivity.KEY_GENERATION, -1);
        long currentExpectedFireAt = prefs.getLong(MainActivity.KEY_EXPECTED_FIRE_AT, -1L);

        boolean valid = occurrenceId != null
            && occurrenceId.equals(currentOccurrenceId)
            && generation == currentGeneration
            && expectedFireAt == currentExpectedFireAt;

        if (valid) {
            MainActivity.appendLog(context, "DELIVERED_VALID occurrence=" + occurrenceId + " generation=" + generation);
            MainActivity.handleValidIntervalDelivery(context);
        } else {
            MainActivity.appendLog(context, "DELIVERED_STALE occurrence=" + occurrenceId + " generation=" + generation + " current=" + currentOccurrenceId + "/" + currentGeneration);
        }
    }
}
