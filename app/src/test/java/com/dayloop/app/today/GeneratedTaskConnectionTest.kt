package com.dayloop.app.today

import com.dayloop.app.core.model.*
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratedTaskConnectionTest {
    private val moment = TodayMoment(Instant.parse("2026-09-08T12:00:00Z"), ZoneId.of("UTC"))
    private val task = Task(TaskId("generated:project:2026-09-08"), "Study",
        schedule = TaskSchedule.AllDay(moment.date), projectId = ProjectId("project"),
        provenance = TaskProvenance("PROJECT_RULE", "SIMPLE_PHASE_CYCLE", 1))

    @Test fun savedCompletionReplacesProjectedTask() {
        val result = TodaySnapshotBuilder.build(listOf(task.markCompleted()), moment, listOf(task))
        assertEquals(1, result.totalCount)
        assertEquals(1, result.completedCount)
    }

    @Test fun obsoleteCompletionDoesNotCompleteANewProtocolVersion() {
        val updated = task.copy(provenance = task.provenance!!.copy(version = 2))
        val result = TodaySnapshotBuilder.build(listOf(task.markCompleted()), moment, listOf(updated))
        assertEquals(1, result.totalCount)
        assertEquals(0, result.completedCount)
    }
}
