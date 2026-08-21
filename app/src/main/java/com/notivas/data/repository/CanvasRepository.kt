package com.notivas.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.UserProfile
import com.notivas.data.remote.CanvasApiService
import com.notivas.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val assignmentDao: AssignmentDao,
    private val preferencesManager: PreferencesManager
) {
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()

    suspend fun fetchAndSaveData() {
        val token = "Bearer ${preferencesManager.accessToken.first()}"
        
        try {
            val courses = apiService.getCourses(token)
            courseDao.insertCourses(courses)
            
            val now = java.time.ZonedDateTime.now()
            
            courses.forEach { course ->
                try {
                    val rawAssignments = apiService.getAssignmentsForCourse(token, course.id)
                    val assignments = rawAssignments.map { assignment ->
                        val status = when {
                            assignment.isCompleted -> "completed"
                            assignment.dueAt == null -> "upcoming"
                            else -> try {
                                if (java.time.ZonedDateTime.parse(assignment.dueAt).isBefore(now)) "missing"
                                else "upcoming"
                            } catch (e: Exception) { "upcoming" }
                        }
                        assignment.copy(status = status)
                    }
                    assignmentDao.insertAssignments(assignments)
                } catch (e: Exception) {
                    Log.e("CanvasRepository", "Error fetching assignments for course ${course.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("CanvasRepository", "Error fetching courses", e)
            throw e
        }
        
        scheduleReminders()
    }

    private fun scheduleReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "assignment_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    suspend fun verifyAndSave(url: String, token: String): Boolean {
        return try {
            // Temporarily save to test connection
            preferencesManager.saveUniversityUrl(url)
            preferencesManager.saveAccessToken(token)
            
            apiService.verifyToken("Bearer $token")
            true
        } catch (e: Exception) {
            preferencesManager.clear()
            false
        }
    }
    
    fun getAssignmentsByCourse(courseId: Long): Flow<List<Assignment>> =
        assignmentDao.getAssignmentsByCourse(courseId)

    suspend fun getProfile(): UserProfile {
        val token = "Bearer ${preferencesManager.accessToken.first()}"
        return apiService.getProfile(token)
    }

    suspend fun logout() {
        preferencesManager.clear()
        courseDao.deleteAll()
        assignmentDao.deleteAll()
        WorkManager.getInstance(context).cancelAllWorkByTag("assignment_reminder")
    }
}
