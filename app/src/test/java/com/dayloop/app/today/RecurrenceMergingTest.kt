package com.dayloop.app.today

import com.dayloop.app.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecurrenceMergingTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 9, 20)
    private val moment = TodayMoment(date.atStartOfDay(zone).toInstant(), zone)
    
    private val masterId = TaskId("habit")
    private val master = Task(
        id = masterId,
        title = "Drink water",
        schedule = TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(date))
    )
    
    private val instanceId = TaskId("generated:recurrence:habit:2026-09-20")
    private val generatedInstance = Task(
        id = instanceId,
        title = "Drink water",
        schedule = TaskSchedule.AllDay(date),
        provenance = TaskProvenance("RECURRENCE_RULE", masterId.value, 1)
    )

    @Test
    fun `stored completed instance replaces generated instance`() {
        val completedInstance = generatedInstance.markCompleted(moment.instant)
        
        // Passing both master and completed instance
        val result = TodaySnapshotBuilder.build(listOf(master, completedInstance), moment)
        
        assertEquals(1, result.totalCount)
        assertEquals(1, result.completedCount)
        assertEquals(instanceId, result.completed.first().taskId)
    }

    @Test
    fun `stored skipped instance replaces generated instance`() {
        val skippedInstance = generatedInstance.skip()
        
        val result = TodaySnapshotBuilder.build(listOf(master, skippedInstance), moment)
        
        assertEquals(0, result.unresolvedCount)
        assertEquals(0, result.completedCount)
        // Skipped tasks are hidden from snapshots (which only have unresolved and completed)
    }
}
