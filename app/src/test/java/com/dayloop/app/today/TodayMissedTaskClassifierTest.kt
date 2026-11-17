package com.dayloop.app.today

import com.dayloop.app.core.model.*
import com.dayloop.app.core.recurrence.RecurrenceOccurrenceGenerator
import com.dayloop.app.widget.toWidgetState
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TodayMissedTaskClassifierTest {
    private val now = LocalDateTime.of(2026, 9, 20, 12, 0)
    private val today = now.toLocalDate()
    private val moment = TodayMoment(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val classifier = TodayMissedTaskClassifier()
    private fun task(id: String, schedule: TaskSchedule?) = Task(TaskId(id), id, schedule = schedule)

    @Test fun includesOldAllDayPastDeadlineAndEndedBlock() {
        val tasks = listOf(
            task("old", TaskSchedule.AllDay(today.minusDays(1))),
            task("due", TaskSchedule.Deadline(now.minusMinutes(1))),
            task("ended", TaskSchedule.TimeBlock(now.minusHours(1), now)),
        )
        assertEquals(tasks.map { it.id }, classifier.classify(tasks, now, today).map { it.taskId })
    }

    @Test fun excludesCurrentFutureInboxAndInactiveWork() {
        val old = task("old", TaskSchedule.AllDay(today.minusDays(1)))
        val tasks = listOf(
            task("today", TaskSchedule.AllDay(today)),
            task("future", TaskSchedule.AllDay(today.plusDays(1))),
            task("due-now", TaskSchedule.Deadline(now)),
            task("ongoing", TaskSchedule.TimeBlock(now.minusHours(1), now.plusMinutes(1))),
            task("inbox", null), old.skip(), old.moveToSomeday(), old.markCompleted(moment.instant),
            task("series", TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(today.minusDays(10))))
        )
        assertTrue(classifier.classify(tasks, now, today).isEmpty())
    }

    @Test fun overdueRemainsVisibleAndCountsOnceAcrossReviewAndWidget() {
        val due = task("due", TaskSchedule.Deadline(now.minusMinutes(1)))
        val old = task("old", TaskSchedule.AllDay(today.minusDays(1)))
        val done = task("done", null).markCompleted(moment.instant)
        val snapshot = TodaySnapshotBuilder.build(listOf(due, old, done), moment)
        assertEquals(listOf(due.id), snapshot.overdue.map { it.taskId })
        assertEquals(listOf(old.id), snapshot.itemsIn(TodaySection.MISSED).map { it.taskId })
        assertEquals(2, snapshot.missed.size)
        assertEquals(2, snapshot.unresolvedCount)
        assertEquals(3, snapshot.totalCount)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
        assertEquals(2, snapshot.toWidgetState().unresolvedCount)
    }

    @Test fun missedOnlyIsNotAnEmptyOrCompleteDay() {
        val snapshot = TodaySnapshotBuilder.build(listOf(task("old", TaskSchedule.AllDay(today.minusDays(1)))), moment)
        assertFalse(snapshot.isEmpty)
        assertEquals(1, snapshot.progress.remainingCount)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
    }

    @Test fun somedayOverrideSuppressesGeneratedOccurrenceWithoutChangingSeries() {
        val master = task("series", TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(today), LocalTime.of(9, 0)))
        val generated = RecurrenceOccurrenceGenerator().generateFor(listOf(master), moment).single()
        assertEquals(1, TodaySnapshotBuilder.build(listOf(master), moment).missed.size)
        val snapshot = TodaySnapshotBuilder.build(listOf(master, generated.moveToSomeday()), moment)
        assertTrue(snapshot.missed.isEmpty())
        assertEquals(0, snapshot.unresolvedCount)
        assertEquals(TaskStatus.ACTIVE, master.status)
    }

    @Test fun futureRescheduleLeavesReview() {
        val old = task("old", TaskSchedule.AllDay(today.minusDays(1)))
        assertTrue(TodaySnapshotBuilder.build(listOf(old.copy(schedule = TaskSchedule.AllDay(today.plusDays(1)))), moment).missed.isEmpty())
    }
}
