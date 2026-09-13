package com.notivas.data.remote

import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.UserProfile
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface CanvasApiService {

    @GET("api/v1/courses")
    suspend fun getCourses(
        @Header("Authorization") token: String,
        @Query("enrollment_state") state: String = "active",
        @Query("per_page") perPage: Int = 100
    ): List<Course>

    @GET("api/v1/users/self/upcoming_assignments")
    suspend fun getUpcomingAssignments(
        @Header("Authorization") token: String
    ): List<Assignment>

    @GET("api/v1/courses/{courseId}/assignments")
    suspend fun getAssignmentsForCourse(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Query("include[]") include: String? = "submission",
        @Query("order_by") orderBy: String? = "due_at",
        @Query("per_page") perPage: Int = 100
    ): List<Assignment>

    @GET("api/v1/courses/{courseId}/assignments/{assignmentId}/submissions/self")
    suspend fun getSubmissionForAssignment(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Path("assignmentId") assignmentId: Long
    ): com.notivas.data.model.SubmissionDetails
    
    @GET("api/v1/users/self/missing_submissions")
    suspend fun getMissingSubmissions(
        @Header("Authorization") token: String,
        @Query("per_page") perPage: Int = 100
    ): List<Assignment>
    
    @GET("api/v1/users/self/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfile
    
    @GET("api/v1/users/self/profile")
    suspend fun verifyToken(
        @Header("Authorization") token: String
    ): UserProfile

    @GET("api/v1/planner/items")
    suspend fun getPlannerItems(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String,
        @Query("per_page") perPage: Int = 100
    ): List<com.notivas.data.model.PlannerItem>

    @GET("api/v1/courses/{courseId}/assignments/{assignmentId}")
    suspend fun getAssignmentDetails(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Path("assignmentId") assignmentId: Long,
        @Query("include[]") include: List<String> = listOf("rubric", "submission", "submission_comments", "rubric_assessment")
    ): com.notivas.data.model.CanvasAssignmentDetailResponse

    @GET("api/v1/courses/{courseId}/modules")
    suspend fun getModulesWithItems(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Query("include[]") include: List<String> = listOf("items"),
        @Query("per_page") perPage: Int = 50
    ): List<com.notivas.data.model.CanvasModule>

    @GET("api/v1/courses/{courseId}/pages/{pageUrl}")
    suspend fun getPageDetails(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Path("pageUrl") pageUrl: String
    ): com.notivas.data.model.CanvasPageDetail

    @GET("api/v1/courses/{courseId}/files/{fileId}")
    suspend fun getFileDetails(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Path("fileId") fileId: Long
    ): com.notivas.data.model.CanvasFileDetail

    @GET("api/v1/courses/{courseId}/discussion_topics")
    suspend fun getDiscussionTopics(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Query("per_page") perPage: Int = 50
    ): List<com.notivas.data.model.CanvasDiscussionTopic>

    @GET("api/v1/courses/{courseId}/discussion_topics/{topicId}")
    suspend fun getDiscussionTopic(
        @Header("Authorization") token: String,
        @Path("courseId") courseId: Long,
        @Path("topicId") topicId: Long
    ): com.notivas.data.model.CanvasDiscussionTopic
}



