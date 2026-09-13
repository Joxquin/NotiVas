package com.notivas.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.UserProfile
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssignmentUiModel(val assignment: Assignment, val courseName: String)

data class CourseStat(
    val course: Course,
    val pendingCount: Int,
    val completedCount: Int,
    val missingCount: Int,
    val averageScore: Double?
)

data class DaySchedule(
    val date: LocalDate,
    val dayName: String,
    val dayNumber: Int,
    val taskCount: Int,
    val isToday: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repository: CanvasRepository) :
    ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _inspectedCourse = MutableStateFlow<Course?>(null)
    val inspectedCourse: StateFlow<Course?> = _inspectedCourse.asStateFlow()

    val institutionName: StateFlow<String> = repository.universityUrl.map { url ->
        if (url.isNullOrBlank()) {
            "CANVAS"
        } else {
            val clean = url.removePrefix("https://").removePrefix("http://").trim()
            val host = clean.substringBefore('/')
            // Toma el primer subdominio (ej. tecsup de tecsup.instructure.com) y lo pasa a mayúsculas
            host.substringBefore('.').trim().uppercase()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CANVAS")

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.getProfile()
            } catch (e: Exception) {
                // Ignore fallback to defaults
            }
        }
    }

    val courses: StateFlow<List<Course>> =
        repository.allCourses.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allAssignmentsUi: StateFlow<List<AssignmentUiModel>> =
        combine(repository.allAssignments, courses) { allAssignments, allCourses ->
            allAssignments.map { assignment ->
                val course = allCourses.find { it.id == assignment.courseId }
                AssignmentUiModel(assignment, course?.name ?: "Curso")
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Urgent assignments (strictly Today and Tomorrow in local time)
    val urgentAssignments: StateFlow<List<AssignmentUiModel>> =
        allAssignmentsUi
            .map { list ->
                val now = ZonedDateTime.now(java.time.ZoneId.systemDefault())
                val today = now.toLocalDate()
                val tomorrow = today.plusDays(1)

                list
                    .filter { ui ->
                        val a = ui.assignment
                        if (a.status != "upcoming") return@filter false
                        val dueRaw = a.dueAt ?: a.lockAt ?: return@filter false
                        try {
                            val dueZoned =
                                ZonedDateTime.parse(dueRaw)
                                    .withZoneSameInstant(
                                        java.time.ZoneId.systemDefault()
                                    )
                            val dueDate = dueZoned.toLocalDate()

                            // Must be due today or tomorrow, and not already past the
                            // due time
                            (dueDate == today || dueDate == tomorrow) &&
                                    !dueZoned.isBefore(now)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    .sortedBy { it.assignment.dueAt ?: it.assignment.lockAt }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly 7-day schedule strip
    val weeklySchedule: StateFlow<List<DaySchedule>> =
        repository
            .allAssignments
            .map { allAssignments ->
                val today = LocalDate.now()
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                (0..6).map { dayOffset ->
                    val date = startOfWeek.plusDays(dayOffset.toLong())
                    val count =
                        allAssignments.count { a ->
                            if (a.dueAt == null) return@count false
                            try {
                                val localDue =
                                    ZonedDateTime.parse(a.dueAt)
                                        .withZoneSameInstant(
                                            java.time.ZoneId.systemDefault()
                                        )
                                        .toLocalDate()
                                localDue == date
                            } catch (e: Exception) {
                                false
                            }
                        }
                    val dayName =
                        when (date.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> "LUN"
                            java.time.DayOfWeek.TUESDAY -> "MAR"
                            java.time.DayOfWeek.WEDNESDAY -> "MIÉ"
                            java.time.DayOfWeek.THURSDAY -> "JUE"
                            java.time.DayOfWeek.FRIDAY -> "VIE"
                            java.time.DayOfWeek.SATURDAY -> "SÁB"
                            java.time.DayOfWeek.SUNDAY -> "DOM"
                        }
                    DaySchedule(
                        date = date,
                        dayName = dayName,
                        dayNumber = date.dayOfMonth,
                        taskCount = count,
                        isToday = date == today
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Course stats for the 2-column matrix
    val courseStats: StateFlow<List<CourseStat>> =
        combine(courses, repository.allAssignments) { allCourses, allAssignments ->
            allCourses.map { course ->
                val courseAssignments =
                    allAssignments.filter { it.courseId == course.id }
                val pending = courseAssignments.count { it.status == "upcoming" }
                val completed = courseAssignments.count { it.status == "completed" }
                val missing = courseAssignments.count { it.status == "missing" }
                val scores = courseAssignments.mapNotNull { it.score }
                val avg = if (scores.isNotEmpty()) scores.average() else null
                CourseStat(
                    course = course,
                    pendingCount = pending,
                    completedCount = completed,
                    missingCount = missing,
                    averageScore = avg
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Assignments for the selected inspected course
    val inspectedCourseAssignments: StateFlow<List<AssignmentUiModel>> =
        combine(allAssignmentsUi, _inspectedCourse) { all, inspected ->
            if (inspected == null) emptyList()
            else all.filter { it.assignment.courseId == inspected.id }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Forums / Discussion topics for the selected inspected course
    val inspectedCourseForums: StateFlow<List<com.notivas.data.model.PlannerItem>> =
        combine(repository.allPlannerItems, _inspectedCourse) { items, inspected ->
            if (inspected == null) emptyList()
            else
                items.filter { item ->
                    val matchesCourse =
                        item.courseId == inspected.id ||
                                item.contextName?.contains(
                                    inspected.name,
                                    ignoreCase = true
                                ) == true ||
                                (inspected.courseCode != null &&
                                        item.contextName?.contains(
                                            inspected.courseCode,
                                            ignoreCase = true
                                        ) == true)
                    val isForum =
                        item.plannableType == "discussion_topic" ||
                                item.plannable.title.contains(
                                    "FORO",
                                    ignoreCase = true
                                ) ||
                                item.plannable.title.contains(
                                    "FORUM",
                                    ignoreCase = true
                                ) ||
                                item.plannable.title.contains(
                                    "DEBATE",
                                    ignoreCase = true
                                )
                    matchesCourse && isForum
                }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks for the selected date in weekly schedule (if any)
    val selectedDateAssignments: StateFlow<List<AssignmentUiModel>> =
        combine(allAssignmentsUi, _selectedDate) { all, date ->
            if (date == null) emptyList()
            else
                all.filter { ui ->
                    ui.assignment.dueAt?.let {
                        try {
                            val localDue =
                                ZonedDateTime.parse(it)
                                    .withZoneSameInstant(
                                        java.time.ZoneId.systemDefault()
                                    )
                                    .toLocalDate()
                            localDue == date
                        } catch (e: Exception) {
                            false
                        }
                    }
                        ?: false
                }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    fun inspectCourse(course: Course?) {
        _inspectedCourse.value = course
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchAndSaveData()
                loadProfile()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
