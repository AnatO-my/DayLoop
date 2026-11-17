package com.dayloop.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GuideProgressPreferences(private val dataStore: DataStore<Preferences>) {
    fun observe(key: GuideKey): Flow<GuideProgress> = dataStore.data
        .map { read(it, key) }.distinctUntilChanged()

    suspend fun update(key: GuideKey, expected: GuideProgress, action: GuideAction): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val current = read(preferences, key)
            if (current != expected) return@edit
            val next = current.apply(action)
            val prefix = prefix(key)
            preferences[stringPreferencesKey("${prefix}status")] = next.status.name
            preferences[longPreferencesKey("${prefix}revision")] = next.revision
            val stepKey = stringPreferencesKey("${prefix}step")
            if (next.stepId == null) preferences.remove(stepKey) else preferences[stepKey] = next.stepId
            changed = true
        }
        // A failed edit throws before success can be reported to the caller.
        return changed
    }

    private fun read(preferences: Preferences, key: GuideKey): GuideProgress {
        val prefix = prefix(key)
        val fields = preferences.asMap().entries.associate { it.key.name to it.value }
        val status = fields["${prefix}status"]
        val step = fields["${prefix}step"]
        val revision = fields["${prefix}revision"]
        if (status == null && step == null && revision == null) return GuideProgress()
        // Corrupt or newer state must not restart an automatic tour on every launch.
        val fallback = GuideProgress(GuideStatus.SKIPPED,
            revision = (revision as? Long)?.takeIf { it >= 0 } ?: 0)
        return runCatching {
            require(status is String && revision is Long && (step == null || step is String))
            GuideProgress(GuideStatus.valueOf(status), step as? String, revision)
        }.getOrDefault(fallback)
    }

    private fun prefix(key: GuideKey) = "guide_progress:${key.id}:v${key.version}:"
}
