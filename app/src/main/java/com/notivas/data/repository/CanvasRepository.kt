package com.notivas.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.dao.PlannerItemDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.model.UserProfile
import com.notivas.data.remote.CanvasApiService
import com.notivas.worker.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class CanvasRepository
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val apiService: CanvasApiService,
        private val courseDao: CourseDao,
        private val assignmentDao: AssignmentDao,
        private val plannerItemDao: PlannerItemDao,
        private val simulationDao: com.notivas.data.local.dao.SimulationDao,
        private val preferencesManager: PreferencesManager
) {
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()
    val allPlannerItems: Flow<List<PlannerItem>> = plannerItemDao.getAllPlannerItems()
    val universityUrl: Flow<String?> = preferencesManager.universityUrl

    fun getSimulationGroupsWithItems(courseId: Long): Flow<List<com.notivas.data.model.SimulationGroupWithItems>> =
        simulationDao.getGroupsWithItemsByCourse(courseId)

    suspend fun createSimulationGroup(group: com.notivas.data.model.SimulationGroup): Long =
        simulationDao.insertGroup(group)

    suspend fun updateSimulationGroup(group: com.notivas.data.model.SimulationGroup) =
        simulationDao.updateGroup(group)

    suspend fun deleteSimulationGroup(group: com.notivas.data.model.SimulationGroup) =
        simulationDao.deleteGroup(group)

    suspend fun addSimulationItem(item: com.notivas.data.model.SimulationItem): Long =
        simulationDao.insertItem(item)

    suspend fun deleteSimulationItem(item: com.notivas.data.model.SimulationItem) =
        simulationDao.deleteItem(item)

    suspend fun updateSimulationItemScore(itemId: Long, score: Float) =
        simulationDao.updateItemScore(itemId, score)

    suspend fun linkSimulationItemWithCanvas(itemId: Long, assignmentId: Long, name: String) =
        simulationDao.linkItemWithCanvasAssignment(itemId, assignmentId, name)

    suspend fun fetchAndSaveData() {
        val rawToken = preferencesManager.accessToken.first() ?: return
        val token = "Bearer $rawToken"

        try {
            // 1. Fetch Planner Items (for Foros and Planner)
            val now = java.time.ZonedDateTime.now()
            val startDate =
                    now.minusDays(30)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            try {
                val plannerItems = apiService.getPlannerItems(token, startDate)
                plannerItemDao.insertPlannerItems(plannerItems)
            } catch (e: Exception) {
                Log.e("CanvasRepository", "Error fetching planner items", e)
            }

            // 2. Fetch Active Courses
            val allCoursesList = apiService.getCourses(token)
            Log.d("CanvasRepository", "Fetched ${allCoursesList.size} active courses")
            courseDao.upsertCourses(allCoursesList)

            // 3. Fetch assignments for ALL active courses in parallel using coroutines
            val currentAssignmentsList = coroutineScope {
                allCoursesList
                        .map { course ->
                            async(Dispatchers.IO) {
                                try {
                                    val rawAssignments =
                                            apiService.getAssignmentsForCourse(
                                                    token = token,
                                                    courseId = course.id,
                                                    include = "submission",
                                                    orderBy = "due_at"
                                            )
                                    rawAssignments.mapNotNull { a ->
                                        // Standard Canvas LMS discussion topic check
                                        val isForum =
                                                a.submissionTypes?.contains("discussion_topic") ==
                                                        true ||
                                                        a.name.contains(
                                                                "FORO",
                                                                ignoreCase = true
                                                        ) ||
                                                        a.name.contains("FORUM", ignoreCase = true)
                                        if (isForum) return@mapNotNull null

                                        val sub = a.submission
                                        val isSubmitted =
                                                sub != null &&
                                                        (!sub.submittedAt.isNullOrBlank() ||
                                                                sub.workflowState in
                                                                        listOf(
                                                                                "submitted",
                                                                                "graded"
                                                                        ))

                                        val status = if (isSubmitted) "completed" else "upcoming"

                                        val effectiveDueAt = a.dueAt ?: a.lockAt
                                        a.copy(
                                                dueAt = effectiveDueAt,
                                                status = status,
                                                submittedAt = sub?.submittedAt,
                                                gradedAt = sub?.gradedAt,
                                                score = sub?.score,
                                                grade = sub?.grade
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                            "CanvasRepository",
                                            "Error fetching assignments for course ${course.id}",
                                            e
                                    )
                                    emptyList()
                                }
                            }
                        }
                        .awaitAll()
                        .flatten()
            }

            // 4. Fetch Missing Submissions from Canvas API (exact endpoint used in Python
            // show_missing_tasks)
            val missingMap =
                    try {
                        val rawMissing = apiService.getMissingSubmissions(token)
                        rawMissing
                                .mapNotNull { a ->
                                    val isForum =
                                            a.submissionTypes?.contains("discussion_topic") ==
                                                    true ||
                                                    a.name.contains("FORO", ignoreCase = true) ||
                                                    a.name.contains("FORUM", ignoreCase = true)
                                    if (isForum) return@mapNotNull null
                                    a.id to a.copy(status = "missing")
                                }
                                .toMap()
                    } catch (e: Exception) {
                        Log.e("CanvasRepository", "Error fetching missing submissions", e)
                        emptyMap()
                    }

            // Combine both: apply missing status if assignment is in missingMap
            val currentWithMissing =
                    currentAssignmentsList.map { a ->
                        if (missingMap.containsKey(a.id) && a.status != "completed") {
                            a.copy(status = "missing")
                        } else {
                            a
                        }
                    }

            val remainingMissing =
                    missingMap.values.filter { m -> currentAssignmentsList.none { it.id == m.id } }
            val combinedAssignments = currentWithMissing + remainingMissing

            assignmentDao.insertAssignments(combinedAssignments)
        } catch (e: Exception) {
            Log.e("CanvasRepository", "Error syncing Canvas data", e)
            throw e
        }

        scheduleReminders()
    }

    suspend fun scheduleReminders(customIntervalMinutes: Long? = null) {
        val interval = customIntervalMinutes ?: preferencesManager.syncIntervalMinutes.first()
        val effectiveMinutes = maxOf(15L, interval)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(effectiveMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "assignment_reminder",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    suspend fun updateSyncInterval(minutes: Long) {
        scheduleReminders(customIntervalMinutes = minutes)
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

    suspend fun markNotificationSent(assignmentId: Long) {
        assignmentDao.updateNotificationSent(assignmentId, true)
    }

    suspend fun markNotified24h(assignmentId: Long) {
        assignmentDao.markNotified24h(assignmentId)
    }

    suspend fun markNotified3h(assignmentId: Long) {
        assignmentDao.markNotified3h(assignmentId)
    }

    suspend fun markNotified30m(assignmentId: Long) {
        assignmentDao.markNotified30m(assignmentId)
    }

    suspend fun getProfile(): UserProfile {
        val token = "Bearer ${preferencesManager.accessToken.first()}"
        return apiService.getProfile(token)
    }

    suspend fun logout() {
        preferencesManager.clear()
        courseDao.deleteAll()
        assignmentDao.deleteAll()
        plannerItemDao.deleteAll()
        WorkManager.getInstance(context).cancelAllWorkByTag("assignment_reminder")
    }
}
