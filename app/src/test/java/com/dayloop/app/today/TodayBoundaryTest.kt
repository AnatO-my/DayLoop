package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayBoundaryTest {
    private val anytimeClassifier = TodayAnytimeClassifier()
    private val overdueClassifier = TodayOverdueClassifier()
    private val nowNextClassifier = TodayNowNextClassifier()

    @Test
    fun sameInstantCanProduceDifferentTodayDatesByZone() {
        val instant = Instant.parse("2026-09-01T00:30:00Z")
        val lagosMoment = TodayMoment(instant, ZoneId.of("Africa/Lagos"))
        val losAngelesMoment = TodayMoment(instant, ZoneId.of("America/Los_Angeles"))
        val augustTask = task(
            id = "august-31",
            schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 8, 31)),
        )
        val septemberTask = task(
            id = "september-1",
            schedule = TaskSchedule.AllDay(date = LocalDate.of(2026, 9, 1)),
        )

        assertEquals(LocalDate.of(2026, 9, 1), lagosMoment.date)
        assertEquals(LocalDateTime.of(2026, 9, 1, 1, 30), lagosMoment.localDateTime)
        assertEquals(LocalDate.of(2026, 8, 31), losAngelesMoment.date)
        assertEquals(
            listOf(septemberTask.id),
            anytimeClassifier.classify(listOf(augustTask, septemberTask), lagosMoment.date)
                .map { item -> item.taskId },
        )
        assertEquals(
            listOf(augustTask.id),
            anytimeClassifier.classify(listOf(augustTask, septemberTask), losAngelesMoment.date)
                .map { item -> item.taskId },
        )
    }

    @Test
    fun overdueDeadlineUsesLocalTimeAfterMidnight() {
        val moment = TodayMoment(
            instant = Instant.parse("2026-08-31T23:01:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )
        val task = task(
            id = "late-deadline",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 8, 31, 23, 59),
            ),
        )

        assertEquals(LocalDateTime.of(2026, 9, 1, 0, 1), moment.localDateTime)
        assertEquals(listOf(TodayItem(task)), overdueClassifier.classify(listOf(task), moment.localDateTime))
    }

    @Test
    fun deadlineAtLocalMidnightIsNextNotOverdue() {
        val moment = TodayMoment(
            instant = Instant.parse("2026-08-31T23:00:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )
        val task = task(
            id = "midnight-deadline",
            schedule = TaskSchedule.Deadline(dueAt = moment.localDateTime),
        )

        val nowNext = nowNextClassifier.classify(listOf(task), moment.localDateTime)

        assertTrue(overdueClassifier.classify(listOf(task), moment.localDateTime).isEmpty())
        assertEquals(listOf(TodayItem(task)), nowNext.next)
    }

    @Test
    fun timeBlockEndingAtLocalMidnightIsNotNowAtBoundary() {
        val moment = TodayMoment(
            instant = Instant.parse("2026-08-31T23:00:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )
        val task = task(
            id = "ended-at-midnight",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 23, 0),
                endsAt = LocalDateTime.of(2026, 9, 1, 0, 0),
            ),
        )

        assertTrue(nowNextClassifier.classify(listOf(task), moment.localDateTime).now.isEmpty())
    }

    @Test
    fun crossMidnightTimeBlockIsNowAfterLocalMidnight() {
        val moment = TodayMoment(
            instant = Instant.parse("2026-08-31T23:30:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )
        val task = task(
            id = "cross-midnight",
            schedule = TaskSchedule.TimeBlock(
                startsAt = LocalDateTime.of(2026, 8, 31, 23, 0),
                endsAt = LocalDateTime.of(2026, 9, 1, 1, 0),
            ),
        )

        assertEquals(
            listOf(TodayItem(task)),
            nowNextClassifier.classify(listOf(task), moment.localDateTime).now,
        )
    }

    @Test
    fun emptyBoundarySnapshotRemainsOpen() {
        val moment = TodayMoment(
            instant = Instant.parse("2026-08-31T23:00:00Z"),
            zoneId = ZoneId.of("Africa/Lagos"),
        )

        val snapshot = TodaySnapshot(date = moment.date)

        assertEquals(TodayProgress(resolvedCount = 0, requiredCount = 0), snapshot.progress)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
        assertTrue(snapshot.isEmpty)
    }

    private fun task(
        id: String,
        schedule: TaskSchedule? = null,
    ): Task = Task(
        id = TaskId(id),
        title = "Task $id",
        schedule = schedule,
    )
}
