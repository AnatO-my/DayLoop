package com.dayloop.app.core.projects

import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ProjectOccurrenceGeneratorTest {

    private val generator = ProjectOccurrenceGenerator()
    private val zone = ZoneId.of("UTC")
    private val anchor = LocalDate.of(2026, 9, 1)
    
    private val protocol = SimplePhaseCycle(
        anchorDate = anchor,
        activeDays = 3,
        restDays = 3,
        taskTemplate = TaskTemplate("Study circuits")
    )
    
    private val project = Project(
        id = ProjectId("project-1"),
        title = "EEE 203",
        status = ProjectStatus.ACTIVE,
        protocol = protocol
    )

    @Test
    fun `generates task on active day`() {
        val today = anchor // Day 1 Active
        val moment = TodayMoment(today.atStartOfDay(zone).toInstant(), zone)
        
        val generated = generator.generateFor(listOf(project), moment)
        
        assertEquals(1, generated.size)
        val task = generated.first()
        assertEquals("Study circuits", task.title)
        assertEquals(TaskId("generated:project-1:2026-09-01"), task.id)
        assertEquals(project.id, task.projectId)
        assertEquals("PROJECT_RULE", task.provenance?.originType)
    }

    @Test
    fun `generates no task on rest day`() {
        val today = anchor.plusDays(3) // Day 1 Rest
        val moment = TodayMoment(today.atStartOfDay(zone).toInstant(), zone)
        
        val generated = generator.generateFor(listOf(project), moment)
        
        assertTrue(generated.isEmpty())
    }

    @Test
    fun `generates no task if project is paused`() {
        val today = anchor
        val moment = TodayMoment(today.atStartOfDay(zone).toInstant(), zone)
        val pausedProject = project.copy(status = ProjectStatus.PAUSED)
        
        val generated = generator.generateFor(listOf(pausedProject), moment)
        
        assertTrue(generated.isEmpty())
    }
}
