package com.dayloop.app.repository

import com.dayloop.app.core.alarms.ScheduledOccurrence
import com.dayloop.app.core.alarms.ScheduledOccurrenceId
import com.dayloop.app.core.alarms.ScheduledOccurrenceStatus
import com.dayloop.app.core.database.ScheduledOccurrenceDao
import com.dayloop.app.core.database.ScheduledOccurrenceEntity
import com.dayloop.app.core.model.TaskId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomScheduledOccurrenceRepositoryTest {
    @Test
    fun saveAndGetOccurrenceRoundTripsDomainModelThroughEntityMapper() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val occurrence = baseOccurrence()

        repository.saveOccurrence(occurrence)

        assertEquals(occurrence, repository.getOccurrence(ScheduledOccurrenceId("occurrence-1")))
    }

    @Test
    fun getOccurrencesForTaskReturnsOnlyMatchingTaskOccurrencesInFireOrder() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val later = baseOccurrence(
            id = "occurrence-later",
            taskId = "task-1",
            fireAt = Instant.parse("2026-09-06T10:00:00Z"),
        )
        val earlier = baseOccurrence(
            id = "occurrence-earlier",
            taskId = "task-1",
            fireAt = Instant.parse("2026-09-06T09:00:00Z"),
        )
        val otherTask = baseOccurrence(
            id = "occurrence-other",
            taskId = "task-2",
            fireAt = Instant.parse("2026-09-06T08:00:00Z"),
        )

        repository.saveOccurrence(later)
        repository.saveOccurrence(otherTask)
        repository.saveOccurrence(earlier)

        assertEquals(listOf(earlier, later), repository.getOccurrencesForTask(TaskId("task-1")))
    }

    @Test
    fun getScheduledOccurrencesExcludesFiredAndCancelledRows() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val scheduled = baseOccurrence(id = "scheduled", fireAt = Instant.parse("2026-09-06T09:00:00Z"))
        val fired = baseOccurrence(id = "fired").markFired(Instant.parse("2026-09-06T09:05:00Z"))
        val cancelled = baseOccurrence(id = "cancelled").markCancelled(Instant.parse("2026-09-06T09:06:00Z"))

        repository.saveOccurrence(fired)
        repository.saveOccurrence(scheduled)
        repository.saveOccurrence(cancelled)

        assertEquals(listOf(scheduled), repository.getScheduledOccurrences())
    }

    @Test
    fun observeOccurrencesEmitsMappedDomainOccurrences() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val first = baseOccurrence(id = "occurrence-1", fireAt = Instant.parse("2026-09-06T09:00:00Z"))
        val second = baseOccurrence(id = "occurrence-2", fireAt = Instant.parse("2026-09-06T10:00:00Z"))

        repository.saveOccurrence(second)
        repository.saveOccurrence(first)

        assertEquals(listOf(first, second), repository.observeOccurrences().first())
    }

    @Test
    fun deleteOccurrenceReportsWhetherARowWasRemoved() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val occurrence = baseOccurrence()

        repository.saveOccurrence(occurrence)

        assertTrue(repository.deleteOccurrence(occurrence.id))
        assertFalse(repository.deleteOccurrence(occurrence.id))
        assertNull(repository.getOccurrence(occurrence.id))
    }

    @Test
    fun deleteOccurrencesForTaskRemovesMatchingTaskRows() = runBlocking {
        val repository = RoomScheduledOccurrenceRepository(FakeScheduledOccurrenceDao())
        val first = baseOccurrence(id = "occurrence-1", taskId = "task-1")
        val second = baseOccurrence(id = "occurrence-2", taskId = "task-1")
        val otherTask = baseOccurrence(id = "occurrence-3", taskId = "task-2")

        repository.saveOccurrence(first)
        repository.saveOccurrence(second)
        repository.saveOccurrence(otherTask)

        assertEquals(2, repository.deleteOccurrencesForTask(TaskId("task-1")))
        assertEquals(emptyList<ScheduledOccurrence>(), repository.getOccurrencesForTask(TaskId("task-1")))
        assertEquals(listOf(otherTask), repository.getOccurrencesForTask(TaskId("task-2")))
    }

    private fun baseOccurrence(
        id: String = "occurrence-1",
        taskId: String = "task-1",
        fireAt: Instant = Instant.parse("2026-09-06T09:00:00Z"),
        generation: Long = 1L,
        createdAt: Instant = Instant.parse("2026-09-06T08:00:00Z"),
        updatedAt: Instant = createdAt,
    ): ScheduledOccurrence = ScheduledOccurrence(
        id = ScheduledOccurrenceId(id),
        taskId = TaskId(taskId),
        fireAt = fireAt,
        generation = generation,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private class FakeScheduledOccurrenceDao : ScheduledOccurrenceDao {
        private val rows = MutableStateFlow<List<ScheduledOccurrenceEntity>>(emptyList())

        override suspend fun upsert(occurrence: ScheduledOccurrenceEntity) {
            rows.value = (rows.value.filterNot { row -> row.id == occurrence.id } + occurrence)
                .sortedWith(compareBy<ScheduledOccurrenceEntity> { row -> row.fireAtEpochMillis }.thenBy { row -> row.id })
        }

        override suspend fun getById(id: String): ScheduledOccurrenceEntity? = rows.value.firstOrNull { row -> row.id == id }

        override suspend fun getForTask(taskId: String): List<ScheduledOccurrenceEntity> =
            rows.value.filter { row -> row.taskId == taskId }

        override suspend fun getScheduled(): List<ScheduledOccurrenceEntity> =
            rows.value.filter { row -> row.status == ScheduledOccurrenceStatus.SCHEDULED.name }

        override fun observeAll(): Flow<List<ScheduledOccurrenceEntity>> = rows

        override suspend fun deleteById(id: String): Int {
            val originalSize = rows.value.size
            rows.value = rows.value.filterNot { row -> row.id == id }
            return originalSize - rows.value.size
        }

        override suspend fun deleteForTask(taskId: String): Int {
            val originalSize = rows.value.size
            rows.value = rows.value.filterNot { row -> row.taskId == taskId }
            return originalSize - rows.value.size
        }
    }
}