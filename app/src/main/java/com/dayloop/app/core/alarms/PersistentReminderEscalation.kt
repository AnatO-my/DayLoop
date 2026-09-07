package com.dayloop.app.core.alarms

import com.dayloop.app.core.model.ReminderPolicy
import com.dayloop.app.core.model.TaskId
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class PersistentReminderEscalationId(val value: String) {
    init {
        require(value.isNotBlank()) { "PersistentReminderEscalationId must not be blank." }
    }

    companion object {
        fun generate(): PersistentReminderEscalationId =
            PersistentReminderEscalationId(UUID.randomUUID().toString())
    }
}

enum class PersistentReminderEscalationStatus {
    ACTIVE,
    RESOLVED,
    CAPPED,
    CANCELLED,
}

data class PersistentReminderEscalation(
    val id: PersistentReminderEscalationId,
    val taskId: TaskId,
    val sourceOccurrenceId: ScheduledOccurrenceId,
    val nextRealertAt: Instant?,
    val realertInterval: Duration,
    val maximumRealerts: Int,
    val realertsDelivered: Int = 0,
    val status: PersistentReminderEscalationStatus = PersistentReminderEscalationStatus.ACTIVE,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
) {
    val isActive: Boolean
        get() = status == PersistentReminderEscalationStatus.ACTIVE

    init {
        require(realertInterval.isPositive()) { "Re-alert interval must be positive." }
        require(maximumRealerts > 0) { "Maximum re-alerts must be greater than 0." }
        require(realertsDelivered >= 0) { "Delivered re-alerts must not be negative." }
        require(realertsDelivered <= maximumRealerts) {
            "Delivered re-alerts cannot exceed maximum re-alerts."
        }
        require(!updatedAt.isBefore(createdAt)) { "Escalation updatedAt cannot be before createdAt." }
        if (status == PersistentReminderEscalationStatus.ACTIVE) {
            require(nextRealertAt != null) { "Active escalation must have a next re-alert time." }
            require(realertsDelivered < maximumRealerts) {
                "Active escalation cannot have exhausted all re-alerts."
            }
        } else {
            require(nextRealertAt == null) { "Inactive escalation cannot have a next re-alert time." }
        }
    }

    fun recordRealertDelivered(at: Instant): PersistentReminderEscalation {
        if (!isActive) return this

        val delivered = realertsDelivered + 1
        return if (delivered >= maximumRealerts) {
            copy(
                nextRealertAt = null,
                realertsDelivered = delivered,
                status = PersistentReminderEscalationStatus.CAPPED,
                updatedAt = at,
            )
        } else {
            copy(
                nextRealertAt = at.plus(realertInterval),
                realertsDelivered = delivered,
                updatedAt = at,
            )
        }
    }

    fun resolve(at: Instant): PersistentReminderEscalation {
        if (status == PersistentReminderEscalationStatus.RESOLVED) return this
        return copy(
            nextRealertAt = null,
            status = PersistentReminderEscalationStatus.RESOLVED,
            updatedAt = at,
        )
    }

    fun cancel(at: Instant): PersistentReminderEscalation {
        if (status == PersistentReminderEscalationStatus.CANCELLED) return this
        return copy(
            nextRealertAt = null,
            status = PersistentReminderEscalationStatus.CANCELLED,
            updatedAt = at,
        )
    }

    companion object {
        fun start(
            policy: ReminderPolicy,
            taskId: TaskId,
            sourceOccurrenceId: ScheduledOccurrenceId,
            deliveredAt: Instant,
            id: PersistentReminderEscalationId = PersistentReminderEscalationId.generate(),
        ): PersistentReminderEscalation {
            require(policy.mode.supportsRealerts && policy.usesRealerts) {
                "Persistent escalation requires a re-alert reminder policy."
            }
            val interval = requireNotNull(policy.realertInterval) {
                "Persistent escalation requires a re-alert interval."
            }
            return PersistentReminderEscalation(
                id = id,
                taskId = taskId,
                sourceOccurrenceId = sourceOccurrenceId,
                nextRealertAt = deliveredAt.plus(interval),
                realertInterval = interval,
                maximumRealerts = policy.maximumRealerts,
                createdAt = deliveredAt,
            )
        }
    }
}

private fun Duration.isPositive(): Boolean = !isZero && !isNegative
