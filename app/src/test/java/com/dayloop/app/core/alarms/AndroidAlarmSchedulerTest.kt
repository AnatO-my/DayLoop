package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.time.FakeTimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAlarmSchedulerTest {
    @Test
    fun schedulesExactWakeupWhenOccurrenceIsScheduledAndCapabilityExists() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = true)
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(AlarmScheduleResult.Scheduled(occurrence.id, exact = true), result)
        assertEquals(listOf(occurrence), platform.scheduledOccurrences)
        assertEquals(listOf(occurrence.fireAt.toEpochMilli()), platform.triggerTimes)
    }

    @Test
    fun rejectsResolvedOccurrenceBeforeTouchingPlatform() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = true)
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence().markFired(Instant.parse("2026-09-06T09:01:00Z"))

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.NOT_SCHEDULED_STATUS,
            ),
            result,
        )
        assertTrue(platform.scheduledOccurrences.isEmpty())
    }

    @Test
    fun schedulesInexactFallbackWhenExactAlarmCapabilityIsMissing() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = false)
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(AlarmScheduleResult.Scheduled(occurrence.id, exact = false), result)
        assertTrue(platform.scheduledOccurrences.isEmpty())
        assertEquals(listOf(occurrence), platform.inexactScheduledOccurrences)
        assertEquals(listOf(occurrence.fireAt.toEpochMilli()), platform.inexactTriggerTimes)
    }

    @Test
    fun schedulingExactAlarmRecordsDiagnosticEvidence() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = true)
        val diagnostics = FakeAlarmDiagnosticsRepository()
        val scheduler = AndroidAlarmScheduler(
            platform = platform,
            diagnosticsRepository = diagnostics,
            timeProvider = FakeTimeProvider(NOW),
        )
        val occurrence = baseOccurrence()

        scheduler.schedule(occurrence)

        assertEquals(1, diagnostics.entries.size)
        assertEquals(AlarmDiagnosticEvent.SCHEDULE_EXACT, diagnostics.entries.single().event)
        assertEquals(occurrence.id, diagnostics.entries.single().occurrenceId)
        assertEquals(occurrence.taskId, diagnostics.entries.single().taskId)
        assertEquals(true, diagnostics.entries.single().exact)
        assertEquals(NOW, diagnostics.entries.single().createdAt)
    }

    @Test
    fun schedulingInexactFallbackRecordsDiagnosticEvidence() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = false)
        val diagnostics = FakeAlarmDiagnosticsRepository()
        val scheduler = AndroidAlarmScheduler(
            platform = platform,
            diagnosticsRepository = diagnostics,
            timeProvider = FakeTimeProvider(NOW),
        )
        val occurrence = baseOccurrence()

        scheduler.schedule(occurrence)

        assertEquals(AlarmDiagnosticEvent.SCHEDULE_INEXACT_FALLBACK, diagnostics.entries.single().event)
        assertEquals(false, diagnostics.entries.single().exact)
    }

    @Test
    fun mapsSecurityExceptionToExactAlarmPermissionRejection() = runBlocking {
        val platform = FakeExactAlarmPlatform(
            canScheduleExactAlarms = true,
            scheduleFailure = SecurityException("revoked"),
        )
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
            ),
            result,
        )
    }

    @Test
    fun mapsRuntimeExceptionToScheduleFailure() = runBlocking {
        val platform = FakeExactAlarmPlatform(
            canScheduleExactAlarms = true,
            scheduleFailure = IllegalStateException("alarm service unavailable"),
        )
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.SCHEDULE_FAILED,
            ),
            result,
        )
    }

    @Test
    fun mapsRuntimeExceptionFromInexactFallbackToScheduleFailure() = runBlocking {
        val platform = FakeExactAlarmPlatform(
            canScheduleExactAlarms = false,
            inexactScheduleFailure = IllegalStateException("alarm service unavailable"),
        )
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.SCHEDULE_FAILED,
            ),
            result,
        )
    }

    @Test
    fun cancelReturnsCancelledWhenPlatformFindsPendingIntent() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = true, cancelResult = true)
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrenceId = ScheduledOccurrenceId("occurrence-1")

        val result = scheduler.cancel(occurrenceId)

        assertEquals(AlarmCancelResult.Cancelled(occurrenceId), result)
        assertEquals(listOf(occurrenceId), platform.cancelledOccurrenceIds)
    }

    @Test
    fun cancelReturnsNotFoundWhenPlatformHasNoPendingIntent() = runBlocking {
        val platform = FakeExactAlarmPlatform(canScheduleExactAlarms = true, cancelResult = false)
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrenceId = ScheduledOccurrenceId("occurrence-1")

        val result = scheduler.cancel(occurrenceId)

        assertEquals(AlarmCancelResult.NotFound(occurrenceId), result)
    }

    @Test
    fun cancelReturnsFailedWhenPlatformThrows() = runBlocking {
        val platform = FakeExactAlarmPlatform(
            canScheduleExactAlarms = true,
            cancelFailure = IllegalStateException("alarm service unavailable"),
        )
        val scheduler = AndroidAlarmScheduler(platform)
        val occurrenceId = ScheduledOccurrenceId("occurrence-1")

        val result = scheduler.cancel(occurrenceId)

        assertEquals(AlarmCancelResult.Failed(occurrenceId), result)
    }

    @Test
    fun occurrenceIdentityDataUrisAreStableAndDistinct() {
        val first = AlarmIntentIdentity.dataUri(
            packageName = "com.dayloop.app",
            occurrenceId = ScheduledOccurrenceId("occurrence-1"),
        )
        val second = AlarmIntentIdentity.dataUri(
            packageName = "com.dayloop.app",
            occurrenceId = ScheduledOccurrenceId("occurrence-2"),
        )

        assertEquals(first, AlarmIntentIdentity.dataUri("com.dayloop.app", ScheduledOccurrenceId("occurrence-1")))
        assertTrue(first.startsWith("dayloop-alarm://com.dayloop.app/occurrences/"))
        assertFalse(first == second)
    }

    @Test
    fun receiverClassNameUsesApplicationPackage() {
        assertEquals(
            "com.dayloop.app.core.alarms.AlarmReceiver",
            AlarmIntentIdentity.receiverClassName("com.dayloop.app"),
        )
    }

    private fun baseOccurrence(): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId("occurrence-1"),
        taskId = TaskId("task-1"),
        fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        createdAt = Instant.parse("2026-09-06T08:00:00Z"),
    )

    private class FakeAlarmDiagnosticsRepository : AlarmDiagnosticsRepository {
        val entries = mutableListOf<AlarmDiagnosticEntry>()
        override suspend fun clearAllEntries() { entries.clear() }

        override fun observeEntries(): Flow<List<AlarmDiagnosticEntry>> = flowOf(entries)

        override suspend fun getRecentEntries(limit: Int): List<AlarmDiagnosticEntry> = entries.take(limit)

        override suspend fun append(entry: AlarmDiagnosticEntry) {
            entries += entry
        }
    }

    private class FakeExactAlarmPlatform(
        private val canScheduleExactAlarms: Boolean,
        private val scheduleFailure: RuntimeException? = null,
        private val inexactScheduleFailure: RuntimeException? = null,
        private val cancelResult: Boolean = true,
        private val cancelFailure: RuntimeException? = null,
    ) : ExactAlarmPlatform {
        val scheduledOccurrences = mutableListOf<ScheduledOccurrence>()
        val triggerTimes = mutableListOf<Long>()
        val inexactScheduledOccurrences = mutableListOf<ScheduledOccurrence>()
        val inexactTriggerTimes = mutableListOf<Long>()
        val cancelledOccurrenceIds = mutableListOf<ScheduledOccurrenceId>()

        override fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarms

        override fun scheduleExactAndAllowWhileIdle(
            occurrence: ScheduledOccurrence,
            triggerAtEpochMillis: Long,
        ) {
            scheduleFailure?.let { throw it }
            scheduledOccurrences += occurrence
            triggerTimes += triggerAtEpochMillis
        }

        override fun scheduleInexactAndAllowWhileIdle(
            occurrence: ScheduledOccurrence,
            triggerAtEpochMillis: Long,
        ) {
            inexactScheduleFailure?.let { throw it }
            inexactScheduledOccurrences += occurrence
            inexactTriggerTimes += triggerAtEpochMillis
        }

        override fun cancel(occurrenceId: ScheduledOccurrenceId): Boolean {
            cancelFailure?.let { throw it }
            cancelledOccurrenceIds += occurrenceId
            return cancelResult
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-06T09:05:00Z")
    }
}
