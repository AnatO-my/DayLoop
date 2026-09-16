package com.dayloop.app.feature.focus

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.MainActivity
import com.dayloop.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.*
import java.io.IOException
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class FocusService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var store: FocusStore
    private var foregroundStarted = false
    private var protocol = 0
    
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var currentResId: Int = 0
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        store = FocusStore.get(this)
        audioManager = getSystemService(AudioManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Focus Session", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
            }
        )
        
        scope.launch {
            protocol = withContext(Dispatchers.IO) { HyperOsFocusNotification.allowedProtocol(this@FocusService) }
            val graph = (applicationContext as DayLoopApplication).appGraph
            store.state.collectLatest { state ->
                syncAudio(state)
                when (state.endsAt) {
                    null if state.activeMode == null -> {
                        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                    }
                    null -> publish(state)
                    else -> while (isActive) {
                        val settings = graph.settingsRepository.settings.first()
                        if (state.activeMode == FocusMode.WORK && settings.hapticHeartbeatEnabled) {
                            pulseHeartbeat()
                        }

                        try {
                            publish(state)
                            store.finishIfDue(Instant.now()) { minutes ->
                                scope.launch { 
                                    graph.settingsRepository.addFocusMinutes(minutes)
                                    if (minutes >= 25) {
                                        graph.settingsRepository.addGift("Deep Work", "You finished a 25-minute Pomodoro. You're in the zone!")
                                    }
                                }
                            }
                            // Record background activity
                            scope.launch { graph.settingsRepository.addBackgroundServiceSeconds(1) }
                        } catch (error: IOException) {
                            Log.w("DayLoopFocus", "Timer update failed", error)
                        }
                        delay(1.seconds)
                    }
                }
            }
        }
    }

    private fun syncAudio(state: FocusState) {
        val playing = state.endsAt != null && state.preferredSound != FocusSound.NONE
        if (playing) {
            val resId = when (state.preferredSound) {
                FocusSound.RAIN -> R.raw.focus_rain
                FocusSound.FIRE -> R.raw.focus_fire
                FocusSound.STREAM -> R.raw.focus_stream
                FocusSound.LIBRARY -> R.raw.focus_library
                FocusSound.CAFE -> R.raw.focus_cafe
                else -> 0
            }
            if (resId != 0) playAudio(resId) else stopAudio()
        } else {
            stopAudio()
        }
    }

    private fun playAudio(resId: Int) {
        if (currentResId == resId && mediaPlayer?.isPlaying == true) return
        stopAudio()
        
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
            
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .build()
            
        if (audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusRequest = request
            try {
                mediaPlayer = MediaPlayer.create(this, resId).apply {
                    isLooping = true
                    start()
                }
                currentResId = resId
            } catch (e: Exception) {
                Log.e("DayLoopFocus", "Failed to start audio", e)
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        currentResId = 0
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun pulseHeartbeat() {
        val effect = VibrationEffect.createOneShot(12, 40) // Very subtle tap
        vibrator?.vibrate(effect)
    }

    private fun publish(state: FocusState) {
        val paused = state.endsAt == null && state.activeMode != null
        val remaining = state.secondsAt(Instant.now())
        val time = String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60)
        val label = (state.activeMode ?: state.mode).label + if (paused) " paused" else ""
        val title = if (paused) "Resume" else "Pause"
        val icon = if (paused) android.R.drawable.ic_media_play else R.drawable.ic_action_pause
        val action = PendingIntent.getForegroundService(this, if (paused) 2 else 1,
            Intent(this, FocusService::class.java).setAction(if (paused) RESUME else PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val extras = HyperOsFocusNotification.extras(protocol, label, time)
        extras.getString("miui.focus.param")?.let {
            val json = JSONObject(it)
            json.getJSONObject("param_v2").put("actions", JSONArray().put(JSONObject().put("action", "miui.focus.action_timer")))
            extras.putString("miui.focus.param", json.toString())
            extras.putBundle("miui.focus.actions", Bundle().apply {
                putParcelable("miui.focus.action_timer", Notification.Action.Builder(Icon.createWithResource(this@FocusService, icon), title, action).build())
            })
        }
        if (Build.VERSION.SDK_INT >= 36 && state.activeMode != null) extras.putBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING, true)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_dayloop_notification).setContentTitle(label).setContentText("$time remaining")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(icon, title, action)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(state.endsAt != null).setChronometerCountDown(true)
            .setShowWhen(state.endsAt != null).setWhen(state.endsAt?.toEpochMilli() ?: 0L)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).setExtras(extras).build()
        if (foregroundStarted) getSystemService(NotificationManager::class.java).notify(ID, notification)
        else if (Build.VERSION.SDK_INT >= 34) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, notification)
        foregroundStarted = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action in listOf(PAUSE, RESUME)) scope.launch {
            try {
                when (intent?.action) {
                    PAUSE -> store.pause(Instant.now())
                    RESUME -> store.resume(Instant.now())
                }
            } catch (error: IOException) { Log.w("DayLoopFocus", "Timer action failed", error) }
        }
        return START_STICKY
    }

    override fun onDestroy() { 
        stopAudio()
        scope.cancel()
        super.onDestroy() 
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "focus_session_v2"
        private const val ID = 1001
        private const val PAUSE = "com.dayloop.app.action.PAUSE_FOCUS"
        private const val RESUME = "com.dayloop.app.action.RESUME_FOCUS"
        fun start(context: Context) { context.startForegroundService(Intent(context, FocusService::class.java)) }
    }
}
