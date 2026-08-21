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
            val now = ZonedDateTime.now()
            
            assignments.forEach { assignment ->
                assignment.dueAt?.let { dueStr ->
                    try {
                        val dueDate = ZonedDateTime.parse(dueStr)
                        val hoursLeft = ChronoUnit.HOURS.between(now, dueDate)
                        
                        if (hoursLeft in 0..24 && !assignment.isCompleted) {
                            notificationHelper.showNotification(
                                "Tarea Próxima: ${assignment.name}",
                                "Faltan ${hoursLeft} horas para la entrega."
                            )
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
