package com.notivas.background.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.notivas.background.notification.NotificationHelper
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val notificationHelper: NotificationHelper,
    private val repositoryProvider: Provider<CanvasRepository>
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(item: AssignmentAlarmItem) {
        if (item.isCompleted) {
            cancel(item)
            return
        }

        val dueStr = item.dueAt ?: item.lockAt ?: return
        val dueDate = try {
            ZonedDateTime.parse(dueStr)
        } catch (_: Exception) {
            return
        }

        val now = ZonedDateTime.now()
        // Si ya venció completamente, no hacemos nada
        if (now.isAfter(dueDate)) {
            return
        }

        val notif24hEnabled = preferencesManager.notif24h.first()
        val notif3hEnabled = preferencesManager.notif3h.first()
        val notif30mEnabled = preferencesManager.notif30m.first()

        val shortName = if (item.name.length > 38) {
            item.name.take(35) + "..."
        } else {
            item.name
        }

        val repository = repositoryProvider.get()
        val durationUntilDue = Duration.between(now, dueDate)
        val minutesUntilDue = durationUntilDue.toMinutes()

        // 1. Alerta Crítica (30 minutos o menos) - Mayor prioridad de entrega inminente
        if (notif30mEnabled && !item.notified30m) {
            val triggerTime30m = dueDate.minusMinutes(30)
            if (triggerTime30m.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = item.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_30M,
                    triggerEpochMillis = triggerTime30m.toInstant().toEpochMilli(),
                    courseName = item.courseName,
                    assignmentName = shortName
                )
            } else if (minutesUntilDue in 1..30) {
                // Estamos activamente dentro de los últimos 30 minutos
                notificationHelper.showNotification(
                    title = "⚠️ Alerta Crítica · ${item.courseName}",
                    message = "$shortName: ¡Últimos $minutesUntilDue minutos para la entrega!",
                    notificationId = generateRequestCode(item.id, AssignmentAlarmReceiver.ALERT_TYPE_30M)
                )
                repository.markNotified30m(item.id)
            }
        }

        // 2. Alerta de Urgencia (3 horas antes)
        if (notif3hEnabled && !item.notified3h) {
            val triggerTime3h = dueDate.minusHours(3)
            if (triggerTime3h.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = item.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_3H,
                    triggerEpochMillis = triggerTime3h.toInstant().toEpochMilli(),
                    courseName = item.courseName,
                    assignmentName = shortName
                )
            } else if (minutesUntilDue in 31..180) {
                // Estamos dentro de la ventana de urgencia (entre 3h y 30m restantes)
                val hours = minutesUntilDue / 60
                notificationHelper.showNotification(
                    title = "⏰ Alerta de Urgencia · ${item.courseName}",
                    message = "$shortName: Quedan ~$hours hora(s) para la entrega",
                    notificationId = generateRequestCode(item.id, AssignmentAlarmReceiver.ALERT_TYPE_3H)
                )
                repository.markNotified3h(item.id)
            }
        }

        // 3. Alerta 24 horas antes
        if (notif24hEnabled && !item.notified24h) {
            val triggerTime24h = dueDate.minusHours(24)
            if (triggerTime24h.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = item.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_24H,
                    triggerEpochMillis = triggerTime24h.toInstant().toEpochMilli(),
                    courseName = item.courseName,
                    assignmentName = shortName
                )
            } else if (minutesUntilDue in 181..1440) {
                // Entre 24 horas y 3 horas restantes
                notificationHelper.showNotification(
                    title = "📅 Recordatorio Preventivo · ${item.courseName}",
                    message = "$shortName: Entrega programada para las próximas 24h",
                    notificationId = generateRequestCode(item.id, AssignmentAlarmReceiver.ALERT_TYPE_24H)
                )
                repository.markNotified24h(item.id)
            }
        }
    }

    override suspend fun scheduleAlarmsForAssignment(assignment: Assignment, courseName: String) {
        schedule(AssignmentAlarmItem.fromAssignment(assignment, courseName))
    }

    override fun cancel(item: AssignmentAlarmItem) {
        cancelAlarmsForAssignment(item.id)
    }

    override fun cancelAlarmsForAssignment(assignmentId: Long) {
        listOf(
            AssignmentAlarmReceiver.ALERT_TYPE_24H,
            AssignmentAlarmReceiver.ALERT_TYPE_3H,
            AssignmentAlarmReceiver.ALERT_TYPE_30M
        ).forEach { alertType ->
            val intent = Intent(context, AssignmentAlarmReceiver::class.java)
            val requestCode = generateRequestCode(assignmentId, alertType)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override suspend fun rescheduleAllAlarms() {
        try {
            val repository = repositoryProvider.get()
            val assignments = repository.allAssignments.first()
            val courses = repository.allCourses.first()
            val courseMap = courses.associateBy { it.id }

            assignments.forEach { assignment ->
                val courseName = courseMap[assignment.courseId]?.name ?: "Curso"
                scheduleAlarmsForAssignment(assignment, courseName)
            }
        } catch (e: Exception) {
            Log.e("AndroidAlarmScheduler", "Error rescheduling all alarms", e)
        }
    }

    private fun scheduleExactAlarm(
        assignmentId: Long,
        alertType: String,
        triggerEpochMillis: Long,
        courseName: String,
        assignmentName: String
    ) {
        val intent = Intent(context, AssignmentAlarmReceiver::class.java).apply {
            putExtra(AssignmentAlarmReceiver.EXTRA_ASSIGNMENT_ID, assignmentId)
            putExtra(AssignmentAlarmReceiver.EXTRA_ALERT_TYPE, alertType)
            putExtra(AssignmentAlarmReceiver.EXTRA_COURSE_NAME, courseName)
            putExtra(AssignmentAlarmReceiver.EXTRA_ASSIGNMENT_NAME, assignmentName)
        }

        val requestCode = generateRequestCode(assignmentId, alertType)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("AndroidAlarmScheduler", "Error scheduling alarm for assignment $assignmentId", e)
        }
    }

    private fun generateRequestCode(assignmentId: Long, alertType: String): Int {
        val typeOffset = when (alertType) {
            AssignmentAlarmReceiver.ALERT_TYPE_24H -> 1
            AssignmentAlarmReceiver.ALERT_TYPE_3H -> 2
            AssignmentAlarmReceiver.ALERT_TYPE_30M -> 3
            else -> 0
        }
        return ((assignmentId % 1000000) * 10 + typeOffset).toInt()
    }
}
