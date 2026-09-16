package com.dayloop.app.core.notifications

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.DayLoopApplication
import com.dayloop.app.MainActivity
import com.dayloop.app.core.alarms.*
import com.dayloop.app.core.model.TaskStatus
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.ui.components.DayLoopBottomActionBar
import com.dayloop.app.ui.components.DayLoopHeroCard
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.DayLoopTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {
    private var locked by mutableStateOf(true)

    override fun onResume() {
        super.onResume()
        locked = getSystemService(KeyguardManager::class.java).isDeviceLocked
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val graph = (application as DayLoopApplication).appGraph
        val id = intent.getStringExtra(ReminderNotifications.EXTRA_ID)?.takeIf { it.isNotBlank() }
        val generation = intent.getLongExtra(ReminderNotifications.EXTRA_GENERATION, 0)
        if (id == null || generation <= 0) { finish(); return }
        setContent {
            val preferences by graph.settingsRepository.settings.collectAsState(AppSettings())
            val current by produceState<Pair<com.dayloop.app.core.model.Task?, ScheduledOccurrence?>?>(null) {
                combine(graph.taskRepository.observeTasks(), graph.scheduledOccurrenceRepository.observeOccurrences()) { tasks, occurrences ->
                    val occurrence = occurrences.firstOrNull { it.id.value == id && it.generation == generation &&
                        it.status != ScheduledOccurrenceStatus.CANCELLED }
                    tasks.firstOrNull { it.id == occurrence?.taskId && it.status == TaskStatus.ACTIVE } to occurrence
                }.collect { value = it }
            }
            val scope = rememberCoroutineScope()
            var busy by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            val hidden = preferences.lockScreenPrivacyEnabled && locked
            val task = current?.first
            val occurrence = current?.second
            fun submit(action: AlarmNotificationAction) {
                if (busy || occurrence == null || hidden) return
                busy = true
                scope.launch {
                    try {
                        when (val result = graph.alarmCoordinator.action(AlarmNotificationCommand(
                            action, AlarmDelivery(occurrence.id, occurrence.generation)))) {
                            is AlarmNotificationActionResult.RescheduleRequested -> {
                                startActivity(Intent(this@ReminderActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .putExtra("edit_task_id", result.taskId.value))
                                finish()
                            }
                            is AlarmNotificationActionResult.Completed, is AlarmNotificationActionResult.Snoozed -> finish()
                            is AlarmNotificationActionResult.SnoozeRejected -> {
                                message = "Snooze saved. Open reminder repair to restore delivery."
                                busy = false
                            }
                            else -> { message = "This reminder has changed. Open Today."; busy = false }
                        }
                    } finally { busy = false }
                }
            }
            DayLoopTheme(settings = preferences) {
                DayLoopScreen(Modifier.statusBarsPadding().navigationBarsPadding()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("DayLoop", style = MaterialTheme.typography.titleLarge)
                        DayLoopHeroCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Reminder", style = MaterialTheme.typography.titleMedium)
                                Text(if (hidden) "Your reminder is ready" else task?.title ?:
                                    if (current == null) "Loading reminder" else "Reminder no longer active",
                                    style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        message?.let { Text(it) }
                        if (!hidden && task != null && occurrence != null) {
                            DayLoopBottomActionBar("Done", { submit(AlarmNotificationAction.COMPLETE) }, enabled = !busy)
                            if (task.reminderPolicy.snoozeAllowed) {
                                DayLoopBottomActionBar("Snooze 10 minutes", { submit(AlarmNotificationAction.SNOOZE) }, enabled = !busy)
                            }
                            if (task.reminderPolicy.rescheduleAllowed) {
                                TextButton(onClick = { submit(AlarmNotificationAction.RESCHEDULE) }, enabled = !busy) { Text("Reschedule") }
                            }
                        }
                        DayLoopBottomActionBar(if (hidden) "Unlock DayLoop" else "Open Today", {
                            val open = { startActivity(Intent(this@ReminderActivity, MainActivity::class.java)); finish() }
                            if (locked) getSystemService(KeyguardManager::class.java).requestDismissKeyguard(
                                this@ReminderActivity, object : KeyguardManager.KeyguardDismissCallback() {
                                    override fun onDismissSucceeded() { locked = false; open() }
                                })
                            else open()
                        })
                    }
                }
            }
        }
    }
}
