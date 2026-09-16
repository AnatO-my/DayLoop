package com.dayloop.app.feature.focus

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

enum class FocusMode(val label: String, val seconds: Long) {
    WORK("Pomodoro", 25 * 60L), SHORT_BREAK("Short break", 5 * 60L), LONG_BREAK("Long break", 15 * 60L)
}

enum class FocusSound(val label: String) {
    NONE("None"), RAIN("Rain"), FIRE("Fire"), STREAM("Stream"), LIBRARY("Library"), CAFE("Cafe")
}

data class FocusState(
    val mode: FocusMode = FocusMode.WORK,
    val activeMode: FocusMode? = null,
    val remaining: Long = FocusMode.WORK.seconds,
    val endsAt: Instant? = null,
    val minutes: Map<LocalDate, Long> = emptyMap(),
    val durations: Map<FocusMode, Long> = emptyMap(),
    val sessionDuration: Long = mode.seconds,
    val sets: Int = 0,
    val setsUntilLongBreak: Int = 4,
    val preferredSound: FocusSound = FocusSound.NONE,
    val lastSessionReflection: String? = null,
) {
    fun durationFor(mode: FocusMode): Long = durations[mode] ?: mode.seconds
    fun secondsAt(now: Instant): Long = endsAt?.let {
        ((Duration.between(now, it).toMillis() + 999L) / 1000L).coerceAtLeast(0)
    } ?: remaining
    fun minutesOn(date: LocalDate): Long = minutes[date] ?: 0
    fun streakOn(today: LocalDate): Int {
        var date = if (minutesOn(today) > 0) today else today.minusDays(1)
        var count = 0
        while (minutesOn(date) > 0) { count++; date = date.minusDays(1) }
        return count
    }
}

class FocusStore internal constructor(private val data: DataStore<Preferences>) {
    val state = data.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::read)

    suspend fun selectMode(mode: FocusMode) = data.edit { prefs ->
        prefs[Mode] = mode.name
    }

    suspend fun start(now: Instant) = data.edit { prefs ->
        val state = read(prefs)
        if (state.endsAt == null) {
            val isSwitching = state.activeMode != null && state.activeMode != state.mode
            val seconds = if (isSwitching) {
                state.durationFor(state.mode)
            } else {
                state.remaining.takeIf { it > 0 } ?: state.durationFor(state.mode)
            }
            if (isSwitching || state.remaining == 0L || state.activeMode == null) {
                prefs[SessionDuration] = seconds
                prefs[Remaining] = seconds
                prefs[ActiveMode] = state.mode.name
            }
            prefs[End] = now.plusSeconds(seconds).toEpochMilli()
        }
    }

    suspend fun pause(now: Instant, onWorkSessionComplete: (Long) -> Unit = {}) {
        finishIfDue(now, onWorkSessionComplete)
        data.edit { prefs ->
            val state = read(prefs)
            prefs[Remaining] = state.secondsAt(now)
            prefs.remove(End)
        }
    }

    suspend fun resume(now: Instant) = data.edit { prefs ->
        val current = read(prefs)
        if (current.endsAt == null && current.activeMode != null && current.remaining > 0) {
            prefs[End] = now.plusSeconds(current.remaining).toEpochMilli()
        }
    }
    suspend fun reset(mode: FocusMode) = data.edit { prefs ->
        val current = read(prefs)
        val seconds = current.durationFor(mode)
        if (current.mode == mode) {
            prefs[Remaining] = seconds
            prefs[SessionDuration] = seconds
        }
        val isActive = current.activeMode == mode || (current.activeMode == null && current.mode == mode)
        if (isActive) {
            prefs.remove(ActiveMode)
            prefs.remove(End)
        }
    }

    suspend fun setDuration(mode: FocusMode, minutes: Int) {
        require(minutes in 1..180)
        data.edit { prefs ->
            val current = read(prefs)
            val seconds = minutes * 60L
            prefs[durationKey(mode)] = seconds
            // Update the current session if it is for the same mode and not currently running.
            if (current.mode == mode && current.endsAt == null) {
                prefs[Remaining] = seconds
                prefs[SessionDuration] = seconds
            }
        }
    }

    suspend fun finishIfDue(now: Instant, onWorkSessionComplete: (Long) -> Unit = {}) {
        val stateSnapshot = state.first()
        val end = stateSnapshot.endsAt ?: return
        if (now.isBefore(end)) return
        data.edit { prefs ->
            val current = read(prefs)
            val deadline = current.endsAt ?: return@edit
            if (now.isBefore(deadline)) return@edit
            val currentActiveMode = current.activeMode ?: current.mode
            var sets = current.sets
            if (currentActiveMode == FocusMode.WORK) {
                val day = deadline.atZone(ZoneId.systemDefault()).toLocalDate()
                val sessionMinutes = current.sessionDuration / 60
                val totals = current.minutes.toMutableMap()
                totals[day] = (totals[day] ?: 0) + sessionMinutes
                val json = JSONObject()
                totals.toSortedMap().entries.toList().takeLast(366).forEach { json.put(it.key.toString(), it.value) }
                prefs[History] = json.toString()
                sets += 1
                prefs[Sets] = sets
                onWorkSessionComplete(sessionMinutes)
            }
            val nextMode = when (currentActiveMode) {
                FocusMode.WORK -> if (sets > 0 && sets % current.setsUntilLongBreak == 0) FocusMode.LONG_BREAK else FocusMode.SHORT_BREAK
                else -> FocusMode.WORK
            }
            val duration = (prefs[durationKey(nextMode)] ?: nextMode.seconds)
            prefs[ActiveMode] = nextMode.name
            prefs[Mode] = nextMode.name
            prefs[SessionDuration] = duration
            prefs[Remaining] = duration
            prefs[End] = now.plusSeconds(duration).toEpochMilli()
        }
    }

    suspend fun setSetsUntilLongBreak(count: Int) {
        require(count in 1..20)
        data.edit { it[SetsUntilLongBreak] = count }
    }

    suspend fun clearSets() = data.edit { it[Sets] = 0 }

    suspend fun setPreferredSound(sound: FocusSound) = data.edit { it[PreferredSound] = sound.name }

    suspend fun saveReflection(reflection: String) = data.edit { it[LastReflection] = reflection }

    private fun read(prefs: Preferences): FocusState {
        val mode = FocusMode.entries.firstOrNull { it.name == prefs[Mode] } ?: FocusMode.WORK
        val activeMode = FocusMode.entries.firstOrNull { it.name == prefs[ActiveMode] }
        val history = try {
            val json = JSONObject(prefs[History] ?: "{}")
            json.keys().asSequence().associate { LocalDate.parse(it) to json.getLong(it).coerceAtLeast(0) }
        } catch (_: org.json.JSONException) { emptyMap() }
          catch (_: java.time.format.DateTimeParseException) { emptyMap() }
        val durations = FocusMode.entries.associateWith { (prefs[durationKey(it)] ?: it.seconds).coerceIn(60L, 10800L) }
        val sessionDuration = (prefs[SessionDuration] ?: mode.seconds).coerceIn(60L, 10800L)
        val sets = prefs[Sets] ?: 0
        val setsUntilLongBreak = prefs[SetsUntilLongBreak] ?: 4
        val preferredSound = FocusSound.entries.firstOrNull { it.name == prefs[PreferredSound] } ?: FocusSound.NONE
        val lastReflection = prefs[LastReflection]
        return FocusState(mode, activeMode, (prefs[Remaining] ?: sessionDuration).coerceIn(0, sessionDuration),
            prefs[End]?.let(Instant::ofEpochMilli), history, durations, sessionDuration, sets, setsUntilLongBreak,
            preferredSound, lastReflection)
    }

    companion object {
        private val Mode = stringPreferencesKey("mode")
        private val ActiveMode = stringPreferencesKey("active_mode")
        private val End = longPreferencesKey("ends_at")
        private val Remaining = longPreferencesKey("remaining_seconds")
        private val History = stringPreferencesKey("completed_minutes")
        private val SessionDuration = longPreferencesKey("session_duration_seconds")
        private val Sets = intPreferencesKey("focus_sets")
        private val SetsUntilLongBreak = intPreferencesKey("sets_until_long_break")
        private val PreferredSound = stringPreferencesKey("preferred_sound")
        private val LastReflection = stringPreferencesKey("last_reflection")
        private fun durationKey(mode: FocusMode) = longPreferencesKey("duration_${mode.name}")
        @Volatile private var instance: FocusStore? = null
        fun get(context: Context): FocusStore = instance ?: synchronized(this) {
            instance ?: FocusStore(PreferenceDataStoreFactory.create(produceFile = {
                File(context.applicationContext.filesDir, "datastore/focus.preferences_pb")
            })).also { instance = it }
        }
    }
}
