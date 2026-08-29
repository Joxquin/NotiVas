package com.notivas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.dao.PlannerItemDao
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.model.SubmissionDetails

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(it, type)
    }

    @TypeConverter
    fun fromSubmissionDetails(value: SubmissionDetails?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toSubmissionDetails(value: String?): SubmissionDetails? = value?.let {
        gson.fromJson(it, SubmissionDetails::class.java)
    }
}

@Database(entities = [Course::class, Assignment::class, PlannerItem::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CanvasDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun plannerItemDao(): PlannerItemDao
}
