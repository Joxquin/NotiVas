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

    private val _selectedSemester = MutableStateFlow<String?>(null) // Default to null (Todos) so all tasks are visible
    val selectedSemester: StateFlow<String?> = _selectedSemester.asStateFlow()

    private val _showUndatedTasks = MutableStateFlow(true)
    val showUndatedTasks: StateFlow<Boolean> = _showUndatedTasks.asStateFlow()

    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper to get semester string from course name
    fun getSemesterForCourse(courseName: String): String {
        return when {
            courseName.contains("1er", ignoreCase = true) || courseName.contains("1ero", ignoreCase = true) -> "1er Semestre"
            courseName.contains("2do", ignoreCase = true) -> "2do Semestre"
            courseName.contains("3er", ignoreCase = true) || courseName.contains("3ero", ignoreCase = true) -> "3er Semestre"
            courseName.contains("4to", ignoreCase = true) -> "4to Semestre"
            courseName.contains("5to", ignoreCase = true) -> "5to Semestre"
            courseName.contains("6to", ignoreCase = true) -> "6to Semestre"
            else -> "Otros"
        }
    }

    val availableSemesters: StateFlow<List<String>> = courses.map { list ->
        list.map { getSemesterForCourse(it.name) }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("3er Semestre", "4to Semestre"))

    val assignments: StateFlow<List<AssignmentUiModel>> = combine(
        repository.allAssignments,
        courses,
        _selectedCourseId,
        _selectedSemester,
        _showUndatedTasks
    ) { allAssignments, allCourses, courseId, semester, showUndated ->
        allAssignments
            .filter { assignment ->
                val course = allCourses.find { it.id == assignment.courseId }
                val matchesCourse = (courseId == null || assignment.courseId == courseId)
                val matchesSemester = if (semester == null || semester == "Todos") true else {
                    course?.let { getSemesterForCourse(it.name) == semester } ?: true
                }
                val matchesUndated = if (!showUndated && assignment.dueAt == null && assignment.status == "upcoming") false else true
                
                matchesCourse && matchesSemester && matchesUndated
            }
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
            !it.plannable.title.startsWith("_MTEO") &&
            !it.plannable.title.contains("FORO", ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCourse(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun selectSemester(semester: String?) {
        _selectedSemester.value = semester
    }

    fun toggleShowUndatedTasks(show: Boolean) {
        _showUndatedTasks.value = show
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
