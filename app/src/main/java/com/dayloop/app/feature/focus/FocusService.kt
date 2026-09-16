package com.dayloop.app.feature.focus

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dayloop.app.MainActivity
import com.dayloop.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Duration
import java.time.Instant

class FocusService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    private lateinit var store: FocusStore

    override fun onCreate() {
        super.onCreate()
        store = FocusStore.get(this)
        createNotificationChannel()
        
        store.state
            .distinctUntilChanged { old, new -> 
                old.endsAt == new.endsAt && old.mode == new.mode && old.activeMode == new.activeMode
            }
            .onEach { state ->
                if (state.endsAt != null) {
                    startUpdateLoop()
                } else {
                    stopUpdateLoop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            .launchIn(serviceScope)
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                updateNotification()
                delay(1000)
            }
        }
    }

    private fun stopUpdateLoop() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun updateNotification() {
        val state = runBlocking { store.state.onEach {}.let { } } 
        // Need a sync read or similar. Actually service is observing.
        // Let's use a simpler approach: the loop just refreshes based on current time and the last known endsAt.
    }
    
    // Reworking update logic to avoid runBlocking
    private var lastKnownState: FocusState? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        store.state
            .onEach { lastKnownState = it }
            .launchIn(serviceScope)
        return START_STICKY
    }

    private fun updateNotification(state: FocusState) {
        val now = Instant.now()
        val remaining = state.secondsAt(now)
        val minutes = remaining / 60
        val seconds = remaining % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val extras = Bundle().apply {
            putBoolean("miui.status_bar_show_pill", true)
            putString("miui.pill_text", timeStr)
            putInt("miui.pill_icon", R.drawable.ic_nav_focus)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dayloop_notification)
            .setContentTitle("${state.mode.label} in progress")
            .setContentText("$timeStr remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setExtras(extras)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays the active focus timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "focus_session"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FocusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusService::class.java))
        }
    }
}
