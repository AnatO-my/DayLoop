package com.dayloop.spike.exactalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ExactAlarmPermissionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.appendLog(context, "PERMISSION_STATE_CHANGED canScheduleExactAlarms=" + MainActivity.canScheduleExactAlarms(context));
    }
}
