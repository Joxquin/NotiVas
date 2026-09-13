package com.notivas.data.model

import com.google.gson.annotations.SerializedName

data class CanvasAssignmentDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("due_at") val dueAt: String? = null,
    @SerializedName("points_possible") val pointsPossible: Double? = null,
    @SerializedName("rubric") val rubric: List<CanvasRubricCriterion>? = null,
    @SerializedName("submission") val submission: CanvasSubmissionDetail? = null
)

data class CanvasSubmissionDetail(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("workflow_state") val workflowState: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("graded_at") val gradedAt: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("late") val late: Boolean? = null,
    @SerializedName("missing") val missing: Boolean? = null,
    @SerializedName("submission_comments") val submissionComments: List<CanvasSubmissionComment>? = null,
    @SerializedName("rubric_assessment") val rubricAssessment: Map<String, CanvasRubricAssessmentItem>? = null
)

data class CanvasSubmissionComment(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("author_id") val authorId: Long? = null,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CanvasRubricAssessmentItem(
    @SerializedName("points") val points: Double? = null,
    @SerializedName("rating_id") val ratingId: String? = null,
    @SerializedName("comments") val comments: String? = null
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

data class CanvasModule(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("position") val position: Int? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    @SerializedName("items") val items: List<CanvasModuleItem>? = null
)

data class CanvasModuleItem(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // "File", "Page", "Discussion", "Assignment", "ExternalUrl", "Quiz"
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("page_url") val pageUrl: String? = null,
    @SerializedName("content_id") val contentId: Long? = null
)

data class CanvasPageDetail(
    @SerializedName("url") val url: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CanvasFileDetail(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("content-type") val contentType: String? = null,
    @SerializedName("size") val size: Long? = null
)


