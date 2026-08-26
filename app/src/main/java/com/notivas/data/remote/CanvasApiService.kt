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
        @Path("courseId") courseId: Long
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
}
