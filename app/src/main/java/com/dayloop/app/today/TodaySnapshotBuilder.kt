package com.dayloop.app.today

import com.dayloop.app.core.model.Task

object TodaySnapshotBuilder {
    fun build(tasks: List<Task>, moment: TodayMoment): TodaySnapshot {
        val overdue = TodayOverdueClassifier().classify(tasks, moment)
        val nowNext = TodayNowNextClassifier().classify(tasks, moment)
        val anytime = TodayAnytimeClassifier().classify(tasks, moment)
        val completed = TodayCompletedClassifier().classify(tasks, moment)

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
