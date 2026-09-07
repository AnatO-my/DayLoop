package com.dayloop.app.today

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class TodayMoment(
    val instant: Instant,
    val zoneId: ZoneId,
) {
    private val zonedDateTime = instant.atZone(zoneId)

    val date: LocalDate = zonedDateTime.toLocalDate()
    val localDateTime: LocalDateTime = zonedDateTime.toLocalDateTime()
}
