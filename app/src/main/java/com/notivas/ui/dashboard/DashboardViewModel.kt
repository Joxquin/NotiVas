package com.notivas.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentUiModel(
    val assignment: Assignment,
    val courseName: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CanvasRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    val selectedCourseId: StateFlow<Long?> = _selectedCourseId.asStateFlow()

    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentUiModel>> = combine(
        repository.allAssignments,
        courses,
        _selectedCourseId
    ) { allAssignments, allCourses, courseId ->
        allAssignments
            .filter { courseId == null || it.courseId == courseId }
            .map { assignment ->
                val course = allCourses.find { it.id == assignment.courseId }
                AssignmentUiModel(assignment, course?.name ?: "Curso desconocido")
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plannerTasks: StateFlow<List<PlannerItem>> = combine(
        repository.allPlannerItems,
        _selectedCourseId
    ) { items, courseId ->
        items.filter { 
            (courseId == null || it.courseId == courseId) &&
            it.plannableType != "discussion_topic" && 
            !it.plannable.title.startsWith("_MTEO") 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCourse(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchAndSaveData()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
