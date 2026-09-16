package com.dayloop.app.core.model

import java.util.UUID

@JvmInline
value class ProjectId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProjectId must not be blank." }
    }

    companion object {
        fun generate(): ProjectId = ProjectId(UUID.randomUUID().toString())
    }
}

enum class ProjectStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED,
}

data class Project(
    val id: ProjectId,
    val goalId: GoalId? = null,
    val title: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val protocol: ProjectProtocol? = null,
    val version: Long = 1L,
) {
    init {
        require(title.isNotBlank()) { "Project title must not be blank." }
        require(version > 0) { "Project version must be positive." }
    }

    fun updateProtocol(newProtocol: ProjectProtocol): Project = copy(
        protocol = newProtocol,
        version = version + 1
    )
}
