package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "priority")
    val priority: String,
    @ColumnInfo(name = "schedule_kind")
    val scheduleKind: String? = null,
    @ColumnInfo(name = "all_day_date")
    val allDayDate: String? = null,
    @ColumnInfo(name = "deadline_at")
    val deadlineAt: String? = null,
    @ColumnInfo(name = "time_block_starts_at")
    val timeBlockStartsAt: String? = null,
    @ColumnInfo(name = "time_block_ends_at")
    val timeBlockEndsAt: String? = null,
    @ColumnInfo(name = "interval_every_millis")
    val intervalEveryMillis: Long? = null,
    @ColumnInfo(name = "interval_window_starts_at")
    val intervalWindowStartsAt: String? = null,
    @ColumnInfo(name = "interval_window_ends_at")
    val intervalWindowEndsAt: String? = null,
    @ColumnInfo(name = "interval_eligible_days")
    val intervalEligibleDays: String? = null,
    @ColumnInfo(name = "interval_starts_on")
    val intervalStartsOn: String? = null,
    @ColumnInfo(name = "interval_ends_on")
    val intervalEndsOn: String? = null,
    @ColumnInfo(name = "interval_anchor")
    val intervalAnchor: String? = null,
    @ColumnInfo(name = "recurrence_frequency")
    val recurrenceFrequency: String? = null,
    @ColumnInfo(name = "recurrence_every")
    val recurrenceEvery: Int? = null,
    @ColumnInfo(name = "recurrence_starts_on")
    val recurrenceStartsOn: String? = null,
    @ColumnInfo(name = "recurrence_ends_on")
    val recurrenceEndsOn: String? = null,
    @ColumnInfo(name = "recurrence_days_of_week")
    val recurrenceDaysOfWeek: String? = null,
    @ColumnInfo(name = "reminder_mode")
    val reminderMode: String = "NORMAL",
    @ColumnInfo(name = "realert_interval_millis")
    val realertIntervalMillis: Long? = null,
    @ColumnInfo(name = "maximum_realerts")
    val maximumRealerts: Int = 0,
    @ColumnInfo(name = "snooze_allowed")
    val snoozeAllowed: Boolean = true,
    @ColumnInfo(name = "reschedule_allowed")
    val rescheduleAllowed: Boolean = true,
    @ColumnInfo(name = "project_id")
    val projectId: String? = null,
    @ColumnInfo(name = "provenance_origin_type")
    val provenanceOriginType: String? = null,
    @ColumnInfo(name = "provenance_rule_id")
    val provenanceRuleId: String? = null,
    @ColumnInfo(name = "provenance_version")
    val provenanceVersion: Long? = null,
    @ColumnInfo(name = "subtasks_json")
    val subtasksJson: String? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "recurrence_time")
    val recurrenceTime: String? = null,
    @ColumnInfo(name = "description")
    val description: String? = null,
)
