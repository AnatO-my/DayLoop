package com.dayloop.app.core.recurrence

import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayMoment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RecurrenceOccurrenceGeneratorTest {
    private val generator = RecurrenceOccurrenceGenerator()
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 20)
    private val moment = TodayMoment(today.atStartOfDay(zone).toInstant(), zone)

    @Test
    fun `generates all-day task when no local time is specified`() {
        val master = Task(
            id = TaskId("habit"),
            title = "Drink water",
            schedule = TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(today))
        )
        
        val instances = generator.generateFor(listOf(master), moment)
        
        assertEquals(1, instances.size)
        val instance = instances.first()
        assertEquals("generated:recurrence:habit:2026-09-20", instance.id.value)
        assertEquals(TaskSchedule.AllDay(today), instance.schedule)
    }

    @Test
    fun `generates deadline task when local time is specified`() {
        val time = LocalTime.of(9, 0)
        val master = Task(
            id = TaskId("habit"),
            title = "Gym",
            schedule = TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(today), time)
        )
        
        val instances = generator.generateFor(listOf(master), moment)
        
        assertEquals(1, instances.size)
        val instance = instances.first()
        assertEquals(TaskSchedule.Deadline(today.atTime(time)), instance.schedule)
    }

    @Test
    fun `does not generate task if not eligible for today`() {
        val master = Task(
            id = TaskId("habit"),
            title = "Gym",
            schedule = TaskSchedule.CalendarRecurrence(RecurrenceRule.daily(today.plusDays(1)))
        )
        
        val instances = generator.generateFor(listOf(master), moment)
        
        assertEquals(0, instances.size)
    }
}
