package com.notivas.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AlarmSchedulerHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val repositoryProvider: Provider<CanvasRepository>
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleAlarmsForAssignment(assignment: Assignment, courseName: String) {
        if (assignment.isCompleted) {
            cancelAlarmsForAssignment(assignment.id)
            return
        }

        val dueStr = assignment.dueAt ?: assignment.lockAt ?: return
        val dueDate = try {
            ZonedDateTime.parse(dueStr)
        } catch (_: Exception) {
            return
        }

        val now = ZonedDateTime.now()
        val notif24hEnabled = preferencesManager.notif24h.first()
        val notif3hEnabled = preferencesManager.notif3h.first()
        val notif30mEnabled = preferencesManager.notif30m.first()

        val shortName = if (assignment.name.length > 38) {
            assignment.name.take(35) + "..."
        } else {
            assignment.name
        }

        // 1. Alerta 24 horas antes
        if (notif24hEnabled && !assignment.notified24h) {
            val triggerTime = dueDate.minusHours(24)
            if (triggerTime.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = assignment.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_24H,
                    triggerEpochMillis = triggerTime.toInstant().toEpochMilli(),
                    courseName = courseName,
                    assignmentName = shortName
                )
            }
        }

        // 2. Alerta 3 horas antes
        if (notif3hEnabled && !assignment.notified3h) {
            val triggerTime = dueDate.minusHours(3)
            if (triggerTime.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = assignment.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_3H,
                    triggerEpochMillis = triggerTime.toInstant().toEpochMilli(),
                    courseName = courseName,
                    assignmentName = shortName
                )
            }
        }

        // 3. Alerta 30 minutos antes
        if (notif30mEnabled && !assignment.notified30m) {
            val triggerTime = dueDate.minusMinutes(30)
            if (triggerTime.isAfter(now)) {
                scheduleExactAlarm(
                    assignmentId = assignment.id,
                    alertType = AssignmentAlarmReceiver.ALERT_TYPE_30M,
                    triggerEpochMillis = triggerTime.toInstant().toEpochMilli(),
                    courseName = courseName,
                    assignmentName = shortName
                )
            }
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
            Log.e("AlarmSchedulerHelper", "Error scheduling alarm for assignment $assignmentId", e)
        }
    }

    fun cancelAlarmsForAssignment(assignmentId: Long) {
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

    suspend fun rescheduleAllAlarms() {
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
            Log.e("AlarmSchedulerHelper", "Error rescheduling all alarms", e)
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
