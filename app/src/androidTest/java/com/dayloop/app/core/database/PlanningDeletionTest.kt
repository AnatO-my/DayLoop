package com.dayloop.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanningDeletionTest {
    private lateinit var db: DayLoopDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            DayLoopDatabase::class.java).build()
    }

    @After fun close() { db.close() }

    @Test fun deletingProjectPreservesTasksAndUnlinksOnlyItsChildren() = runBlocking {
        db.projectDao().upsert(ProjectEntity(id = "p", title = "Project", status = "ACTIVE"))
        db.projectDao().upsert(ProjectEntity(id = "other", title = "Other", status = "ACTIVE"))
        val task = TaskEntity(id = "t", title = "Saved task", status = "ACTIVE", priority = "NORMAL", projectId = "p")
        db.taskDao().upsert(task)
        db.taskDao().upsert(task.copy(id = "other-task", projectId = "other"))
        assertEquals(1, db.projectDao().deleteAndUnlink("p"))
        assertNull(db.projectDao().getById("p"))
        assertEquals(task.copy(projectId = null), db.taskDao().getById("t"))
        assertEquals("other", db.taskDao().getById("other-task")?.projectId)
        assertEquals(0, db.projectDao().deleteAndUnlink("p"))
    }

    @Test fun deletingGoalPreservesProjectsAndTheirTaskLinks() = runBlocking {
        db.goalDao().upsert(GoalEntity(id = "g", title = "Goal", progressPolicy = "COUNT", status = "ACTIVE"))
        val project = ProjectEntity(id = "p", goalId = "g", title = "Project", status = "ACTIVE")
        db.projectDao().upsert(project)
        val task = TaskEntity(id = "t", title = "Task", status = "ACTIVE", priority = "NORMAL", projectId = "p")
        db.taskDao().upsert(task)
        assertEquals(1, db.goalDao().deleteAndUnlink("g"))
        assertNull(db.goalDao().getById("g"))
        assertEquals(project.copy(goalId = null), db.projectDao().getById("p"))
        assertEquals(task, db.taskDao().getById("t"))
        assertEquals(0, db.goalDao().deleteAndUnlink("g"))
    }
}
