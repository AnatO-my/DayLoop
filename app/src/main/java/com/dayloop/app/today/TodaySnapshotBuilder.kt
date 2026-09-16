package com.dayloop.app.today

import com.dayloop.app.core.model.Task

object TodaySnapshotBuilder {
    fun build(
        tasks: List<Task>,
        moment: TodayMoment,
        generatedTasks: List<Task> = emptyList(),
    ): TodaySnapshot {
        val generatedIds = generatedTasks.map { it.id }.toSet()
        val storedById = tasks.associateBy { it.id }
        val allTasks = tasks.filter { it.id !in generatedIds } + generatedTasks.map { generated ->
            storedById[generated.id]?.takeIf { it.provenance == generated.provenance } ?: generated
        }
        val overdue = TodayOverdueClassifier().classify(allTasks, moment.localDateTime)
        val nowNext = TodayNowNextClassifier().classify(allTasks, moment.localDateTime)
        val anytime = TodayAnytimeClassifier().classify(allTasks, moment.date)
        val completed = TodayCompletedClassifier().classify(allTasks, moment.date)

        return TodaySnapshot(
            date = moment.date,
            overdue = overdue,
            now = nowNext.now,
            next = nowNext.next,
            anytime = anytime,
            completed = completed
        )
    }
}
