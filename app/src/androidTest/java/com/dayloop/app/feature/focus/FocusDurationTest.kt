package com.dayloop.app.feature.focus

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FocusDurationTest {
    private lateinit var job: Job
    private lateinit var file: File
    private lateinit var store: FocusStore

    @Before fun setup() {
        job = SupervisorJob()
        file = File(ApplicationProvider.getApplicationContext<Context>().cacheDir,
            "${UUID.randomUUID()}.preferences_pb")
        store = FocusStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file }))
    }

    @After fun cleanup() = runBlocking { job.cancelAndJoin(); file.delete(); Unit }

    @Test fun customWorkDurationIsCreditedOnce() = runBlocking {
        val start = Instant.parse("2026-09-08T12:00:00Z")
        store.setDuration(FocusMode.WORK, 40)
        assertEquals(2400L, store.state.first().remaining)
        store.start(start)
        val end = start.plusSeconds(2400)
        store.finishIfDue(end)
        store.finishIfDue(end)
        assertEquals(40L, store.state.first().minutesOn(end.atZone(ZoneId.systemDefault()).toLocalDate()))
    }

    @Test fun completionWithoutScreenAdvancesActiveSessionOnlyOnce() = runBlocking {
        val start = Instant.parse("2026-09-10T12:00:00Z")
        store.setDuration(FocusMode.WORK, 1)
        store.start(start)
        store.selectMode(FocusMode.LONG_BREAK)
        val end = start.plusSeconds(60)
        store.finishIfDue(end)
        store.finishIfDue(end)
        val state = store.state.first()
        assertEquals(FocusMode.SHORT_BREAK, state.activeMode)
        assertEquals(end.plusSeconds(state.durationFor(FocusMode.SHORT_BREAK)), state.endsAt)
        assertEquals(1L, state.minutesOn(end.atZone(ZoneId.systemDefault()).toLocalDate()))
    }

    @Test fun editingPreservesRunningAndPausedSessionUntilReset() = runBlocking {
        val start = Instant.parse("2026-09-08T12:00:00Z")
        store.start(start)
        val end = store.state.first().endsAt
        store.setDuration(FocusMode.WORK, 45)
        assertEquals(end, store.state.first().endsAt)
        assertEquals(1500L, store.state.first().sessionDuration)
        store.pause(start.plusSeconds(60))
        store.setDuration(FocusMode.WORK, 50)
        assertEquals(1440L, store.state.first().remaining)
        store.reset(FocusMode.WORK)
        assertEquals(3000L, store.state.first().remaining)
    }

    @Test fun breakDurationsAreIndependent() = runBlocking {
        store.setDuration(FocusMode.SHORT_BREAK, 7)
        store.setDuration(FocusMode.LONG_BREAK, 20)
        store.reset(FocusMode.SHORT_BREAK)
        assertEquals(420L, store.state.first().remaining)
        store.reset(FocusMode.LONG_BREAK)
        assertEquals(1200L, store.state.first().remaining)
        assertEquals(1500L, store.state.first().durationFor(FocusMode.WORK))
    }

    @Test fun notificationPauseResumeIsIdempotentAndIgnoresViewedTab() = runBlocking {
        val start = Instant.parse("2026-09-19T12:00:00Z")
        store.start(start)
        val running = store.state.first().notificationToken()
        store.selectMode(FocusMode.LONG_BREAK)
        store.applyNotificationCommand(FocusNotificationCommand.PAUSE, running, start.plusSeconds(60))
        val paused = store.state.first()
        assertEquals(FocusMode.WORK, paused.activeMode)
        assertEquals(1440L, paused.remaining)
        assertNull(paused.endsAt)
        store.applyNotificationCommand(FocusNotificationCommand.RESUME, paused.notificationToken(), start.plusSeconds(120))
        val resumedEnd = store.state.first().endsAt
        store.applyNotificationCommand(FocusNotificationCommand.RESUME, paused.notificationToken(), start.plusSeconds(180))
        store.applyNotificationCommand(FocusNotificationCommand.END, running, start.plusSeconds(180))
        assertEquals(resumedEnd, store.state.first().endsAt)
    }

    @Test fun notificationEndResetsSelectedModeWithoutClearingHistory() = runBlocking {
        val start = Instant.parse("2026-09-19T12:00:00Z")
        store.start(start)
        store.finishIfDue(start.plusSeconds(1500))
        val before = store.state.first()
        store.selectMode(FocusMode.LONG_BREAK)
        store.applyNotificationCommand(FocusNotificationCommand.END, before.notificationToken(), start.plusSeconds(1501))
        val ended = store.state.first()
        assertNull(ended.activeMode)
        assertNull(ended.endsAt)
        assertEquals(FocusMode.LONG_BREAK.seconds, ended.remaining)
        assertEquals(before.minutes, ended.minutes)
        assertEquals(before.sets, ended.sets)
    }

    @Test fun expiredNotificationCannotEndAutoTransitionedSession() = runBlocking {
        val start = Instant.parse("2026-09-19T12:00:00Z")
        store.start(start)
        val token = store.state.first().notificationToken()
        val end = start.plusSeconds(1500)
        store.applyNotificationCommand(FocusNotificationCommand.END, token, end)
        assertNotNull(store.state.first().endsAt)
        store.finishIfDue(end)
        store.applyNotificationCommand(FocusNotificationCommand.END, token, end.plusSeconds(1))
        assertEquals(FocusMode.SHORT_BREAK, store.state.first().activeMode)
    }

    @Test fun oldPausedControlsCannotTargetIdenticalLaterSession() = runBlocking {
        val start = Instant.parse("2026-09-19T12:00:00Z")
        store.start(start)
        store.pause(start.plusSeconds(60))
        val oldToken = store.state.first().notificationToken()
        store.reset(FocusMode.WORK)
        store.start(start.plusSeconds(600))
        store.pause(start.plusSeconds(660))
        assertEquals(1440L, store.state.first().remaining)
        assertNotEquals(oldToken, store.state.first().notificationToken())
        store.applyNotificationCommand(FocusNotificationCommand.END, oldToken, start.plusSeconds(700))
        assertEquals(FocusMode.WORK, store.state.first().activeMode)
    }
}
