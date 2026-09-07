package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {
    @Test
    fun scheduleResultCarriesOccurrenceIdentityAndExactness() {
        val result = AlarmScheduleResult.Scheduled(
            occurrenceId = ScheduledOccurrenceId("occurrence-1"),
            exact = true,
        )

        assertEquals(ScheduledOccurrenceId("occurrence-1"), result.occurrenceId)
        assertTrue(result.exact)
    }

    @Test
    fun scheduleRejectionCarriesOccurrenceIdentityAndReason() {
        val result = AlarmScheduleResult.Rejected(
            occurrenceId = ScheduledOccurrenceId("occurrence-1"),
            reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
        )

        assertEquals(ScheduledOccurrenceId("occurrence-1"), result.occurrenceId)
        assertEquals(AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING, result.reason)
    }

    @Test
    fun cancelResultCarriesOccurrenceIdentity() {
        val cancelled = AlarmCancelResult.Cancelled(ScheduledOccurrenceId("occurrence-1"))
        val notFound = AlarmCancelResult.NotFound(ScheduledOccurrenceId("occurrence-2"))
        val failed = AlarmCancelResult.Failed(ScheduledOccurrenceId("occurrence-3"))

        assertEquals(ScheduledOccurrenceId("occurrence-1"), cancelled.occurrenceId)
        assertEquals(ScheduledOccurrenceId("occurrence-2"), notFound.occurrenceId)
        assertEquals(ScheduledOccurrenceId("occurrence-3"), failed.occurrenceId)
    }

    @Test
    fun fakeSchedulerCanImplementContractWithoutAndroidFrameworkTypes() = runBlocking {
        val scheduler = FakeAlarmScheduler(canScheduleExact = true)
        val occurrence = baseOccurrence()

        val scheduleResult = scheduler.schedule(occurrence)
        val cancelResult = scheduler.cancel(occurrence.id)

        assertEquals(AlarmScheduleResult.Scheduled(occurrence.id, exact = true), scheduleResult)
        assertEquals(AlarmCancelResult.Cancelled(occurrence.id), cancelResult)
        assertEquals(listOf(occurrence), scheduler.scheduledOccurrences)
        assertEquals(listOf(occurrence.id), scheduler.cancelledOccurrenceIds)
    }

    @Test
    fun fakeSchedulerCanRepresentCapabilityRejection() = runBlocking {
        val scheduler = FakeAlarmScheduler(canScheduleExact = false)
        val occurrence = baseOccurrence()

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
            ),
            result,
        )
        assertTrue(scheduler.scheduledOccurrences.isEmpty())
    }

    @Test
    fun fakeSchedulerRejectsAlreadyResolvedOccurrence() = runBlocking {
        val scheduler = FakeAlarmScheduler(canScheduleExact = true)
        val occurrence = baseOccurrence().markCancelled(Instant.parse("2026-09-06T08:05:00Z"))

        val result = scheduler.schedule(occurrence)

        assertEquals(
            AlarmScheduleResult.Rejected(
                occurrenceId = occurrence.id,
                reason = AlarmScheduleRejectionReason.NOT_SCHEDULED_STATUS,
            ),
            result,
        )
        assertFalse(scheduler.scheduledOccurrences.contains(occurrence))
    }

    private fun baseOccurrence(): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId("occurrence-1"),
        taskId = TaskId("task-1"),
        fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        createdAt = Instant.parse("2026-09-06T08:00:00Z"),
    )

    private class FakeAlarmScheduler(
        private val canScheduleExact: Boolean,
    ) : AlarmScheduler {
        val scheduledOccurrences = mutableListOf<ScheduledOccurrence>()
        val cancelledOccurrenceIds = mutableListOf<ScheduledOccurrenceId>()

        override suspend fun schedule(occurrence: ScheduledOccurrence): AlarmScheduleResult {
            if (occurrence.status != ScheduledOccurrenceStatus.SCHEDULED) {
                return AlarmScheduleResult.Rejected(
                    occurrenceId = occurrence.id,
                    reason = AlarmScheduleRejectionReason.NOT_SCHEDULED_STATUS,
                )
            }
            if (!canScheduleExact) {
                return AlarmScheduleResult.Rejected(
                    occurrenceId = occurrence.id,
                    reason = AlarmScheduleRejectionReason.EXACT_ALARM_PERMISSION_MISSING,
                )
            }
            scheduledOccurrences += occurrence
            return AlarmScheduleResult.Scheduled(occurrence.id, exact = true)
        }

        override suspend fun cancel(occurrenceId: ScheduledOccurrenceId): AlarmCancelResult {
            cancelledOccurrenceIds += occurrenceId
            return AlarmCancelResult.Cancelled(occurrenceId)
        }
    }
}