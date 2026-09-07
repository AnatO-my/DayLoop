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

class TodayNowNextClassifierTest {
    private val classifier = TodayNowNextClassifier()
    private val now = LocalDateTime.of(2026, 8, 31, 12, 0)

    @Test
    fun activeTimeBlockIsNow() {
        val task = task(
            id = "active-block",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 11, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 13, 0),
            ),
        )

        val result = classifier.classify(listOf(task), now)

        assertEquals(listOf(TodayItem(task)), result.now)
        assertTrue(result.next.isEmpty())
    }

    @Test
    fun timeBlockStartIsInclusiveAndEndIsExclusiveForNow() {
        val startsNow = task(
            id = "starts-now",
            schedule = TaskSchedule.TimeBlock(
                startsAt = now,
                endsAt = now.plusHours(1),
            ),
        )
        val endedNow = task(
            id = "ended-now",
            schedule = TaskSchedule.TimeBlock(
                startsAt = now.minusHours(1),
                endsAt = now,
            ),
        )

        val result = classifier.classify(listOf(startsNow, endedNow), now)

        assertEquals(listOf(TodayItem(startsNow)), result.now)
        assertTrue(result.next.isEmpty())
    }

    @Test
    fun crossMidnightTimeBlockIsNowWhenCurrentLocalTimeFallsInsideIt() {
        val task = task(
            id = "cross-midnight",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 30, 23, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 1, 0),
            ),
        )

        val result = classifier.classify(
            tasks = listOf(task),
            now = LocalDateTime.of(2026, 8, 31, 0, 30),
        )

        assertEquals(listOf(TodayItem(task)), result.now)
    }

    @Test
    fun sameDayFutureTimeBlocksAndDeadlinesAreNextInTimeOrder() {
        val deadline = task(
            id = "deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 15, 0),
            ),
        )
        val timeBlock = task(
            id = "time-block",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 13, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 14, 0),
            ),
        )

        val result = classifier.classify(listOf(deadline, timeBlock), now)

        assertEquals(listOf(TodayItem(timeBlock), TodayItem(deadline)), result.next)
        assertTrue(result.now.isEmpty())
    }

    @Test
    fun deadlineAtNowIsNextBecauseItIsDueButNotOverdueYet() {
        val task = task(
            id = "deadline-now",
            schedule = TaskSchedule.Deadline(dueAt = now),
        )

        val result = classifier.classify(listOf(task), now)

        assertEquals(listOf(TodayItem(task)), result.next)
    }

    @Test
    fun completedTasksAreExcludedFromNowAndNext() {
        val task = task(
            id = "completed-block",
            status = TaskStatus.COMPLETED,
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 11, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 13, 0),
            ),
        )

        val result = classifier.classify(listOf(task), now)

        assertTrue(result.now.isEmpty())
        assertTrue(result.next.isEmpty())
    }

    @Test
    fun pastDeadlinesPastTimeBlocksAndFutureDaySchedulesAreExcluded() {
        val tasks = listOf(
            task(
                id = "past-deadline",
                schedule = TaskSchedule.Deadline(
                    dueAt = LocalDateTime.of(2026, 8, 31, 11, 0),
                ),
            ),
            task(
                id = "past-time-block",
                schedule = TaskSchedule.TimeBlock(
                    startsAt = LocalDateTime.of(2026, 8, 31, 9, 0),
                    endsAt = LocalDateTime.of(2026, 8, 31, 10, 0),
                ),
            ),
            task(
                id = "tomorrow-deadline",
                schedule = TaskSchedule.Deadline(
                    dueAt = LocalDateTime.of(2026, 9, 1, 9, 0),
                ),
            ),
            task(
                id = "tomorrow-time-block",
                schedule = TaskSchedule.TimeBlock(
                    startsAt = LocalDateTime.of(2026, 9, 1, 9, 0),
                    endsAt = LocalDateTime.of(2026, 9, 1, 10, 0),
                ),
            ),
        )

        val result = classifier.classify(tasks, now)

        assertTrue(result.now.isEmpty())
        assertTrue(result.next.isEmpty())
    }

    @Test
    fun classifierLeavesAnytimeIntervalAndRecurrenceForLaterPolicies() {
        val tasks = listOf(
            task(
                id = "all-day",
                schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 8, 31)),
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
                        startsOn = LocalDate.of(2026, 8, 31),
                    ),
                ),
            ),
            task(
                id = "recurrence",
                schedule = TaskSchedule.CalendarRecurrence(
                    rule = RecurrenceRule.daily(startsOn = LocalDate.of(2026, 8, 31)),
                ),
            ),
            task(id = "unscheduled"),
        )

        val result = classifier.classify(tasks, now)

        assertTrue(result.now.isEmpty())
        assertTrue(result.next.isEmpty())
    }

    @Test
    fun nowNextItemsCanPopulateTodaySnapshotSections() {
        val nowTask = task(
            id = "now",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 11, 0),
                endsAt = LocalDateTime.of(2026, 8, 31, 13, 0),
            ),
        )
        val nextTask = task(
            id = "next",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 16, 0),
            ),
        )
        val result = classifier.classify(listOf(nowTask, nextTask), now)

        val snapshot = TodaySnapshot(
            date = now.toLocalDate(),
            now = result.now,
            next = result.next,
        )

        assertEquals(listOf(nowTask.id), snapshot.now.map { item -> item.taskId })
        assertEquals(listOf(nextTask.id), snapshot.next.map { item -> item.taskId })
        assertEquals(2, snapshot.unresolvedCount)
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
