package com.dayloop.app.core.projects

import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayDayStatus
import com.dayloop.app.today.TodayMoment
import com.dayloop.app.today.TodaySnapshotBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProjectScenarioTest {

    private val generator = ProjectOccurrenceGenerator()
    private val zone = ZoneId.of("UTC")
    private val anchor = LocalDate.of(2026, 9, 1) // Day 1 Active

    private val protocol = SimplePhaseCycle(
        anchorDate = anchor,
        activeDays = 3,
        restDays = 3,
        taskTemplate = TaskTemplate("Study Circuits")
    )

    private val project = Project(
        id = ProjectId("eee-203"),
        title = "EEE 203 Spaced Study",
        status = ProjectStatus.ACTIVE,
        protocol = protocol
    )

    @Test
    fun `verifies 3-3 study-rest cycle across 7 days`() {
        val days = listOf(
            ScenarioDay(offset = 0, expectTask = true, expectedStatus = TodayDayStatus.OPEN),
            ScenarioDay(offset = 1, expectTask = true, expectedStatus = TodayDayStatus.OPEN),
            ScenarioDay(offset = 2, expectTask = true, expectedStatus = TodayDayStatus.OPEN),
            ScenarioDay(offset = 3, expectTask = false, expectedStatus = TodayDayStatus.COMPLETE),
            ScenarioDay(offset = 4, expectTask = false, expectedStatus = TodayDayStatus.COMPLETE),
            ScenarioDay(offset = 5, expectTask = false, expectedStatus = TodayDayStatus.COMPLETE),
            ScenarioDay(offset = 6, expectTask = true, expectedStatus = TodayDayStatus.OPEN)
        )

        days.forEach { day ->
            val date = anchor.plusDays(day.offset.toLong())
            val moment = TodayMoment(date.atStartOfDay(zone).toInstant(), zone)
            
            val generated = generator.generateFor(listOf(project), moment)
            
            assertEquals("Day ${day.offset} task count mismatch", 
                if (day.expectTask) 1 else 0, generated.size)
            
            if (day.expectTask) {
                assertEquals("Study Circuits", generated.first().title)
            }

            val snapshot = TodaySnapshotBuilder.build(
                tasks = emptyList(),
                moment = moment,
                generatedTasks = generated
            )

            assertEquals("Day ${day.offset} Today status mismatch", 
                day.expectedStatus, snapshot.dayStatus)
        }
    }

    private data class ScenarioDay(
        val offset: Int,
        val expectTask: Boolean,
        val expectedStatus: TodayDayStatus
    )
}
