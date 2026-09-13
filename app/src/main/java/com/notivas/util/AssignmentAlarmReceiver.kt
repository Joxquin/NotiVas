package com.notivas.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AssignmentAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var repository: CanvasRepository

    @Inject
    lateinit var alarmSchedulerHelper: AlarmSchedulerHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reagendar todas las alarmas tras el reinicio del dispositivo
            CoroutineScope(Dispatchers.IO).launch {
                alarmSchedulerHelper.rescheduleAllAlarms()
            }
            return
        }

        val assignmentId = intent.getLongExtra(EXTRA_ASSIGNMENT_ID, -1L)
        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: return
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Curso"
        val assignmentName = intent.getStringExtra(EXTRA_ASSIGNMENT_NAME) ?: "Tarea"

        if (assignmentId == -1L) return

        val (title, message) = when (alertType) {
            ALERT_TYPE_30M -> {
                "⚠️ Alerta Crítica · $courseName" to "$assignmentName: ¡Últimos 30 minutos para la entrega!"
            }
            ALERT_TYPE_3H -> {
                "⏰ Alerta de Urgencia · $courseName" to "$assignmentName: Quedan 3 horas para la entrega"
            }
            ALERT_TYPE_24H -> {
                "📅 Recordatorio Preventivo · $courseName" to "$assignmentName: Entrega programada para mañana"
            }
            else -> return
        }

        notificationHelper.showNotification(title, message)

        CoroutineScope(Dispatchers.IO).launch {
            when (alertType) {
                ALERT_TYPE_30M -> {
                    repository.markNotified30m(assignmentId)
                    repository.markNotified3h(assignmentId)
                    repository.markNotified24h(assignmentId)
                    repository.markNotificationSent(assignmentId)
                }
                ALERT_TYPE_3H -> {
                    repository.markNotified3h(assignmentId)
                    repository.markNotified24h(assignmentId)
                    repository.markNotificationSent(assignmentId)
                }
                ALERT_TYPE_24H -> {
                    repository.markNotified24h(assignmentId)
                    repository.markNotificationSent(assignmentId)
                }
            }
        }
    }

    companion object {
        const val EXTRA_ASSIGNMENT_ID = "extra_assignment_id"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_ASSIGNMENT_NAME = "extra_assignment_name"

        const val ALERT_TYPE_24H = "ALERT_24H"
        const val ALERT_TYPE_3H = "ALERT_3H"
        const val ALERT_TYPE_30M = "ALERT_30M"
    }
}
