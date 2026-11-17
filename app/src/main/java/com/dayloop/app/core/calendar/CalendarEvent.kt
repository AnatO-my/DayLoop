package com.dayloop.app.core.calendar

data class ExternalCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int,
    val isPrimary: Boolean
)

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String?,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isAllDay: Boolean,
    val location: String?,
    val color: Int?
)
