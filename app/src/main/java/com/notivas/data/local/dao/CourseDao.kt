package com.notivas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.notivas.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourses(courses: List<Course>): List<Long>

    @androidx.room.Update
    suspend fun updateCourses(courses: List<Course>)

    @Transaction
    suspend fun upsertCourses(courses: List<Course>) {
        val insertResults = insertCourses(courses)
        val updateList = mutableListOf<Course>()
        for (i in insertResults.indices) {
            if (insertResults[i] == -1L) {
                updateList.add(courses[i])
            }
        }
        if (updateList.isNotEmpty()) {
            updateCourses(updateList)
        }
    }

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
