package com.notivas.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notivas.data.repository.CanvasRepository
import com.notivas.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CanvasRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val assignments = repository.allAssignments.first()
            val courses = repository.allCourses.first()
            val courseMap = courses.associateBy { it.id }
            val now = ZonedDateTime.now()
            
            assignments.forEach { assignment ->
                if (assignment.notificationSent || assignment.isCompleted) return@forEach
                
                assignment.dueAt?.let { dueStr ->
                    try {
                        val dueDate = ZonedDateTime.parse(dueStr)
                        val hoursLeft = ChronoUnit.HOURS.between(now, dueDate)
                        
                        // Notify if due within 24 hours
                        if (hoursLeft in 0..24) {
                            val courseName = courseMap[assignment.courseId]?.name ?: "Curso"
                            val shortName = if (assignment.name.length > 40) {
                                assignment.name.take(37) + "..."
                            } else {
                                assignment.name
                            }
                            
                            notificationHelper.showNotification(
                                courseName,
                                "$shortName - Entrega mañana"
                            )
                            repository.markNotificationSent(assignment.id)
                        }
                    } catch (e: Exception) {
                        // Skip invalid date formats
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
