package com.dayloop.app.repository

import com.dayloop.app.core.database.AlarmDiagnosticEntryEntity
import com.dayloop.app.core.database.PersistentReminderEscalationEntity
import com.dayloop.app.core.database.ScheduledOccurrenceEntity
import com.dayloop.app.core.database.TaskEntity
import com.dayloop.app.core.model.RecurrenceFrequency
import com.dayloop.app.core.model.RecurrenceRule
import com.dayloop.app.core.model.ReminderMode
import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.Task
import com.dayloop.app.core.model.TaskId
import com.dayloop.app.core.model.TaskSchedule
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceValidationTest {
    @Test
    fun persistedMonthlyShortMonthAnchorRoundTripsWithoutDateArithmetic() {
        val task = Task(
            id = TaskId("monthly-jan-31"),
            title = "Month end review",
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.monthly(
                    startsOn = LocalDate.of(2026, 1, 31),
                    endsOn = LocalDate.of(2026, 12, 31),
                ),
            ),
        )

        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun persistedLeapDayYearlyAnchorRoundTripsWithoutExpansion() {
        val task = Task(
            id = TaskId("yearly-leap-day"),
            title = "Leap day ritual",
            schedule = TaskSchedule.CalendarRecurrence(
                rule = RecurrenceRule.yearly(
                    startsOn = LocalDate.of(2028, 2, 29),
                    every = 4,
                ),
            ),
        )

        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun persistedReversedTimeBlockIsRejectedByDomainValidation() {
        val entity = baseEntity(
            scheduleKind = "TIME_BLOCK",
            timeBlockStartsAt = "2026-08-31T17:00",
            timeBlockEndsAt = "2026-08-31T09:00",
        )

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedIntervalWithNonPositiveDurationIsRejectedByDomainValidation() {
        val entity = baseIntervalEntity(intervalEveryMillis = 0L)

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedIntervalWithNoEligibleDaysIsRejectedByDomainValidation() {
        val entity = baseIntervalEntity(intervalEligibleDays = "")

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedWeeklyRecurrenceWithNoDaysIsRejectedByDomainValidation() {
        val entity = baseEntity(
            scheduleKind = "CALENDAR_RECURRENCE",
            recurrenceFrequency = RecurrenceFrequency.WEEKLY.name,
            recurrenceEvery = 1,
            recurrenceStartsOn = "2026-08-31",
            recurrenceDaysOfWeek = null,
        )

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedReminderPolicyWithIncompleteRealertConfigurationIsRejected() {
        val entity = baseEntity(
            reminderMode = ReminderMode.STRICT.name,
            realertIntervalMillis = null,
            maximumRealerts = 2,
        )

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedFocusReminderPolicyRequiresTimeBlockSchedule() {
        val entity = baseEntity(
            scheduleKind = "ALL_DAY",
            allDayDate = "2026-08-31",
            reminderMode = ReminderMode.FOCUS.name,
        )

        assertThrows(IllegalArgumentException::class.java) {
            entity.toDomain()
        }
    }

    @Test
    fun persistedStrictReminderPolicyRoundTripsWithTimeBoundaryValues() {
        val task = Task(
            id = TaskId("strict-deadline"),
            title = "Submit application",
            schedule = TaskSchedule.Deadline(
                dueAt = LocalDateTime.of(2026, 12, 31, 23, 59),
            ),
            reminderPolicy = ReminderPolicy.strict(
                realertInterval = Duration.ofMinutes(10),
                maximumRealerts = 2,
            ),
        )

        assertEquals(task, task.toEntity().toDomain())
    }
    @Test
    fun persistedScheduledOccurrenceRoundTripsAlarmIdentityFields() {
        val occurrence = ScheduledOccurrenceEntity(
            id = "occurrence-1",
            taskId = "task-1",
            fireAtEpochMillis = 1_788_680_800_000L,
            generation = 3L,
            status = "SCHEDULED",
            createdAtEpochMillis = 1_788_677_200_000L,
            updatedAtEpochMillis = 1_788_677_200_000L,
        )

        assertEquals(occurrence, occurrence.toDomain().toEntity())
    }

    @Test
    fun persistedScheduledOccurrenceWithInvalidGenerationIsRejected() {
        val occurrence = ScheduledOccurrenceEntity(
            id = "occurrence-1",
            taskId = "task-1",
            fireAtEpochMillis = 1_788_680_800_000L,
            generation = 0L,
            status = "SCHEDULED",
            createdAtEpochMillis = 1_788_677_200_000L,
            updatedAtEpochMillis = 1_788_677_200_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            occurrence.toDomain()
        }
    }

    @Test
    fun persistedAlarmDiagnosticEntryRoundTripsAlarmEvidenceFields() {
        val entry = AlarmDiagnosticEntryEntity(
            id = "diagnostic-1",
            occurrenceId = "occurrence-1",
            taskId = "task-1",
            event = "SCHEDULE_INEXACT_FALLBACK",
            exact = false,
            message = "EXACT_ALARM_PERMISSION_MISSING",
            createdAtEpochMillis = 1_788_677_200_000L,
        )

        assertEquals(entry, entry.toDomain().toEntity())
    }
    @Test
    fun persistedPersistentReminderEscalationRoundTripsStateMachineFields() {
        val escalation = PersistentReminderEscalationEntity(
            id = "escalation-1",
            taskId = "task-1",
            sourceOccurrenceId = "occurrence-1",
            nextRealertAtEpochMillis = 1_788_681_700_000L,
            realertIntervalMillis = 15 * 60 * 1000L,
            maximumRealerts = 3,
            realertsDelivered = 1,
            status = "ACTIVE",
            createdAtEpochMillis = 1_788_680_800_000L,
            updatedAtEpochMillis = 1_788_680_800_000L,
        )

        assertEquals(escalation, escalation.toDomain().toEntity())
    }

    @Test
    fun persistedActivePersistentReminderEscalationRequiresNextRealertTime() {
        val escalation = PersistentReminderEscalationEntity(
            id = "escalation-1",
            taskId = "task-1",
            sourceOccurrenceId = "occurrence-1",
            nextRealertAtEpochMillis = null,
            realertIntervalMillis = 15 * 60 * 1000L,
            maximumRealerts = 3,
            realertsDelivered = 1,
            status = "ACTIVE",
            createdAtEpochMillis = 1_788_680_800_000L,
            updatedAtEpochMillis = 1_788_680_800_000L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            escalation.toDomain()
        }
    }
    @Test
    fun exportedRoomSchemaKeepsTaskAndOccurrencePersistenceColumns() {
        val schema = readExportedSchema()

        assertTrue(schema.contains("\"version\": 4"))
        assertTrue(schema.contains("\"tableName\": \"tasks\""))
        assertTrue(schema.contains("\"columnName\": \"schedule_kind\""))
        assertTrue(schema.contains("\"columnName\": \"interval_every_millis\""))
        assertTrue(schema.contains("\"columnName\": \"recurrence_frequency\""))
        assertTrue(schema.contains("\"columnName\": \"reminder_mode\""))
        assertTrue(schema.contains("\"columnName\": \"maximum_realerts\""))
        assertTrue(schema.contains("\"tableName\": \"scheduled_occurrences\""))
        assertTrue(schema.contains("\"columnName\": \"task_id\""))
        assertTrue(schema.contains("\"columnName\": \"fire_at_epoch_millis\""))
        assertTrue(schema.contains("\"columnName\": \"generation\""))
        assertTrue(schema.contains("\"columnName\": \"status\""))
        assertTrue(schema.contains("\"tableName\": \"alarm_diagnostic_entries\""))
        assertTrue(schema.contains("\"columnName\": \"occurrence_id\""))
        assertTrue(schema.contains("\"columnName\": \"task_id\""))
        assertTrue(schema.contains("\"columnName\": \"event\""))
        assertTrue(schema.contains("\"columnName\": \"exact\""))
        assertTrue(schema.contains("\"tableName\": \"persistent_reminder_escalations\""))
        assertTrue(schema.contains("\"columnName\": \"source_occurrence_id\""))
        assertTrue(schema.contains("\"columnName\": \"next_realert_at_epoch_millis\""))
        assertTrue(schema.contains("\"columnName\": \"realerts_delivered\""))
    }

    private fun baseIntervalEntity(
        intervalEveryMillis: Long? = 30 * 60 * 1000L,
        intervalEligibleDays: String? = "MONDAY,WEDNESDAY",
    ): TaskEntity = baseEntity(
        scheduleKind = "INTERVAL",
        intervalEveryMillis = intervalEveryMillis,
        intervalWindowStartsAt = "09:00",
        intervalWindowEndsAt = "17:00",
        intervalEligibleDays = intervalEligibleDays,
        intervalStartsOn = "2026-08-31",
        intervalAnchor = "SCHEDULED_START",
    )

    private fun baseEntity(
        scheduleKind: String? = null,
        allDayDate: String? = null,
        timeBlockStartsAt: String? = null,
        timeBlockEndsAt: String? = null,
        intervalEveryMillis: Long? = null,
        intervalWindowStartsAt: String? = null,
        intervalWindowEndsAt: String? = null,
        intervalEligibleDays: String? = null,
        intervalStartsOn: String? = null,
        intervalAnchor: String? = null,
        recurrenceFrequency: String? = null,
        recurrenceEvery: Int? = null,
        recurrenceStartsOn: String? = null,
        recurrenceDaysOfWeek: String? = null,
        reminderMode: String = ReminderMode.NORMAL.name,
        realertIntervalMillis: Long? = null,
        maximumRealerts: Int = 0,
    ): TaskEntity = TaskEntity(
        id = "task-1",
        title = "Persisted task",
        status = "ACTIVE",
        priority = "NORMAL",
        scheduleKind = scheduleKind,
        allDayDate = allDayDate,
        timeBlockStartsAt = timeBlockStartsAt,
        timeBlockEndsAt = timeBlockEndsAt,
        intervalEveryMillis = intervalEveryMillis,
        intervalWindowStartsAt = intervalWindowStartsAt,
        intervalWindowEndsAt = intervalWindowEndsAt,
        intervalEligibleDays = intervalEligibleDays,
        intervalStartsOn = intervalStartsOn,
        intervalAnchor = intervalAnchor,
        recurrenceFrequency = recurrenceFrequency,
        recurrenceEvery = recurrenceEvery,
        recurrenceStartsOn = recurrenceStartsOn,
        recurrenceDaysOfWeek = recurrenceDaysOfWeek,
        reminderMode = reminderMode,
        realertIntervalMillis = realertIntervalMillis,
        maximumRealerts = maximumRealerts,
    )

    private fun readExportedSchema(): String {
        val relativePath = "schemas/com.dayloop.app.core.database.DayLoopDatabase/4.json"
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
        )
        return requireNotNull(candidates.firstOrNull { file -> file.isFile }) {
            "Expected exported Room schema at $relativePath."
        }.readText()
    }
}




