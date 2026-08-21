package com.notivas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("course_code") val courseCode: String? = null
)
