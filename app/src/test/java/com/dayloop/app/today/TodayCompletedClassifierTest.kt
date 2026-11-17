package com.dayloop.app.today

import com.dayloop.app.core.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TodayCompletedClassifierTest {
    private val classifier = TodayCompletedClassifier()
    private val at = Instant.parse("2026-09-20T23:30:00Z")
    private val utc = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 9, 20)
    private val task = Task(TaskId("task"), "Read")

    @Test fun lateAndEarlyCompletionUseActualDateNotSchedule() {
        for (scheduledDate in listOf(date.minusDays(1), date.plusDays(1))) {
            val completed = task.copy(schedule = TaskSchedule.AllDay(scheduledDate)).markCompleted(at)
            assertEquals(listOf(TodayItem(completed)), classifier.classify(listOf(completed), date, utc))
            assertTrue(classifier.classify(listOf(completed), scheduledDate, utc).isEmpty())
        }
    }

    @Test fun unscheduledAndRepeatingTasksCanHaveCompletionDates() {
        val schedules = listOf(null, TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(startsOn = date)))
        for (schedule in schedules) {
            val completed = task.copy(schedule = schedule).markCompleted(at)
            assertEquals(1, classifier.classify(listOf(completed), date, utc).size)
        }
    }

    @Test fun crossMidnightBlockCountsOnce() {
        val completed = task.copy(schedule = TaskSchedule.TimeBlock(
            date.minusDays(1).atTime(23, 0), date.atTime(1, 0)
        )).markCompleted(at)
        assertEquals(1, classifier.classify(listOf(completed), date, utc).size)
        assertTrue(classifier.classify(listOf(completed), date.minusDays(1), utc).isEmpty())
    }

    @Test fun viewerTimezoneDeterminesCompletionDay() {
        val completed = task.markCompleted(at)
        val lagos = ZoneId.of("Africa/Lagos")
        assertTrue(classifier.classify(listOf(completed), date, lagos).isEmpty())
        assertEquals(1, classifier.classify(listOf(completed), date.plusDays(1), lagos).size)
    }

    @Test fun repeatedDstHourStaysOnOneCalendarDay() {
        val zone = ZoneId.of("America/New_York")
        val day = LocalDate.of(2026, 11, 1)
        for (instant in listOf("2026-11-01T05:30:00Z", "2026-11-01T06:30:00Z")) {
            assertEquals(1, classifier.classify(listOf(task.markCompleted(Instant.parse(instant))), day, zone).size)
        }
    }

    @Test fun legacyUnknownAndReopenedTasksAreNotDatedCompletions() {
        val legacy = task.copy(status = TaskStatus.COMPLETED, schedule = TaskSchedule.AllDay(date))
        assertTrue(classifier.classify(listOf(legacy, task.markCompleted(at).reopen()), date, utc).isEmpty())
        assertEquals(TaskStatus.COMPLETED, legacy.status)
    }

    @Test fun builderUsesCompletionTimestamp() {
        val snapshot = TodaySnapshotBuilder.build(listOf(task.markCompleted(at)), TodayMoment(at, utc))
        assertEquals(TodayProgress(resolvedCount = 1, requiredCount = 1), snapshot.progress)
        assertEquals(TodayDayStatus.COMPLETE, snapshot.dayStatus)
    }
}
