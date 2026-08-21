package com.notivas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("due_at") val dueAt: String? = null,
    @SerializedName("course_id") val courseId: Long,
    @SerializedName("has_submitted_submissions") val isCompleted: Boolean = false,
    @SerializedName("locked_for_user") val isLocked: Boolean = false,
    val status: String = "missing" // Manual status: "upcoming", "completed", "missing"
)
