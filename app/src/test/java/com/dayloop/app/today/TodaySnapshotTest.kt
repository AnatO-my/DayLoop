package com.dayloop.app.today

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TodaySnapshotTest {
    @Test
    fun emptySnapshotKeepsLocalDateAndHasNoWork() {
        val snapshot = TodaySnapshot(date = LocalDate.of(2026, 8, 31))

        assertEquals(LocalDate.of(2026, 8, 31), snapshot.date)
        assertTrue(snapshot.isEmpty)
        assertEquals(0, snapshot.unresolvedCount)
        assertEquals(0, snapshot.completedCount)
        assertEquals(0, snapshot.totalCount)
        assertEquals(TodayProgress(resolvedCount = 0, requiredCount = 0), snapshot.progress)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
        assertEquals(emptyList<TodayItem>(), snapshot.unresolved)
    }

    @Test
    fun snapshotKeepsSectionsInTodayDisplayOrder() {
        assertEquals(
            listOf(
                TodaySection.OVERDUE,
                TodaySection.NOW,
                TodaySection.NEXT,
                TodaySection.ANYTIME,
                TodaySection.COMPLETED,
                TodaySection.MISSED,
            ),
            TodaySnapshot.sectionsInDisplayOrder,
        )
    }

    @Test
    fun unresolvedCombinesOverdueNowNextAndAnytimeWithoutCompleted() {
        val overdue = item("overdue")
        val now = item("now")
        val next = item("next")
        val anytime = item("anytime")
        val completed = item("completed", status = TaskStatus.COMPLETED)

        val snapshot = TodaySnapshot(
            date = LocalDate.of(2026, 8, 31),
            overdue = listOf(overdue),
            now = listOf(now),
            next = listOf(next),
            anytime = listOf(anytime),
            completed = listOf(completed),
        )

        assertEquals(listOf(overdue, now, next, anytime), snapshot.unresolved)
        assertEquals(4, snapshot.unresolvedCount)
        assertEquals(1, snapshot.completedCount)
        assertEquals(5, snapshot.totalCount)
        assertEquals(TodayProgress(resolvedCount = 1, requiredCount = 5), snapshot.progress)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
        assertFalse(snapshot.isEmpty)
        assertEquals(listOf(completed), snapshot.itemsIn(TodaySection.COMPLETED))
    }

    @Test
    fun completedOnlySnapshotIsDayComplete() {
        val completed = item("completed", status = TaskStatus.COMPLETED)

        val snapshot = TodaySnapshot(
            date = LocalDate.of(2026, 8, 31),
            completed = listOf(completed),
        )

        assertEquals(0, snapshot.unresolvedCount)
        assertEquals(1, snapshot.completedCount)
        assertEquals(TodayProgress(resolvedCount = 1, requiredCount = 1), snapshot.progress)
        assertEquals(TodayDayStatus.COMPLETE, snapshot.dayStatus)
    }

    @Test
    fun addingUnresolvedWorkReopensTheDay() {
        val active = item("active")
        val completed = item("completed", status = TaskStatus.COMPLETED)

        val snapshot = TodaySnapshot(
            date = LocalDate.of(2026, 8, 31),
            anytime = listOf(active),
            completed = listOf(completed),
        )

        assertEquals(TodayProgress(resolvedCount = 1, requiredCount = 2), snapshot.progress)
        assertEquals(1, snapshot.progress.remainingCount)
        assertEquals(TodayDayStatus.OPEN, snapshot.dayStatus)
    }

    @Test
    fun completedTaskCannotAppearInUnresolvedSections() {
        assertThrows(IllegalArgumentException::class.java) {
            TodaySnapshot(
                date = LocalDate.of(2026, 8, 31),
                now = listOf(item("task-1", status = TaskStatus.COMPLETED)),
            )
        }
    }

    @Test
    fun activeTaskCannotAppearInCompletedSection() {
        assertThrows(IllegalArgumentException::class.java) {
            TodaySnapshot(
                date = LocalDate.of(2026, 8, 31),
                completed = listOf(item("task-1")),
            )
        }
    }

    @Test
    fun taskCanAppearInOnlyOneTodaySection() {
        val duplicate = item("task-1")

        assertThrows(IllegalArgumentException::class.java) {
            TodaySnapshot(
                date = LocalDate.of(2026, 8, 31),
                now = listOf(duplicate),
                next = listOf(duplicate),
            )
        }
    }

    @Test
    fun progressRejectsInvalidCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            TodayProgress(resolvedCount = -1, requiredCount = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TodayProgress(resolvedCount = 1, requiredCount = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TodayProgress(resolvedCount = 2, requiredCount = 1)
        }
    }

    private fun item(
        id: String,
        status: TaskStatus = TaskStatus.ACTIVE,
    ): TodayItem = TodayItem(
        task = Task(
            id = TaskId(id),
            title = "Task $id",
            status = status,
        ),
    )
}
