package com.dayloop.spike.exactalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    static final String PACKAGE_LOG_TAG = "DayLoopAlarmSpike";
    static final String ACTION_DELIVER_REMINDER = "dayloop.spike.DELIVER_REMINDER";
    static final String ACTION_CHECK = "dayloop.spike.CHECK";
    static final String ACTION_SCHEDULE_EXACT = "dayloop.spike.SCHEDULE_EXACT";
    static final String ACTION_SCHEDULE_FALLBACK = "dayloop.spike.SCHEDULE_FALLBACK";
    static final String ACTION_SCHEDULE_INTERVAL = "dayloop.spike.SCHEDULE_INTERVAL";
    static final String ACTION_CANCEL = "dayloop.spike.CANCEL";
    static final String ACTION_CLEAR = "dayloop.spike.CLEAR";
    static final String ACTION_POST_HIGH = "dayloop.spike.POST_HIGH";
    static final String ACTION_POST_FULL_SCREEN = "dayloop.spike.POST_FULL_SCREEN";
    static final String ACTION_CHECK_UI = "dayloop.spike.CHECK_UI";
    static final String CHANNEL_REMINDER = "dayloop_spike_reminders";
    static final String PREFS = "alarm_spike";
    static final String KEY_LOG = "log";
    static final String KEY_OCCURRENCE_ID = "occurrenceId";
    static final String KEY_GENERATION = "generation";
    static final String KEY_EXPECTED_FIRE_AT = "expectedFireAtEpochMillis";
    static final String KEY_SCHEDULE_ACTIVE = "scheduleActive";
    static final String KEY_REQUESTED_EXACT = "requestedExact";
    static final String KEY_INTERVAL_ACTIVE = "intervalActive";
    static final String KEY_INTERVAL_MS = "intervalMs";
    static final String KEY_INTERVAL_DELIVERY_COUNT = "intervalDeliveryCount";
    static final String KEY_INTERVAL_MAX_DELIVERIES = "intervalMaxDeliveries";
    static final String KEY_INTERVAL_WINDOW_START_MINUTE = "intervalWindowStartMinute";
    static final String KEY_INTERVAL_WINDOW_END_MINUTE = "intervalWindowEndMinute";
    static final int REQUEST_CODE_REMINDER = 41005;
    static final int NOTIFICATION_ID_HIGH = 42006;
    static final int NOTIFICATION_ID_FULL_SCREEN = 42007;
    private static final long DEFAULT_DELAY_MS = 90_000L;
    private static final long DEFAULT_INTERVAL_MS = 30_000L;

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("DayLoop Alarm Spike");
        logView = new TextView(this);
        logView.setTextSize(14f);
        logView.setPadding(24, 24, 24, 24);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(24, 24, 24, 24);
        controls.addView(button("Refresh capability", new View.OnClickListener() {
            @Override public void onClick(View view) { refreshCapability(); }
        }));
        controls.addView(button("Request exact-alarm settings", new View.OnClickListener() {
            @Override public void onClick(View view) { openExactAlarmSettings(); }
        }));
        controls.addView(button("Schedule exact alarm in 90s", new View.OnClickListener() {
            @Override public void onClick(View view) { scheduleNew(MainActivity.this, true, DEFAULT_DELAY_MS, "button"); renderLog(); }
        }));
        controls.addView(button("Schedule fallback alarm in 90s", new View.OnClickListener() {
            @Override public void onClick(View view) { scheduleNew(MainActivity.this, false, DEFAULT_DELAY_MS, "button"); renderLog(); }
        }));
        controls.addView(button("Cancel alarm", new View.OnClickListener() {
            @Override public void onClick(View view) { cancelAlarm(MainActivity.this); renderLog(); }
        }));
        controls.addView(button("Post high-priority notification", new View.OnClickListener() {
            @Override public void onClick(View view) { postReminderNotification(MainActivity.this, false); renderLog(); }
        }));
        controls.addView(button("Post full-screen notification", new View.OnClickListener() {
            @Override public void onClick(View view) { postReminderNotification(MainActivity.this, true); renderLog(); }
        }));
        controls.addView(button("Check UI capabilities", new View.OnClickListener() {
            @Override public void onClick(View view) { logUiCapabilities(MainActivity.this); renderLog(); }
        }));
        controls.addView(button("Clear log", new View.OnClickListener() {
            @Override public void onClick(View view) { clearLog(); }
        }));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView);
        controls.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));
        setContentView(controls);
        refreshCapability();
        handleCommand(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCommand(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderLog();
    }

    private void handleCommand(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        long delayMs = intent.getLongExtra("delayMs", DEFAULT_DELAY_MS);
        if (ACTION_CHECK.equals(action)) {
            refreshCapability();
        } else if (ACTION_SCHEDULE_EXACT.equals(action)) {
            scheduleNew(this, true, delayMs, "activity-command");
            renderLog();
        } else if (ACTION_SCHEDULE_FALLBACK.equals(action)) {
            scheduleNew(this, false, delayMs, "activity-command");
            renderLog();
        } else if (ACTION_CANCEL.equals(action)) {
            cancelAlarm(this);
            renderLog();
        } else if (ACTION_CHECK_UI.equals(action)) {
            logUiCapabilities(this);
            renderLog();
        } else if (ACTION_POST_HIGH.equals(action)) {
            postReminderNotification(this, false);
            renderLog();
        } else if (ACTION_POST_FULL_SCREEN.equals(action)) {
            postReminderNotification(this, true);
            renderLog();
        } else if (ACTION_CLEAR.equals(action)) {
            clearLog();
        }
    }

    private Button button(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setOnClickListener(listener);
        return button;
    }

    private void refreshCapability() {
        appendLog(this, "CAPABILITY canScheduleExactAlarms=" + canScheduleExactAlarms(this) + " sdk=" + Build.VERSION.SDK_INT);
        renderLog();
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            appendLog(this, "SETTINGS not required below Android 12");
            renderLog();
        }
    }

    private void clearLog() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LOG, "").apply();
        renderLog();
    }

    private void renderLog() {
        logView.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LOG, ""));
    }

    static void scheduleNew(Context context, boolean requestedExact, long delayMs, String source) {
        long now = System.currentTimeMillis();
        long fireAt = now + Math.max(10_000L, delayMs);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        int generation = prefs.getInt(KEY_GENERATION, 0) + 1;
        String occurrenceId = "occ-" + fireAt;
        prefs.edit()
            .putString(KEY_OCCURRENCE_ID, occurrenceId)
            .putInt(KEY_GENERATION, generation)
            .putLong(KEY_EXPECTED_FIRE_AT, fireAt)
            .putBoolean(KEY_SCHEDULE_ACTIVE, true)
            .putBoolean(KEY_REQUESTED_EXACT, requestedExact)
            .apply();
        setBootReceiverEnabled(context, true);
        registerAlarm(context, requestedExact, occurrenceId, generation, fireAt, source);
    }


    static void scheduleInterval(Context context, boolean requestedExact, long intervalMs, int windowStartMinute, int windowEndMinute, int maxDeliveries, String source) {
        long normalizedIntervalMs = Math.max(10_000L, intervalMs);
        int normalizedWindowStart = clampMinute(windowStartMinute, 0);
        int normalizedWindowEnd = clampMinute(windowEndMinute, 1440);
        if (normalizedWindowEnd <= normalizedWindowStart) {
            normalizedWindowStart = 0;
            normalizedWindowEnd = 1440;
        }
        int normalizedMaxDeliveries = Math.max(1, maxDeliveries);
        long now = System.currentTimeMillis();
        long fireAt = computeNextIntervalFireAt(now, normalizedIntervalMs, normalizedWindowStart, normalizedWindowEnd);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        int generation = prefs.getInt(KEY_GENERATION, 0) + 1;
        String occurrenceId = "interval-" + generation + "-" + fireAt;
        prefs.edit()
            .putString(KEY_OCCURRENCE_ID, occurrenceId)
            .putInt(KEY_GENERATION, generation)
            .putLong(KEY_EXPECTED_FIRE_AT, fireAt)
            .putBoolean(KEY_SCHEDULE_ACTIVE, true)
            .putBoolean(KEY_REQUESTED_EXACT, requestedExact)
            .putBoolean(KEY_INTERVAL_ACTIVE, true)
            .putLong(KEY_INTERVAL_MS, normalizedIntervalMs)
            .putInt(KEY_INTERVAL_DELIVERY_COUNT, 0)
            .putInt(KEY_INTERVAL_MAX_DELIVERIES, normalizedMaxDeliveries)
            .putInt(KEY_INTERVAL_WINDOW_START_MINUTE, normalizedWindowStart)
            .putInt(KEY_INTERVAL_WINDOW_END_MINUTE, normalizedWindowEnd)
            .apply();
        setBootReceiverEnabled(context, true);
        appendLog(context, "INTERVAL_START source=" + source
            + " intervalMs=" + normalizedIntervalMs
            + " window=" + normalizedWindowStart + "-" + normalizedWindowEnd
            + " maxDeliveries=" + normalizedMaxDeliveries
            + " firstFireAt=" + stamp(fireAt));
        registerAlarm(context, requestedExact, occurrenceId, generation, fireAt, source + "-interval");
    }

    static void handleValidIntervalDelivery(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean intervalActive = prefs.getBoolean(KEY_INTERVAL_ACTIVE, false);
        boolean scheduleActive = prefs.getBoolean(KEY_SCHEDULE_ACTIVE, false);
        if (!intervalActive || !scheduleActive) {
            return;
        }
        int deliveredCount = prefs.getInt(KEY_INTERVAL_DELIVERY_COUNT, 0) + 1;
        int maxDeliveries = prefs.getInt(KEY_INTERVAL_MAX_DELIVERIES, 1);
        long intervalMs = prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS);
        int windowStart = prefs.getInt(KEY_INTERVAL_WINDOW_START_MINUTE, 0);
        int windowEnd = prefs.getInt(KEY_INTERVAL_WINDOW_END_MINUTE, 1440);
        boolean requestedExact = prefs.getBoolean(KEY_REQUESTED_EXACT, false);
        if (deliveredCount >= maxDeliveries) {
            prefs.edit()
                .putInt(KEY_INTERVAL_DELIVERY_COUNT, deliveredCount)
                .putBoolean(KEY_SCHEDULE_ACTIVE, false)
                .putBoolean(KEY_INTERVAL_ACTIVE, false)
                .remove(KEY_OCCURRENCE_ID)
                .remove(KEY_EXPECTED_FIRE_AT)
                .apply();
            setBootReceiverEnabled(context, false);
            appendLog(context, "INTERVAL_CHAIN_STOP reason=max-deliveries delivered=" + deliveredCount + "/" + maxDeliveries);
            return;
        }
        long nextFireAt = computeNextIntervalFireAt(System.currentTimeMillis(), intervalMs, windowStart, windowEnd);
        int nextGeneration = prefs.getInt(KEY_GENERATION, 0) + 1;
        String nextOccurrenceId = "interval-" + nextGeneration + "-" + nextFireAt;
        prefs.edit()
            .putString(KEY_OCCURRENCE_ID, nextOccurrenceId)
            .putInt(KEY_GENERATION, nextGeneration)
            .putLong(KEY_EXPECTED_FIRE_AT, nextFireAt)
            .putBoolean(KEY_SCHEDULE_ACTIVE, true)
            .putInt(KEY_INTERVAL_DELIVERY_COUNT, deliveredCount)
            .apply();
        appendLog(context, "INTERVAL_NEXT delivered=" + deliveredCount + "/" + maxDeliveries
            + " nextOccurrence=" + nextOccurrenceId
            + " generation=" + nextGeneration
            + " fireAt=" + stamp(nextFireAt));
        registerAlarm(context, requestedExact, nextOccurrenceId, nextGeneration, nextFireAt, "interval-delivery");
    }

    static long computeNextIntervalFireAt(long nowMillis, long intervalMs, int windowStartMinute, int windowEndMinute) {
        long candidate = nowMillis + Math.max(10_000L, intervalMs);
        if (isWithinWindow(candidate, windowStartMinute, windowEndMinute)) {
            return candidate;
        }
        Calendar todayStart = localStartOfDay(nowMillis);
        long todayWindowStart = todayStart.getTimeInMillis() + windowStartMinute * 60_000L;
        if (candidate < todayWindowStart) {
            return todayWindowStart;
        }
        Calendar tomorrowStart = localStartOfDay(nowMillis);
        tomorrowStart.add(Calendar.DAY_OF_YEAR, 1);
        return tomorrowStart.getTimeInMillis() + windowStartMinute * 60_000L;
    }

    private static boolean isWithinWindow(long epochMillis, int windowStartMinute, int windowEndMinute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return minuteOfDay >= windowStartMinute && minuteOfDay < windowEndMinute;
    }

    private static Calendar localStartOfDay(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static int clampMinute(int value, int fallback) {
        if (value < 0 || value > 1440) {
            return fallback;
        }
        return value;
    }

    static void rescheduleFromPersisted(Context context, String source) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean active = prefs.getBoolean(KEY_SCHEDULE_ACTIVE, false);
        String occurrenceId = prefs.getString(KEY_OCCURRENCE_ID, null);
        int generation = prefs.getInt(KEY_GENERATION, -1);
        long fireAt = prefs.getLong(KEY_EXPECTED_FIRE_AT, -1L);
        boolean requestedExact = prefs.getBoolean(KEY_REQUESTED_EXACT, false);
        long now = System.currentTimeMillis();
        appendLog(context, "REBOOT_RELOAD active=" + active + " occurrence=" + occurrenceId + " generation=" + generation + " fireAt=" + stamp(fireAt));
        if (!active || occurrenceId == null || generation < 0 || fireAt <= now) {
            appendLog(context, "REBOOT_SKIP reason=" + (!active ? "inactive" : "past-or-invalid"));
            setBootReceiverEnabled(context, false);
            return;
        }
        registerAlarm(context, requestedExact, occurrenceId, generation, fireAt, source);
    }

    static void registerAlarm(Context context, boolean requestedExact, String occurrenceId, int generation, long fireAt, String source) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            appendLog(context, "SCHEDULE_SKIPPED source=" + source + " no AlarmManager");
            return;
        }
        PendingIntent pendingIntent = reminderPendingIntent(context, occurrenceId, generation, fireAt, false);
        boolean canExact = canScheduleExactAlarms(context);
        try {
            if (requestedExact && canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                appendLog(context, "SCHEDULE_EXACT source=" + source + " occurrence=" + occurrenceId + " generation=" + generation + " fireAt=" + stamp(fireAt));
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                appendLog(context, "SCHEDULE_FALLBACK source=" + source + " requestedExact=" + requestedExact + " canExact=" + canExact + " occurrence=" + occurrenceId + " generation=" + generation + " fireAt=" + stamp(fireAt));
            }
        } catch (SecurityException securityException) {
            appendLog(context, "SECURITY_EXCEPTION source=" + source + " exact permission missing; falling back: " + securityException.getClass().getSimpleName());
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
        }
    }

    static void cancelAlarm(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = reminderPendingIntent(context, "cancel", 0, 0L, true);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_SCHEDULE_ACTIVE, false)
            .putBoolean(KEY_INTERVAL_ACTIVE, false)
            .remove(KEY_OCCURRENCE_ID)
            .remove(KEY_EXPECTED_FIRE_AT)
            .remove(KEY_INTERVAL_MS)
            .remove(KEY_INTERVAL_DELIVERY_COUNT)
            .remove(KEY_INTERVAL_MAX_DELIVERIES)
            .remove(KEY_INTERVAL_WINDOW_START_MINUTE)
            .remove(KEY_INTERVAL_WINDOW_END_MINUTE)
            .apply();
        setBootReceiverEnabled(context, false);
        appendLog(context, "CANCEL requestCode=" + REQUEST_CODE_REMINDER);
    }

    static PendingIntent reminderPendingIntent(Context context, String occurrenceId, int generation, long fireAt, boolean noCreate) {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class)
            .setAction(ACTION_DELIVER_REMINDER)
            .putExtra(KEY_OCCURRENCE_ID, occurrenceId)
            .putExtra(KEY_GENERATION, generation)
            .putExtra(KEY_EXPECTED_FIRE_AT, fireAt);
        int flags = PendingIntent.FLAG_IMMUTABLE;
        flags |= noCreate ? PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REMINDER, intent, flags);
    }

    static void setBootReceiverEnabled(Context context, boolean enabled) {
        ComponentName receiver = new ComponentName(context, BootRecoveryReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(
            receiver,
            enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
        appendLog(context, "BOOT_RECEIVER enabled=" + enabled);
    }

    static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    static void logUiCapabilities(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        boolean notificationsEnabled = notificationManager != null && notificationManager.areNotificationsEnabled();
        boolean postGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            || (notificationManager != null && notificationManager.canUseFullScreenIntent());
        NotificationChannel channel = notificationManager == null ? null : notificationManager.getNotificationChannel(CHANNEL_REMINDER);
        String channelImportance = channel == null ? "missing" : String.valueOf(channel.getImportance());
        appendLog(context, "UI_CAPABILITY notificationsEnabled=" + notificationsEnabled
            + " postGranted=" + postGranted
            + " canUseFullScreenIntent=" + fullScreenAllowed
            + " channelImportance=" + channelImportance
            + " sdk=" + Build.VERSION.SDK_INT);
    }

    static void ensureNotificationChannel(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) return;
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_REMINDER,
            "DayLoop Spike Reminders",
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Disposable DayLoop spike reminders");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }

    static void postReminderNotification(Context context, boolean fullScreen) {
        ensureNotificationChannel(context);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            appendLog(context, "NOTIFICATION_SKIPPED no NotificationManager");
            return;
        }
        boolean postGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            || notificationManager.canUseFullScreenIntent();
        if (!postGranted) {
            appendLog(context, "NOTIFICATION_SKIPPED postNotifications=false requestedFullScreen=" + fullScreen);
            return;
        }

        Intent contentIntent = new Intent(context, MainActivity.class).setAction(ACTION_CHECK_UI);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            context,
            42060,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(fullScreen ? "DayLoop alarm-style spike" : "DayLoop high-priority spike")
            .setContentText(fullScreen ? "Full-screen intent requested" : "High-priority notification posted")
            .setCategory(fullScreen ? Notification.CATEGORY_ALARM : Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH);

        if (fullScreen) {
            Intent fullIntent = new Intent(context, FullScreenAlarmActivity.class)
                .putExtra("source", fullScreenAllowed ? "allowed" : "fallback-denied");
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                42061,
                fullIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }

        int notificationId = fullScreen ? NOTIFICATION_ID_FULL_SCREEN : NOTIFICATION_ID_HIGH;
        notificationManager.notify(notificationId, builder.build());
        appendLog(context, "NOTIFICATION_POSTED type=" + (fullScreen ? "FULL_SCREEN_REQUEST" : "HIGH_PRIORITY")
            + " postGranted=" + postGranted
            + " canUseFullScreenIntent=" + fullScreenAllowed);
    }

    static void appendLog(Context context, String message) {
        String line = stamp(System.currentTimeMillis()) + " " + message;
        android.util.Log.i(PACKAGE_LOG_TAG, line);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        String oldLog = prefs.getString(KEY_LOG, "");
        prefs.edit().putString(KEY_LOG, oldLog + line + "\n").apply();
    }

    static String stamp(long epochMillis) {
        if (epochMillis <= 0L) return "none";
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date(epochMillis));
    }
}
