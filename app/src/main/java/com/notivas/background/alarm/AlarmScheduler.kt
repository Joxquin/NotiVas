package com.notivas.background.alarm

import com.notivas.data.model.Assignment

data class AssignmentAlarmItem(
    val id: Long,
    val name: String,
    val courseName: String,
    val dueAt: String?,
    val lockAt: String?,
    val isCompleted: Boolean,
    val notified24h: Boolean = false,
    val notified3h: Boolean = false,
    val notified30m: Boolean = false
) {
    companion object {
        fun fromAssignment(assignment: Assignment, courseName: String): AssignmentAlarmItem {
            return AssignmentAlarmItem(
                id = assignment.id,
                name = assignment.name,
                courseName = courseName,
                dueAt = assignment.dueAt,
                lockAt = assignment.lockAt,
                isCompleted = assignment.isCompleted,
                notified24h = assignment.notified24h,
                notified3h = assignment.notified3h,
                notified30m = assignment.notified30m
            )
        }
    }
}

interface AlarmScheduler {
    suspend fun schedule(item: AssignmentAlarmItem)
    suspend fun scheduleAlarmsForAssignment(assignment: Assignment, courseName: String)
    fun cancel(item: AssignmentAlarmItem)
    fun cancelAlarmsForAssignment(assignmentId: Long)
    suspend fun rescheduleAllAlarms()
}
