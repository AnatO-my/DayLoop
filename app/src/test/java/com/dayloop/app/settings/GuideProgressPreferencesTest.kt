package com.dayloop.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuideProgressPreferencesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun progressSurvivesClosingAndReopeningStore() = runTest {
        val folder = temporaryFolder.newFolder("reopen")
        val path = File(folder, "reopen.preferences_pb").absolutePath.toPath()
        val job = SupervisorJob(backgroundScope.coroutineContext[Job])
        val original = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(backgroundScope.coroutineContext + job), produceFile = { path })
        val key = GuideKey("today", 1)
        val repository = DataStoreSettingsRepository(original)
        assertTrue(repository.updateGuideProgress(key, GuideProgress(), GuideAction.Start("create_task")))
        val saved = repository.observeGuideProgress(key).first()
        job.cancelAndJoin()
        val reopened = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope, produceFile = { path })
        assertEquals(saved, DataStoreSettingsRepository(reopened).observeGuideProgress(key).first())
    }

    @Test fun concurrentActionsAcceptOnlyOneExpectedRevision() = runTest {
        val folder = temporaryFolder.newFolder("concurrent")
        val store = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope,
            produceFile = { File(folder, "concurrent.preferences_pb").absolutePath.toPath() })
        val repository = DataStoreSettingsRepository(store)
        val key = GuideKey("today", 1)
        val start = async { repository.updateGuideProgress(key, GuideProgress(), GuideAction.Start("navigation")) }
        val skip = async { repository.updateGuideProgress(key, GuideProgress(), GuideAction.Skip) }
        assertEquals(1, listOf(start.await(), skip.await()).count { it })
        assertEquals(1L, repository.observeGuideProgress(key).first().revision)
    }

    @Test fun preservesSettingsVersionsAndRejectsStaleCallbacks() = runTest {
        val folder = temporaryFolder.newFolder("preserves")
        val path = File(folder, "preserves.preferences_pb").absolutePath.toPath()
        val store = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope,
            produceFile = { path })
        val repository = DataStoreSettingsRepository(store)
        val key = GuideKey("today", 1)
        
        val unseen = repository.observeGuideProgress(key).first()
        assertEquals(GuideProgress(), unseen)
        assertTrue(repository.updateGuideProgress(key, unseen, GuideAction.Start("navigation")))
        val started = repository.observeGuideProgress(key).first()
        assertFalse(repository.updateGuideProgress(key, unseen, GuideAction.Skip))
        assertTrue(repository.updateGuideProgress(key, started, GuideAction.Skip))
        val skipped = repository.observeGuideProgress(key).first()
        assertFalse(skipped.allowsAutomaticOffer)
        assertTrue(repository.updateGuideProgress(key, skipped, GuideAction.Replay("navigation")))
        // Same visible step, different revision: an old completion is still stale.
        assertFalse(repository.updateGuideProgress(key, started, GuideAction.Complete))
        val resumed = DataStoreSettingsRepository(store).observeGuideProgress(key).first()
        assertEquals(3L, resumed.revision)
        assertEquals("navigation", resumed.stepId)
        assertEquals(GuideProgress(), repository.observeGuideProgress(GuideKey("today", 2)).first())
    }

    @Test fun invalidStoredStateIsSuppressedAndCanBeExplicitlyReplayed() = runTest {
        val folder = temporaryFolder.newFolder("invalid")
        val path = File(folder, "invalid_replay.preferences_pb").absolutePath.toPath()
        val store = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope,
            produceFile = { path })
        store.edit {
            it[stringPreferencesKey("guide_progress:today:v1:status")] = "FUTURE_STATUS"
            it[longPreferencesKey("guide_progress:today:v1:revision")] = 8
        }
        val repository = DataStoreSettingsRepository(store)
        val key = GuideKey("today", 1)
        val invalid = repository.observeGuideProgress(key).first()
        assertEquals(GuideStatus.SKIPPED, invalid.status)
        assertTrue(repository.updateGuideProgress(key, invalid, GuideAction.Replay("navigation")))
        assertEquals(9L, repository.observeGuideProgress(key).first().revision)
    }

    @Test fun failedWriteDoesNotReportCompletion() = runTest {
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                transform(emptyPreferences())
                throw IOException("Storage unavailable")
            }
        }
        val repository = DataStoreSettingsRepository(store)
        val key = GuideKey("today", 1)
        val result = runCatching { repository.updateGuideProgress(key, GuideProgress(), GuideAction.Start("navigation")) }
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(GuideProgress(), repository.observeGuideProgress(key).first())
    }
}
