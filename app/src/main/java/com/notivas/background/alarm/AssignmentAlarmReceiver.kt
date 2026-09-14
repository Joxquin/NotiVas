package com.notivas.background.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notivas.background.notification.NotificationHelper
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class AssignmentAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var repository: CanvasRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var preferencesManager: com.notivas.data.local.prefs.PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reagendar todas las alarmas tras el reinicio del dispositivo
            CoroutineScope(Dispatchers.IO).launch {
                alarmScheduler.rescheduleAllAlarms()
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

        CoroutineScope(Dispatchers.IO).launch {
            val shouldNotify = when (alertType) {
                ALERT_TYPE_24H -> preferencesManager.notif24h.first()
                ALERT_TYPE_3H -> preferencesManager.notif3h.first()
                ALERT_TYPE_30M -> preferencesManager.notif30m.first()
                else -> false
            }

            if (shouldNotify) {
                notificationHelper.showNotification(title, message)
                when (alertType) {
                    ALERT_TYPE_24H -> repository.markNotified24h(assignmentId)
                    ALERT_TYPE_3H -> repository.markNotified3h(assignmentId)
                    ALERT_TYPE_30M -> repository.markNotified30m(assignmentId)
                }
            }
        }
    }

    companion object {
        const val EXTRA_ASSIGNMENT_ID = "extra_assignment_id"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_ASSIGNMENT_NAME = "extra_assignment_name"

        const val ALERT_TYPE_24H = "24h"
        const val ALERT_TYPE_3H = "3h"
        const val ALERT_TYPE_30M = "30m"
    }
}
