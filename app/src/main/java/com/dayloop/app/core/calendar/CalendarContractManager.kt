package com.dayloop.app.core.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

class CalendarContractManager(
    private val context: Context
) {
    fun hasCalendarPermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        val writePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        return readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
    }

    fun getAvailableCalendars(): List<ExternalCalendar> {
        if (!hasCalendarPermission()) return emptyList()

        val calendars = mutableListOf<ExternalCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            val primaryIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: "Calendar"
                val accountName = cursor.getString(accountIdx) ?: "Local"
                val color = cursor.getInt(colorIdx)
                val isPrimary = cursor.getInt(primaryIdx) == 1

                calendars.add(
                    ExternalCalendar(
                        id = id,
                        name = name,
                        accountName = accountName,
                        color = color,
                        isPrimary = isPrimary
                    )
                )
            }
        }

        return calendars
    }

    fun getEventsForTimeRange(
        startTimestamp: Long,
        endTimestamp: Long,
        calendarIds: Set<Long>
    ): List<CalendarEvent> {
        if (!hasCalendarPermission() || calendarIds.isEmpty()) return emptyList()

        val events = mutableListOf<CalendarEvent>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startTimestamp)
        ContentUris.appendId(builder, endTimestamp)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR
        )

        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"

        context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)

            while (cursor.moveToNext()) {
                events.add(
                    CalendarEvent(
                        id = cursor.getLong(idIdx),
                        calendarId = cursor.getLong(calIdIdx),
                        title = cursor.getString(titleIdx) ?: "Untitled Event",
                        description = cursor.getString(descIdx),
                        startTimestamp = cursor.getLong(beginIdx),
                        endTimestamp = cursor.getLong(endIdx),
                        isAllDay = cursor.getInt(allDayIdx) == 1,
                        location = cursor.getString(locIdx),
                        color = cursor.getInt(colorIdx)
                    )
                )
            }
        }

        return events
    }

    fun exportTaskToCalendar(
        calendarId: Long,
        title: String,
        description: String?,
        startTimestamp: Long,
        endTimestamp: Long
    ): Uri? {
        if (!hasCalendarPermission()) return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description ?: "")
            put(CalendarContract.Events.DTSTART, startTimestamp)
            put(CalendarContract.Events.DTEND, endTimestamp)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    }
}

private object ContentUris {
    fun appendId(builder: Uri.Builder, id: Long): Uri.Builder {
        return builder.appendPath(id.toString())
    }
}
