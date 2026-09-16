package com.dayloop.app.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File
import java.util.UUID

class FontIconPreferencesTest {
    @Test fun bundledFontsLoadNatively() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(com.dayloop.app.R.font.roboto_regular, com.dayloop.app.R.font.opensans_regular,
            com.dayloop.app.R.font.lato_regular, com.dayloop.app.R.font.montserrat_regular,
            com.dayloop.app.R.font.poppins_regular).forEach {
            org.junit.Assert.assertNotNull(androidx.core.content.res.ResourcesCompat.getFont(context, it))
        }
    }
    @Test fun fontAndIconPreferencesPersistIndependently() = runBlocking {
        val job = SupervisorJob()
        val file = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "${UUID.randomUUID()}.preferences_pb")
        try {
            val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
            val repository = DataStoreSettingsRepository(data)
            repository.setFont(AppFont.POPPINS)
            repository.setPlanningIcon("project:one", "TIMER")
            repository.setPlanningIcon("goal:one", "FLAG")
            val state = repository.settings.first()
            assertEquals(AppFont.POPPINS, state.font)
            assertEquals(mapOf("project:one" to "TIMER", "goal:one" to "FLAG"), state.planningIcons)
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
