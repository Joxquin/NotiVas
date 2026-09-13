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
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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
            // Sincronizar datos con Canvas si el usuario tiene sesión activa
            val token = preferencesManager.accessToken.first()
            if (!token.isNullOrBlank()) {
                try {
                    repository.fetchAndSaveData()
                } catch (_: Exception) {
                    // Si falla la red, continuamos con los datos en caché local
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

            // Read user settings for granular notifications
            val notif24hEnabled = preferencesManager.notif24h.first()
            val notif3hEnabled = preferencesManager.notif3h.first()
            val notif30mEnabled = preferencesManager.notif30m.first()

            assignments.forEach { assignment ->
                if (assignment.isCompleted) return@forEach

                val dueStr = assignment.dueAt ?: assignment.lockAt ?: return@forEach
                try {
                    val dueDate = ZonedDateTime.parse(dueStr)
                    val hoursRemaining = ChronoUnit.HOURS.between(now, dueDate)
                    val minutesRemaining = ChronoUnit.MINUTES.between(now, dueDate)
                    val courseName = courseMap[assignment.courseId]?.name ?: "Curso"

                    // Formatear nombre para notificaciones compactas
                    val shortName = if (assignment.name.length > 38) {
                        assignment.name.take(35) + "..."
                    } else {
                        assignment.name
                    }

                    // 1. Alerta Crítica (30 minutos o menos)
                    if (notif30mEnabled && !assignment.notified30m && minutesRemaining in 1..30) {
                        notificationHelper.showNotification(
                            title = "⚠️ Alerta Crítica · $courseName",
                            message = "$shortName: ¡Últimos $minutesRemaining minutos para la entrega!"
                        )
                        repository.markNotified30m(assignment.id)
                    }
                    // 2. Alerta de Urgencia (3 horas o menos)
                    else if (notif3hEnabled && !assignment.notified3h && hoursRemaining in 1..3) {
                        notificationHelper.showNotification(
                            title = "⏰ Alerta de Urgencia · $courseName",
                            message = "$shortName: Quedan ~$hoursRemaining hora(s) para la entrega"
                        )
                        repository.markNotified3h(assignment.id)
                    }
                    // 3. Recordatorio Preventivo (24 horas o menos)
                    else if (notif24hEnabled && !assignment.notified24h && hoursRemaining in 4..24) {
                        notificationHelper.showNotification(
                            title = "📅 Recordatorio Preventivo · $courseName",
                            message = "$shortName: Entrega programada para las próximas 24h"
                        )
                        repository.markNotified24h(assignment.id)
                    }
                } catch (_: Exception) {
                    // Ignorar errores de parsing de fechas específicas
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
