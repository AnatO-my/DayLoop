package com.dayloop.app.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

object AppSettingsStore {
    @Volatile private var repository: SettingsRepository? = null

    fun get(context: Context): SettingsRepository = repository ?: synchronized(this) {
        repository ?: DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = {
                File(context.applicationContext.filesDir, "datastore/dayloop.preferences_pb")
            }),
        ).also { repository = it }
    }
}
