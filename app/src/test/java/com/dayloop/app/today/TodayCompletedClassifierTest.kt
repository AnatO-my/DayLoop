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

class TodayCompletedClassifierTest {
    private val classifier = TodayCompletedClassifier()
    private val today = LocalDate.of(2026, 8, 31)

    @Test
    fun completedAllDayDeadlineAndTimeBlockTasksForDateAreCompletedTodayItems() {
        val allDay = completedTask(
            id = "all-day",
            schedule = TaskSchedule.AllDay(date = today),
        )
        val deadline = completedTask(
            id = "deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 17, 0),
            ),
        )
        val timeBlock = completedTask(
            id = "time-block",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 9, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 10, 0),
            ),
        )

        val result = classifier.classify(listOf(allDay, deadline, timeBlock), today)

        assertEquals(listOf(allDay.id, deadline.id, timeBlock.id), result.map { item -> item.taskId })
    }

    @Test
    fun completedCrossMidnightTimeBlockBelongsToEitherTouchedDate() {
        val task = completedTask(
            id = "cross-midnight",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 30, 23, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 1, 0),
            ),
        )

        assertEquals(listOf(TodayItem(task)), classifier.classify(listOf(task), today))
        assertEquals(listOf(TodayItem(task)), classifier.classify(listOf(task), today.minusDays(1)))
    }

    @Test
    fun activeTasksAreNotCompletedTodayItems() {
        val task = Task(
            id = TaskId("active"),
            title = "Task active",
            schedule = TaskSchedule.AllDay(date = today),
        )

        assertTrue(classifier.classify(listOf(task), today).isEmpty())
    }

    @Test
    fun completedTasksForOtherDatesAndUnsupportedSchedulesAreExcluded() {
        val tasks = listOf(
            completedTask(
                id = "yesterday-all-day",
                schedule = TaskSchedule.AllDay(date = today.minusDays(1)),
            ),
            completedTask(
                id = "tomorrow-deadline",
                schedule = TaskSchedule.Deadline(
                    dueAt = LocalDateTime.of(2026, 9, 1, 9, 0),
                ),
            ),
            completedTask(
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
            completedTask(
                id = "recurrence",
                schedule = TaskSchedule.CalendarRecurrence(
                    rule = RecurrenceRule.daily(startsOn = today),
                ),
            ),
            completedTask(id = "unscheduled"),
        )

        assertTrue(classifier.classify(tasks, today).isEmpty())
    }

    @Test
    fun completedItemsCanPopulateSnapshotProgress() {
        val completed = completedTask(
            id = "all-day",
            schedule = TaskSchedule.AllDay(date = today),
        )

        val snapshot = TodaySnapshot(
            date = today,
            completed = classifier.classify(listOf(completed), today),
        )

        assertEquals(TodayProgress(resolvedCount = 1, requiredCount = 1), snapshot.progress)
        assertEquals(TodayDayStatus.COMPLETE, snapshot.dayStatus)
    }

    private fun completedTask(
        id: String,
        schedule: TaskSchedule? = null,
    ): Task = Task(
        id = TaskId(id),
        title = "Task $id",
        status = TaskStatus.COMPLETED,
        schedule = schedule,
    )
}
