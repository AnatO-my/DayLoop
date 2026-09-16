package com.dayloop.app.core.projects

import com.dayloop.app.core.model.*
import com.dayloop.app.today.TodayMoment
import java.time.LocalDate

class ProjectOccurrenceGenerator {
    fun generateFor(projects: List<Project>, moment: TodayMoment): List<Task> {
        return projects
            .filter { it.status == ProjectStatus.ACTIVE }
            .mapNotNull { project ->
                val protocol = project.protocol as? SimplePhaseCycle ?: return@mapNotNull null
                val status = protocol.getStatusAt(moment.date)
                
                if (status is ProtocolDayStatus.ACTIVE) {
                    val template = protocol.taskTemplate
                    Task(
                        id = generateTaskId(project.id, moment.date),
                        title = template.title,
                        priority = template.priority,
                        status = TaskStatus.ACTIVE,
                        schedule = TaskSchedule.AllDay(moment.date),
                        reminderPolicy = template.reminderPolicy,
                        projectId = project.id,
                        provenance = TaskProvenance(
                            originType = "PROJECT_RULE",
                            ruleId = "SIMPLE_PHASE_CYCLE",
                            version = project.version
                        )
                    )
                } else {
                    null
                }
            }
    }

    private fun generateTaskId(projectId: ProjectId, date: LocalDate): TaskId {
        return TaskId("generated:${projectId.value}:${date}")
    }
}
