package com.notivas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.dao.PlannerItemDao
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem

@Database(entities = [Course::class, Assignment::class, PlannerItem::class], version = 4, exportSchema = false)
abstract class CanvasDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun plannerItemDao(): PlannerItemDao
}
