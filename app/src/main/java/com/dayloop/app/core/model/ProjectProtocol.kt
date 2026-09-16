package com.dayloop.app.core.model

import java.time.LocalDate

sealed interface ProjectProtocol {
    val anchorDate: LocalDate
}

data class SimplePhaseCycle(
    override val anchorDate: LocalDate,
    val activeDays: Int,
    val restDays: Int,
    val taskTemplate: TaskTemplate,
) : ProjectProtocol {
    init {
        require(activeDays > 0) { "Active days must be positive." }
        require(restDays >= 0) { "Rest days must not be negative." }
    }

    val totalDays: Int get() = activeDays + restDays

    fun getStatusAt(date: LocalDate): ProtocolDayStatus {
        if (date.isBefore(anchorDate)) return ProtocolDayStatus.BEFORE_START
        
        val elapsed = java.time.temporal.ChronoUnit.DAYS.between(anchorDate, date).toInt()
        val position = elapsed % totalDays
        
        return if (position < activeDays) {
            ProtocolDayStatus.ACTIVE(dayOfPhase = position + 1)
        } else {
            ProtocolDayStatus.REST(dayOfPhase = position - activeDays + 1)
        }
    }
}

data class TaskTemplate(
    val title: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val reminderPolicy: ReminderPolicy = ReminderPolicy.normal(),
)

sealed interface ProtocolDayStatus {
    object BEFORE_START : ProtocolDayStatus
    data class ACTIVE(val dayOfPhase: Int) : ProtocolDayStatus
    data class REST(val dayOfPhase: Int) : ProtocolDayStatus
}
