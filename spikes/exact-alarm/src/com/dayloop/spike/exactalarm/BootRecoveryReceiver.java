package com.dayloop.spike.exactalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootRecoveryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        MainActivity.appendLog(context, "BOOT_RECEIVED action=" + action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            MainActivity.rescheduleFromPersisted(context, "boot-completed");
        }
    }
}
