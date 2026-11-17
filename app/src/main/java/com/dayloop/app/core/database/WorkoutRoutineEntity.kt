package com.dayloop.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Newline-delimited, escaped exercise fields. Parsed defensively by the repository. */
    @ColumnInfo(name = "exercises_payload") val exercisesPayload: String,
    /** ISO-8601 weekday numbers, comma-separated. */
    @ColumnInfo(name = "scheduled_days") val scheduledDays: String,
)

@Dao
interface WorkoutRoutineDao {
    @Query("SELECT * FROM workout_routines ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<WorkoutRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(routine: WorkoutRoutineEntity)

    @Query("DELETE FROM workout_routines WHERE id = :id")
    suspend fun delete(id: String)
}
