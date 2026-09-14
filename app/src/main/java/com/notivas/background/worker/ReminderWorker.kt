package com.notivas.background.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notivas.background.alarm.AlarmScheduler
import com.notivas.background.notification.NotificationHelper
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.repository.CanvasRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CanvasRepository,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val token = preferencesManager.accessToken.first()
            if (!token.isNullOrBlank()) {
                try {
                    repository.fetchAndSaveData()
                } catch (_: Exception) {
                    // Cache fallback
                }
            }

            val assignments = repository.allAssignments.first()
            val courses = repository.allCourses.first()
            val courseMap = courses.associateBy { it.id }
            val now = ZonedDateTime.now()

            // Asegurar que las alarmas exactas estén programadas
            assignments.forEach { assignment ->
                val courseName = courseMap[assignment.courseId]?.name ?: "Curso"
                alarmScheduler.scheduleAlarmsForAssignment(assignment, courseName)
            }

            val notif24hEnabled = preferencesManager.notif24h.first()
            val notif3hEnabled = preferencesManager.notif3h.first()
            val notif30mEnabled = preferencesManager.notif30m.first()

            assignments.forEach { assignment ->
                if (assignment.isCompleted || assignment.status == "completed") return@forEach

                val dueStr = assignment.dueAt ?: assignment.lockAt ?: return@forEach
                try {
                    val dueDate = ZonedDateTime.parse(dueStr)
                    if (now.isAfter(dueDate)) return@forEach

                    val minutesRemaining = Duration.between(now, dueDate).toMinutes()
                    val courseName = courseMap[assignment.courseId]?.name ?: "Curso"

                    val shortName = if (assignment.name.length > 38) {
                        assignment.name.take(35) + "..."
                    } else {
                        assignment.name
                    }

                    // 1. Alerta Crítica (30 minutos o menos)
                    if (notif30mEnabled && !assignment.notified30m && minutesRemaining in 1..30) {
                        val notifId = ((assignment.id % 1000000) * 10 + 3).toInt()
                        notificationHelper.showNotification(
                            title = "⚠️ Alerta Crítica · $courseName",
                            message = "$shortName: ¡Últimos $minutesRemaining minutos para la entrega!",
                            notificationId = notifId
                        )
                        repository.markNotified30m(assignment.id)
                    }
                    // 2. Alerta de Urgencia (3 horas a 31 min)
                    else if (notif3hEnabled && !assignment.notified3h && minutesRemaining in 31..180) {
                        val hours = minutesRemaining / 60
                        val notifId = ((assignment.id % 1000000) * 10 + 2).toInt()
                        notificationHelper.showNotification(
                            title = "⏰ Alerta de Urgencia · $courseName",
                            message = "$shortName: Quedan ~$hours hora(s) para la entrega",
                            notificationId = notifId
                        )
                        repository.markNotified3h(assignment.id)
                    }
                    // 3. Recordatorio Preventivo (24 horas a 3h)
                    else if (notif24hEnabled && !assignment.notified24h && minutesRemaining in 181..1440) {
                        val notifId = ((assignment.id % 1000000) * 10 + 1).toInt()
                        notificationHelper.showNotification(
                            title = "📅 Recordatorio Preventivo · $courseName",
                            message = "$shortName: Entrega programada para las próximas 24h",
                            notificationId = notifId
                        )
                        repository.markNotified24h(assignment.id)
                    }
                } catch (_: Exception) {
                    // Ignorar errores de parsing
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
