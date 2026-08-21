package com.notivas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course

@Database(entities = [Course::class, Assignment::class], version = 1, exportSchema = false)
abstract class CanvasDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun assignmentDao(): AssignmentDao
}
