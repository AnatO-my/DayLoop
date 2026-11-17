package com.dayloop.app.repository

import com.dayloop.app.core.calendar.CalendarContractManager
import com.dayloop.app.core.calendar.CalendarEvent
import com.dayloop.app.core.calendar.ExternalCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CalendarSyncRepository {
    fun hasPermission(): Boolean
    fun getAvailableCalendars(): List<ExternalCalendar>
    fun observeSelectedCalendarIds(): StateFlow<Set<Long>>
    fun toggleCalendarSelected(calendarId: Long, selected: Boolean)
    fun getEventsForRange(startTimestamp: Long, endTimestamp: Long): List<CalendarEvent>
    fun exportTaskToCalendar(
        calendarId: Long,
        title: String,
        description: String?,
        startTimestamp: Long,
        endTimestamp: Long
    ): Boolean
}

class AndroidCalendarSyncRepository(
    private val calendarManager: CalendarContractManager
) : CalendarSyncRepository {

    private val _selectedCalendarIds = MutableStateFlow<Set<Long>>(emptySet())

    override fun hasPermission(): Boolean {
        return calendarManager.hasCalendarPermission()
    }

    override fun getAvailableCalendars(): List<ExternalCalendar> {
        val available = calendarManager.getAvailableCalendars()
        // If selected set is empty, auto-select primary/first available calendars
        if (_selectedCalendarIds.value.isEmpty() && available.isNotEmpty()) {
            val primaryIds = available.filter { it.isPrimary }.map { it.id }.toSet()
            _selectedCalendarIds.value = if (primaryIds.isNotEmpty()) primaryIds else setOf(available.first().id)
        }
        return available
    }

    override fun observeSelectedCalendarIds(): StateFlow<Set<Long>> {
        return _selectedCalendarIds.asStateFlow()
    }

    override fun toggleCalendarSelected(calendarId: Long, selected: Boolean) {
        val current = _selectedCalendarIds.value.toMutableSet()
        if (selected) {
            current.add(calendarId)
        } else {
            current.remove(calendarId)
        }
        _selectedCalendarIds.value = current
    }

    override fun getEventsForRange(startTimestamp: Long, endTimestamp: Long): List<CalendarEvent> {
        val activeIds = _selectedCalendarIds.value
        if (activeIds.isEmpty()) return emptyList()
        return calendarManager.getEventsForTimeRange(startTimestamp, endTimestamp, activeIds)
    }

    override fun exportTaskToCalendar(
        calendarId: Long,
        title: String,
        description: String?,
        startTimestamp: Long,
        endTimestamp: Long
    ): Boolean {
        val resultUri = calendarManager.exportTaskToCalendar(
            calendarId = calendarId,
            title = title,
            description = description,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
        return resultUri != null
    }
}
