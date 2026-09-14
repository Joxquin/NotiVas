package com.notivas.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.background.alarm.AlarmScheduler
import com.notivas.background.notification.NotificationHelper
import com.notivas.data.model.Assignment
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val canvasRepository: CanvasRepository,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val assignments: StateFlow<List<Assignment>> = canvasRepository.allAssignments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendTestNotification24h() {
        notificationHelper.showNotification(
            title = "📅 Recordatorio Preventivo · Prueba Debug",
            message = "Tarea de Prueba: Entrega programada para mañana (24h)"
        )
    }

    fun sendTestNotification3h() {
        notificationHelper.showNotification(
            title = "⏰ Alerta de Urgencia · Prueba Debug",
            message = "Tarea de Prueba: Quedan 3 horas para la entrega"
        )
    }

    fun sendTestNotification30m() {
        notificationHelper.showNotification(
            title = "⚠️ Alerta Crítica · Prueba Debug",
            message = "Tarea de Prueba: ¡Últimos 30 minutos para la entrega!"
        )
    }

    fun triggerRescheduleAlarms() {
        viewModelScope.launch {
            alarmScheduler.rescheduleAllAlarms()
        }
    }

    fun createMockAssignment(name: String, dueAt: String) {
        viewModelScope.launch {
            val mockId = System.currentTimeMillis()
            val mockAssignment = Assignment(
                id = mockId,
                courseId = 99999L,
                name = name,
                description = "Tarea de prueba generada desde Debug Screen",
                dueAt = dueAt,
                lockAt = null,
                pointsPossible = 100.0,
                htmlUrl = "",
                submissionTypes = emptyList(),
                status = "upcoming"
            )
            canvasRepository.insertMockAssignment(mockAssignment)
        }
    }

    fun deleteMockAssignment(id: Long) {
        viewModelScope.launch {
            canvasRepository.deleteAssignmentById(id)
        }
    }
}
