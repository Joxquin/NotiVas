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
    @SerializedName("points_possible") val pointsPossible: Double? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("submission_types") val submissionTypes: List<String>? = null,
    @SerializedName("has_submitted_submissions") val isCompleted: Boolean = false,
    @SerializedName("locked_for_user") val isLocked: Boolean = false,
    @SerializedName("submission") val submission: SubmissionDetails? = null,
    val status: String = "upcoming", // "upcoming", "completed", "missing"
    val submittedAt: String? = null,
    val gradedAt: String? = null,
    val score: Double? = null,
    val grade: String? = null,
    val notificationSent: Boolean = false
)

data class SubmissionDetails(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("workflow_state") val workflowState: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("graded_at") val gradedAt: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("late") val late: Boolean? = null,
    @SerializedName("missing") val missing: Boolean? = null
)
