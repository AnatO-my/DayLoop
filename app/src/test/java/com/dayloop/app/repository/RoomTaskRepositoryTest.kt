package com.dayloop.app.repository

import com.dayloop.app.core.database.TaskDao
import com.dayloop.app.core.database.TaskEntity
import com.dayloop.app.core.model.IntervalAnchor
import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskPriority
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTaskRepositoryTest {
    @Test
    fun descriptionsSurviveSaveEditAndClear() = runBlocking {
        val repository = RoomTaskRepository(FakeTaskDao())
        val task = Task(id = TaskId("description"), title = "Read", description = "First line\nSecond line")
        repository.saveTask(task)
        assertEquals(task, repository.getTask(task.id))
        for (description in listOf("Updated description", "", null)) {
            val updated = task.copy(description = description)
            repository.saveTask(updated)
            assertEquals(updated, repository.getTask(task.id))
            assertEquals(description, repository.observeTask(task.id).first()?.description)
        }
    }

    @Test
    fun saveAndGetTaskRoundTripsDomainModelThroughEntityMapper() = runBlocking {
        val repository = RoomTaskRepository(FakeTaskDao())
        val task = Task(
            id = TaskId("task-1"),
            title = "Hydrate",
            status = TaskStatus.ACTIVE,
            priority = TaskPriority.HIGH,
            schedule = TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofMinutes(90),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(9, 0),
                        endsAt = LocalTime.of(17, 0),
                    ),
                    eligibleDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    startsOn = LocalDate.of(2026, 8, 31),
                    endsOn = LocalDate.of(2026, 9, 30),
                    anchor = IntervalAnchor.COMPLETION,
                ),
            ),
            reminderPolicy = ReminderPolicy.persistent(
                realertInterval = Duration.ofMinutes(15),
                maximumRealerts = 3,
            ),
        )

        repository.saveTask(task)

        assertEquals(task, repository.getTask(TaskId("task-1")))
    }

    @Test
    fun observeTasksEmitsMappedDomainTasks() = runBlocking {
        val repository = RoomTaskRepository(FakeTaskDao())
        val first = Task(
            id = TaskId("task-1"),
            title = "Plan",
            schedule = TaskSchedule.AllDay(LocalDate.of(2026, 8, 31)),
        )
        val second = Task(
            id = TaskId("task-2"),
            title = "Review",
            status = TaskStatus.COMPLETED,
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.weekly(
                    startsOn = LocalDate.of(2026, 9, 1),
                    daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                    every = 2,
                ),
            ),
        )

        repository.saveTask(second)
        repository.saveTask(first)

        assertEquals(listOf(first, second), repository.observeTasks().first())
        assertEquals(second, repository.observeTask(TaskId("task-2")).first())
    }

    @Test
    fun deleteTaskReportsWhetherARowWasRemoved() = runBlocking {
        val repository = RoomTaskRepository(FakeTaskDao())
        val task = Task(
            id = TaskId("task-1"),
            title = "Plan",
        )

        repository.saveTask(task)

        assertTrue(repository.deleteTask(TaskId("task-1")))
        assertFalse(repository.deleteTask(TaskId("task-1")))
        assertNull(repository.getTask(TaskId("task-1")))
    }

    @Test
    fun taskEntityMapperRoundTripsEveryScheduleKind() {
        val schedules = listOf(
            null,
            TaskSchedule.AllDay(LocalDate.of(2026, 8, 31)),
            TaskSchedule.Deadline(LocalDateTime.of(2026, 8, 31, 17, 30)),
            TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 9, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 10, 0),
            ),
            TaskSchedule.Interval(
                rule = IntervalRule(
                    every = Duration.ofHours(2),
                    activeWindow = LocalTimeRange(
                        startsAt = LocalTime.of(9, 0),
                        endsAt = LocalTime.of(17, 0),
                    ),
                    eligibleDays = setOf(DayOfWeek.MONDAY),
                    startsOn = LocalDate.of(2026, 8, 31),
                ),
            ),
            TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.monthly(
                    startsOn = LocalDate.of(2026, 1, 31),
                    every = 1,
                    endsOn = LocalDate.of(2026, 12, 31),
                ),
            ),
        )

        schedules.forEachIndexed { index, schedule ->
            val task = Task(
                id = TaskId("task-$index"),
                title = "Task $index",
                schedule = schedule,
            )

            assertEquals(task, task.toEntity().toDomain())
        }
    }

    @Test
    fun mapperRejectsStoredRowsMissingRequiredScheduleColumns() {
        val entity = baseEntity(
            scheduleKind = "TIME_BLOCK",
            timeBlockStartsAt = "2026-08-31T09:00",
            timeBlockEndsAt = null,
        )

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun mapperPreservesStrictReminderPolicySeparatelyFromPriority() {
        val task = Task(
            id = TaskId("task-1"),
            title = "File report",
            priority = TaskPriority.LOW,
            reminderPolicy = ReminderPolicy.strict(
                realertInterval = Duration.ofMinutes(10),
                maximumRealerts = 2,
            ),
        )

        val roundTripped = task.toEntity().toDomain()

        assertEquals(TaskPriority.LOW, roundTripped.priority)
        assertEquals(ReminderMode.STRICT, roundTripped.reminderPolicy.mode)
        assertEquals(Duration.ofMinutes(10), roundTripped.reminderPolicy.realertInterval)
    }

    private fun baseEntity(
        scheduleKind: String? = "ALL_DAY",
        timeBlockStartsAt: String? = null,
        timeBlockEndsAt: String? = null,
    ): TaskEntity = TaskEntity(
        id = "task-1",
        title = "Plan",
        status = "ACTIVE",
        priority = "NORMAL",
        scheduleKind = scheduleKind,
        allDayDate = if (scheduleKind == "ALL_DAY") "2026-08-31" else null,
        timeBlockStartsAt = timeBlockStartsAt,
        timeBlockEndsAt = timeBlockEndsAt,
    )

    private class FakeTaskDao : TaskDao {
        private val rows = MutableStateFlow<List<TaskEntity>>(emptyList())

        override suspend fun upsert(task: TaskEntity) {
            rows.value = (rows.value.filterNot { row -> row.id == task.id } + task)
                .sortedBy { row -> row.id }
        }

        override suspend fun getById(id: String): TaskEntity? = rows.value.firstOrNull { row -> row.id == id }

        override suspend fun getAll(): List<TaskEntity> = rows.value

        override fun observeAll(): Flow<List<TaskEntity>> = rows

        override fun observeById(id: String): Flow<TaskEntity?> = rows.map { values ->
            values.firstOrNull { row -> row.id == id }
        }

        override suspend fun getForProject(projectId: String): List<TaskEntity> =
            rows.value.filter { it.projectId == projectId }

        override fun observeForProject(projectId: String): Flow<List<TaskEntity>> =
            rows.map { values -> values.filter { it.projectId == projectId } }

        override suspend fun deleteById(id: String): Int {
            val originalSize = rows.value.size
            rows.value = rows.value.filterNot { row -> row.id == id }
            return originalSize - rows.value.size
        }
    }
}
