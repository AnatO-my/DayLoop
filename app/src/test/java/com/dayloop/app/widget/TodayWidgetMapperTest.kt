package com.dayloop.app.widget

import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.today.TodayItem
import com.dayloop.app.today.TodaySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodayWidgetMapperTest {

    @Test
    fun `anytime tasks fill both agenda slots with accurate labels`() {
        val first = TodayItem(Task(TaskId("a"), "Read"))
        val second = TodayItem(Task(TaskId("b"), "Write"))
        val state = TodaySnapshot(date = LocalDate.of(2026, 9, 19), anytime = listOf(first, second)).toWidgetState()
        assertEquals("ANYTIME", state.nowTask?.section)
        assertEquals("ANYTIME", state.nextTask?.section)
        assertEquals("b", state.nextTask?.id?.value)
    }

    @Test
    fun `overdue is not relabeled now and completion count is retained`() {
        val task = TodayItem(Task(TaskId("a"), "Read"))
        val state = TodaySnapshot(date = LocalDate.of(2026, 9, 19), overdue = listOf(task),
            completed = listOf(TodayItem(Task(TaskId("b"), "Write").markCompleted(java.time.Instant.EPOCH)))).toWidgetState()
        assertEquals("OVERDUE", state.nowTask?.section)
        assertEquals(1, state.completedCount)
    }

    @Test
    fun `maps empty snapshot to empty widget state`() {
        val date = LocalDate.of(2026, 9, 7)
        val snapshot = TodaySnapshot(date = date)
        
        val state = snapshot.toWidgetState()
        
        assertEquals(0, state.unresolvedCount)
        assertEquals(null, state.nowTask)
        assertEquals(null, state.nextTask)
        assertEquals("Mon, Sep 7", state.dateLabel)
    }

    @Test
    fun `projects count and first two tasks as now and next`() {
        val task1 = Task(id = TaskId("1"), title = "Now Task")
        val task2 = Task(id = TaskId("2"), title = "Next Task")
        val task3 = Task(id = TaskId("3"), title = "Other Task")
        
        val snapshot = TodaySnapshot(
            date = LocalDate.of(2026, 9, 7),
            overdue = listOf(TodayItem(task1)),
            now = listOf(TodayItem(task2)),
            next = listOf(TodayItem(task3))
        )
        
        val state = snapshot.toWidgetState()
        
        assertEquals(3, state.unresolvedCount)
        assertEquals("Now Task", state.nowTask?.title)
        assertEquals("Next Task", state.nextTask?.title)
    }
}
