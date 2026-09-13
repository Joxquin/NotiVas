package com.notivas.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.repository.CanvasRepository
import com.notivas.util.AlarmSchedulerHelper
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
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper,
    private val alarmSchedulerHelper: AlarmSchedulerHelper
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
                alarmSchedulerHelper.scheduleAlarmsForAssignment(assignment, courseName)
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
                    val minutesLeft = ChronoUnit.MINUTES.between(now, dueDate)

                    // Skip tasks already past due
                    if (minutesLeft < 0) return@forEach

                    val courseName = courseMap[assignment.courseId]?.name ?: "Curso"
                    val shortName = if (assignment.name.length > 38) {
                        assignment.name.take(35) + "..."
                    } else {
                        assignment.name
                    }

                    // 1. Alerta crítica (30 minutos o menos)
                    if (notif30mEnabled && minutesLeft in 0..30 && !assignment.notified30m) {
                        val minText = if (minutesLeft <= 1) "¡Cierra en menos de 1 minuto!" else "¡Últimos $minutesLeft minutos para la entrega!"
                        notificationHelper.showNotification(
                            "⚠️ Alerta Crítica · $courseName",
                            "$shortName: $minText"
                        )
                        repository.markNotified30m(assignment.id)
                        repository.markNotified3h(assignment.id)
                        repository.markNotified24h(assignment.id)
                        repository.markNotificationSent(assignment.id)
                        return@forEach
                    }

                    // 2. Alerta de urgencia (3 horas / 180 minutos o menos)
                    if (notif3hEnabled && minutesLeft in 31..180 && !assignment.notified3h) {
                        val hours = (minutesLeft / 60).coerceAtLeast(1)
                        notificationHelper.showNotification(
                            "⏰ Alerta de Urgencia · $courseName",
                            "$shortName: Quedan menos de $hours hora(s) para entregar"
                        )
                        repository.markNotified3h(assignment.id)
                        repository.markNotified24h(assignment.id)
                        repository.markNotificationSent(assignment.id)
                        return@forEach
                    }

                    // 3. Recordatorio preventivo (24 horas / 1440 minutos o menos)
                    if (notif24hEnabled && minutesLeft in 181..1440 && !assignment.notified24h) {
                        notificationHelper.showNotification(
                            "📅 Recordatorio Preventivo · $courseName",
                            "$shortName: Entrega programada para mañana"
                        )
                        repository.markNotified24h(assignment.id)
                        repository.markNotificationSent(assignment.id)
                    }

                } catch (_: Exception) {
                    // Skip malformed dates
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
