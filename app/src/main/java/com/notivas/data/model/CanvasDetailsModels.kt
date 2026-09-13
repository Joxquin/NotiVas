package com.notivas.data.model

import com.google.gson.annotations.SerializedName

data class CanvasAssignmentDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("due_at") val dueAt: String? = null,
    @SerializedName("points_possible") val pointsPossible: Double? = null,
    @SerializedName("rubric") val rubric: List<CanvasRubricCriterion>? = null,
    @SerializedName("submission") val submission: SubmissionDetails? = null
)

data class CanvasRubricCriterion(
    @SerializedName("id") val id: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("long_description") val longDescription: String? = null,
    @SerializedName("points") val points: Double? = null,
    @SerializedName("ratings") val ratings: List<CanvasRubricRating>? = null
)

data class CanvasRubricRating(
    @SerializedName("id") val id: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("points") val points: Double? = null
)
