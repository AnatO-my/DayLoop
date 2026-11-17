package com.dayloop.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EditorialTypographyPreferencesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun typographySurvivesReopeningWithoutChangingProfile() = runTest {
        val path = File(folder.newFolder(), "typography.preferences_pb").absolutePath.toPath()
        val job = SupervisorJob(backgroundScope.coroutineContext[Job])
        val store = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(backgroundScope.coroutineContext + job), produceFile = { path })
        val repository = DataStoreSettingsRepository(store)
        assertEquals(AppFont.FUNNEL_DISPLAY, repository.settings.first().font)
        assertTrue(repository.settings.first().readableBodyText)
        repository.setDisplayName("Ot")
        repository.setFont(AppFont.JETBRAINS_MONO)
        repository.setReadableBodyText(false)
        job.cancelAndJoin()
        val reopened = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope, produceFile = { path })
        val saved = DataStoreSettingsRepository(reopened).settings.first()
        assertEquals(AppFont.JETBRAINS_MONO, saved.font)
        assertFalse(saved.readableBodyText)
        assertEquals("Ot", saved.displayName)
    }

    @Test fun newlyBundledFontsPersistLikeExistingTypographyChoices() = runTest {
        val repository = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.createWithPath(scope = backgroundScope, produceFile = {
                File(folder.newFolder(), "bundled-fonts.preferences_pb").absolutePath.toPath()
            }),
        )

        repository.setFont(AppFont.GEIST_MONO)
        assertEquals(AppFont.GEIST_MONO, repository.settings.first().font)
        repository.setFont(AppFont.STACK_SANS_TEXT)
        assertEquals(AppFont.STACK_SANS_TEXT, repository.settings.first().font)
    }

    @Test fun legacyRemovedFontPreferenceFallsBackToFunnelDisplay() = runTest {
        val store = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope, produceFile = {
            File(folder.newFolder(), "legacy-font.preferences_pb").absolutePath.toPath()
        })
        store.edit { it[stringPreferencesKey("editorial_font")] = "OCR_A" }

        assertEquals(
            AppFont.FUNNEL_DISPLAY,
            DataStoreSettingsRepository(store).settings.first().font,
        )
    }
}
