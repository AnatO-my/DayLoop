package com.dayloop.app.today

import com.dayloop.app.core.model.IntervalRule
import com.dayloop.app.core.model.LocalTimeRange
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import com.dayloop.app.core.model.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayAnytimeClassifierTest {
    private val classifier = TodayAnytimeClassifier()
    private val today = LocalDate.of(2026, 8, 31)

    @Test
    fun activeAllDayTaskForTodayIsAnytime() {
        val task = task(
            id = "all-day",
            schedule = TaskSchedule.AllDay(date = today),
        )

        assertEquals(listOf(TodayItem(task)), classifier.classify(listOf(task), today))
    }

    @Test
    fun completedAllDayTaskForTodayIsNotUnresolvedAnytime() {
        val task = task(
            id = "all-day",
            status = TaskStatus.COMPLETED,
            schedule = TaskSchedule.AllDay(date = today),
        )

        assertTrue(classifier.classify(listOf(task), today).isEmpty())
    }

    @Test
    fun allDayTasksForOtherDatesDoNotBecomeAnytimeTodayWithoutRolloverPolicy() {
        val tasks = listOf(
            task(
                id = "yesterday",
                schedule = TaskSchedule.AllDay(date = today.minusDays(1)),
            ),
            task(
                id = "tomorrow",
                schedule = TaskSchedule.AllDay(date = today.plusDays(1)),
            ),
        )

        assertTrue(classifier.classify(tasks, today).isEmpty())
    }

    @Test
    fun classifierLeavesClockedAndGeneratedTimingModelsForOtherPolicies() {
        val tasks = listOf(
            task(
                id = "deadline",
                schedule = TaskSchedule.Deadline(
                    dueAt = LocalDateTime.of(2026, 8, 31, 17, 0),
                ),
            ),
            task(
                id = "time-block",
                schedule = TaskSchedule.TimeBlock(
                    startsAt = LocalDateTime.of(2026, 8, 31, 9, 0),
                    endsAt = LocalDateTime.of(2026, 8, 31, 10, 0),
                ),
            ),
            task(
                id = "interval",
                schedule = TaskSchedule.Interval(
                    rule = IntervalRule(
                        every = Duration.ofHours(1),
                        activeWindow = LocalTimeRange(
                            startsAt = LocalTime.of(9, 0),
                            endsAt = LocalTime.of(17, 0),
                        ),
                        eligibleDays = setOf(DayOfWeek.MONDAY),
                        startsOn = today,
                    ),
                ),
            ),
            task(
                id = "recurrence",
                schedule = TaskSchedule.CalendarRecurrence(
                    rule = RecurrenceRule.daily(startsOn = today),
                ),
            ),
            task(id = "unscheduled"),
        )

        assertTrue(classifier.classify(tasks, today).isEmpty())
    }

    @Test
    fun anytimeClassificationPreservesInputOrder() {
        val second = task(
            id = "second",
            schedule = TaskSchedule.AllDay(date = today),
        )
        val first = task(
            id = "first",
            schedule = TaskSchedule.AllDay(date = today),
        )

        val result = classifier.classify(listOf(second, first), today)

        assertEquals(listOf(second.id, first.id), result.map { item -> item.taskId })
    }

    @Test
    fun anytimeItemsCanPopulateTodaySnapshotAnytimeSection() {
        val task = task(
            id = "anytime",
            schedule = TaskSchedule.AllDay(date = today),
        )

        val snapshot = TodaySnapshot(
            date = today,
            anytime = classifier.classify(listOf(task), today),
        )

        assertEquals(listOf(task.id), snapshot.anytime.map { item -> item.taskId })
        assertEquals(1, snapshot.unresolvedCount)
    }

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.ACTIVE,
        schedule: TaskSchedule? = null,
    ): Task = Task(
        id = TaskId(id),
        title = "Task $id",
        status = status,
        schedule = schedule,
    )
}
