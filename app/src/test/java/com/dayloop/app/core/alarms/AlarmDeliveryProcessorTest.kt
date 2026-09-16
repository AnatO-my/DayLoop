package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.TaskId
import com.dayloop.app.repository.AlarmDiagnosticsRepository
import com.dayloop.app.repository.ScheduledOccurrenceRepository
import com.dayloop.app.time.FakeTimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmDeliveryProcessorTest {
    @Test
    fun acceptsMatchingScheduledOccurrenceAndMarksItFired() = runBlocking {
        val repository = FakeScheduledOccurrenceRepository(baseOccurrence())
        val processor = AlarmDeliveryProcessor(
            scheduledOccurrenceRepository = repository,
            timeProvider = FakeTimeProvider(Instant.parse("2026-09-06T09:00:05Z")),
        )

        val result = processor.process(AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L))

        val expected = baseOccurrence().markFired(Instant.parse("2026-09-06T09:00:05Z"))
        assertEquals(AlarmDeliveryResult.Accepted(expected), result)
        assertEquals(expected, repository.getOccurrence(ScheduledOccurrenceId("occurrence-1")))
    }

    @Test
    fun ignoresMissingOccurrenceAsStale() = runBlocking {
        val repository = FakeScheduledOccurrenceRepository()
        val processor = AlarmDeliveryProcessor(repository, FakeTimeProvider(Instant.parse("2026-09-06T09:00:05Z")))

        val result = processor.process(AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L))

        assertEquals(AlarmDeliveryResult.StaleMissing(ScheduledOccurrenceId("occurrence-1")), result)
        assertNull(repository.getOccurrence(ScheduledOccurrenceId("occurrence-1")))
    }

    @Test
    fun ignoresOlderGenerationAsStale() = runBlocking {
        val repository = FakeScheduledOccurrenceRepository(baseOccurrence(generation = 3L))
        val processor = AlarmDeliveryProcessor(repository, FakeTimeProvider(Instant.parse("2026-09-06T09:00:05Z")))

        val result = processor.process(AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L))

        assertEquals(
            AlarmDeliveryResult.StaleGeneration(
                occurrenceId = ScheduledOccurrenceId("occurrence-1"),
                expectedGeneration = 3L,
                deliveredGeneration = 2L,
            ),
            result,
        )
        assertEquals(baseOccurrence(generation = 3L), repository.getOccurrence(ScheduledOccurrenceId("occurrence-1")))
    }

    @Test
    fun ignoresResolvedOccurrenceAsStale() = runBlocking {
        val cancelled = baseOccurrence().markCancelled(Instant.parse("2026-09-06T08:30:00Z"))
        val repository = FakeScheduledOccurrenceRepository(cancelled)
        val processor = AlarmDeliveryProcessor(repository, FakeTimeProvider(Instant.parse("2026-09-06T09:00:05Z")))

        val result = processor.process(AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L))

        assertEquals(
            AlarmDeliveryResult.StaleStatus(
                occurrenceId = ScheduledOccurrenceId("occurrence-1"),
                status = ScheduledOccurrenceStatus.CANCELLED,
            ),
            result,
        )
        assertEquals(cancelled, repository.getOccurrence(ScheduledOccurrenceId("occurrence-1")))
    }

    @Test
    fun acceptedDeliveryRecordsDiagnosticEvidence() = runBlocking {
        val repository = FakeScheduledOccurrenceRepository(baseOccurrence())
        val diagnostics = FakeAlarmDiagnosticsRepository()
        val processor = AlarmDeliveryProcessor(
            scheduledOccurrenceRepository = repository,
            timeProvider = FakeTimeProvider(Instant.parse("2026-09-06T09:00:05Z")),
            diagnosticsRepository = diagnostics,
        )

        processor.process(AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 2L))

        assertEquals(AlarmDiagnosticEvent.DELIVERY_ACCEPTED, diagnostics.entries.single().event)
        assertEquals(ScheduledOccurrenceId("occurrence-1"), diagnostics.entries.single().occurrenceId)
        assertEquals(TaskId("task-1"), diagnostics.entries.single().taskId)
    }
    @Test(expected = IllegalArgumentException::class)
    fun deliveryRejectsInvalidGeneration() {
        AlarmDelivery(ScheduledOccurrenceId("occurrence-1"), generation = 0L)
    }

    private fun baseOccurrence(generation: Long = 2L): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId("occurrence-1"),
        taskId = TaskId("task-1"),
        fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        generation = generation,
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
    private class FakeScheduledOccurrenceRepository(
        vararg occurrences: ScheduledOccurrence,
    ) : ScheduledOccurrenceRepository {
        private val stored = occurrences.associateBy { it.id }.toMutableMap()
        private val observed = MutableStateFlow(stored.values.toList())

        override fun observeOccurrences(): Flow<List<ScheduledOccurrence>> = observed

        override suspend fun getOccurrence(id: ScheduledOccurrenceId): ScheduledOccurrence? = stored[id]

        override suspend fun getOccurrencesForTask(taskId: TaskId): List<ScheduledOccurrence> =
            stored.values.filter { it.taskId == taskId }

        override suspend fun getScheduledOccurrences(): List<ScheduledOccurrence> =
            stored.values.filter { it.status == ScheduledOccurrenceStatus.SCHEDULED }

        override suspend fun saveOccurrence(occurrence: ScheduledOccurrence) {
            stored[occurrence.id] = occurrence
            observed.value = stored.values.toList()
        }

        override suspend fun deleteOccurrence(id: ScheduledOccurrenceId): Boolean = stored.remove(id) != null

        override suspend fun deleteOccurrencesForTask(taskId: TaskId): Int {
            val idsToRemove = stored.values.filter { it.taskId == taskId }.map { it.id }
            idsToRemove.forEach { stored.remove(it) }
            return idsToRemove.size
        }
    }
}
