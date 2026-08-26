package com.notivas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notivas.data.model.Assignment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments ORDER BY dueAt ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE courseId = :courseId ORDER BY dueAt ASC")
    fun getAssignmentsByCourse(courseId: Long): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<Assignment>)

    @Query("UPDATE assignments SET notificationSent = :sent WHERE id = :id")
    suspend fun updateNotificationSent(id: Long, sent: Boolean)

    @Query("DELETE FROM assignments")
    suspend fun deleteAll()
}
