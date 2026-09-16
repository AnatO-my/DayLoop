package com.dayloop.app.core.model

import java.time.LocalDate
import java.util.UUID

@JvmInline
value class GoalId(val value: String) {
    init {
        require(value.isNotBlank()) { "GoalId must not be blank." }
    }

    companion object {
        fun generate(): GoalId = GoalId(UUID.randomUUID().toString())
    }
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}

enum class GoalProgressPolicy {
    BINARY,
    COUNT,
    MANUAL,
}

data class Goal(
    val id: GoalId,
    val title: String,
    val targetDate: LocalDate? = null,
    val progressPolicy: GoalProgressPolicy = GoalProgressPolicy.BINARY,
    val status: GoalStatus = GoalStatus.ACTIVE,
) {
    init {
        require(title.isNotBlank()) { "Goal title must not be blank." }
    }
}
