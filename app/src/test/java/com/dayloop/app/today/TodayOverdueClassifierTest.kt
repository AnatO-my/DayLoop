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

class TodayOverdueClassifierTest {
    private val classifier = TodayOverdueClassifier()
    private val now = LocalDateTime.of(2026, 8, 31, 12, 0)

    @Test
    fun activeDeadlineBeforeNowIsOverdue() {
        val task = task(
            id = "deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 11, 59),
            ),
        )

        assertEquals(listOf(TodayItem(task)), classifier.classify(listOf(task), now))
    }

    @Test
    fun deadlineAtNowIsNotOverdueUntilAfterDueTime() {
        val task = task(
            id = "deadline",
            schedule = TaskSchedule.Deadline(dueAt = now),
        )

        assertTrue(classifier.classify(listOf(task), now).isEmpty())
    }

    @Test
    fun futureDeadlineIsNotOverdue() {
        val task = task(
            id = "deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 12, 1),
            ),
        )

        assertTrue(classifier.classify(listOf(task), now).isEmpty())
    }

    @Test
    fun completedPastDeadlineIsNotAnUnresolvedOverdueTask() {
        val task = task(
            id = "deadline",
            status = TaskStatus.COMPLETED,
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 30, 9, 0),
            ),
        )

        assertTrue(classifier.classify(listOf(task), now).isEmpty())
    }

    @Test
    fun classifierLeavesNonDeadlineTimingModelsForLaterPolicies() {
        val tasks = listOf(
            task(
                id = "all-day",
                schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 8, 30)),
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
                        startsOn = LocalDate.of(2026, 8, 30),
                    ),
                ),
            ),
            task(
                id = "recurrence",
                schedule = TaskSchedule.CalendarRecurrence(
                    rule = RecurrenceRule.daily(startsOn = LocalDate.of(2026, 8, 30)),
                ),
            ),
            task(id = "unscheduled"),
        )

        assertTrue(classifier.classify(tasks, now).isEmpty())
    }

    @Test
    fun overdueItemsCanPopulateTodaySnapshotOverdueSection() {
        val task = task(
            id = "deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 9, 0),
            ),
        )

        val snapshot = TodaySnapshot(
            date = now.toLocalDate(),
            overdue = classifier.classify(listOf(task), now),
        )

        assertEquals(listOf(task.id), snapshot.overdue.map { item -> item.taskId })
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
