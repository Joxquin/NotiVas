package com.notivas.ui.copilot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.model.CanvasModule
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.remote.openrouter.OpenRouterMessage
import com.notivas.data.model.CopilotSession
import com.notivas.data.repository.CanvasRepository
import com.notivas.data.repository.CopilotChatRepository
import com.notivas.data.repository.CopilotRepository
import com.notivas.data.repository.CopilotSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class CopilotRole {
    USER, ASSISTANT
}

enum class MentionStep {
    COURSES,
    COURSE_RESOURCES
}

data class CopilotMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val role: CopilotRole,
    val text: String,
    val sources: List<CopilotSource> = emptyList(),
    val actionFeedback: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CopilotUiState(
    val courses: List<Course> = emptyList(),
    val selectedCourseId: Long? = null,
    val messages: List<CopilotMessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isCopilotEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val currentModel: String = "google/gemini-2.5-flash",
    val inputText: String = "",
    val errorMessage: String? = null,
    // Interactive @ mention state
    val showMentionMenu: Boolean = false,
    val mentionStep: MentionStep = MentionStep.COURSES,
    val mentionQuery: String = "",
    val activeMentionCourse: Course? = null,
    val courseAssignments: List<Assignment> = emptyList(),
    val coursePlannerItems: List<PlannerItem> = emptyList(),
    val courseModules: List<CanvasModule> = emptyList(),
    // Chat sessions and history
    val currentSessionId: String? = null,
    val savedSessions: List<CopilotSession> = emptyList(),
    val showHistorySheet: Boolean = false
)

@HiltViewModel
class CopilotViewModel @Inject constructor(
    private val copilotRepository: CopilotRepository,
    private val canvasRepository: CanvasRepository,
    private val copilotChatRepository: CopilotChatRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    private val _messages = MutableStateFlow<List<CopilotMessageItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _inputText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _currentSessionId = MutableStateFlow<String?>(null)
    private val _showHistorySheet = MutableStateFlow(false)

    // @ Mention State
    private val _showMentionMenu = MutableStateFlow(false)
    private val _mentionStep = MutableStateFlow(MentionStep.COURSES)
    private val _mentionQuery = MutableStateFlow("")
    private val _activeMentionCourse = MutableStateFlow<Course?>(null)
    private val _courseAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    private val _coursePlannerItems = MutableStateFlow<List<PlannerItem>>(emptyList())
    private val _courseModules = MutableStateFlow<List<CanvasModule>>(emptyList())

    val uiState: StateFlow<CopilotUiState> = combine(
        combine(
            canvasRepository.allCourses,
            _selectedCourseId,
            _messages,
            _isLoading,
            _inputText
        ) { courses, selectedCourseId, messages, isLoading, inputText ->
            Tuple5(courses, selectedCourseId, messages, isLoading, inputText)
        },
        combine(
            preferencesManager.copilotEnabled,
            preferencesManager.openRouterApiKey,
            preferencesManager.openRouterModel,
            _errorMessage
        ) { enabled, apiKey, model, error ->
            Tuple4(enabled, !apiKey.isNullOrBlank(), model, error)
        },
        combine(
            _showMentionMenu,
            _mentionStep,
            _mentionQuery,
            _activeMentionCourse
        ) { showMenu, step, query, activeCourse ->
            Tuple4(showMenu, step, query, activeCourse)
        },
        combine(
            combine(
                _courseAssignments,
                _coursePlannerItems,
                _courseModules
            ) { assignments, plannerItems, modules ->
                Triple(assignments, plannerItems, modules)
            },
            _currentSessionId,
            copilotChatRepository.allSessions,
            _showHistorySheet
        ) { (assignments, plannerItems, modules), currentSessionId, savedSessions, showHistory ->
            Tuple4(Triple(assignments, plannerItems, modules), currentSessionId, savedSessions, showHistory)
        }
    ) { (courses, selectedCourseId, messages, isLoading, inputText),
        (enabled, hasApiKey, model, error),
        (showMenu, step, query, activeCourse),
        (resources, currentSessionId, savedSessions, showHistory) ->
        val (assignments, plannerItems, modules) = resources
        CopilotUiState(
            courses = courses,
            selectedCourseId = selectedCourseId,
            messages = messages,
            isLoading = isLoading,
            isCopilotEnabled = enabled,
            hasApiKey = hasApiKey,
            currentModel = model,
            inputText = inputText,
            errorMessage = error,
            showMentionMenu = showMenu,
            mentionStep = step,
            mentionQuery = query,
            activeMentionCourse = activeCourse,
            courseAssignments = assignments,
            coursePlannerItems = plannerItems,
            courseModules = modules,
            currentSessionId = currentSessionId,
            savedSessions = savedSessions,
            showHistorySheet = showHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CopilotUiState()
    )

    fun openHistorySheet() {
        _showHistorySheet.value = true
    }

    fun dismissHistorySheet() {
        _showHistorySheet.value = false
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _messages.value = emptyList()
        _inputText.value = ""
        _errorMessage.value = null
        _showHistorySheet.value = false
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = copilotChatRepository.getSessionById(sessionId)
            val messages = copilotChatRepository.getMessagesForSession(sessionId)
            _currentSessionId.value = sessionId
            _selectedCourseId.value = session?.courseId
            _messages.value = messages
            _showHistorySheet.value = false
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                copilotChatRepository.renameSession(sessionId, trimmed)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            copilotChatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewChat()
            }
        }
    }

    fun selectCourse(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun updateInputText(text: String) {
        _inputText.value = text

        // Check if cursor/last text ends with an '@' symbol or partial mention token
        val lastAtIndex = text.lastIndexOf('@')
        if (lastAtIndex != -1) {
            val afterAt = text.substring(lastAtIndex + 1)
            // If after '@' there is no space, user is actively typing a mention
            if (!afterAt.contains(' ')) {
                _mentionQuery.value = afterAt
                if (!_showMentionMenu.value) {
                    _showMentionMenu.value = true
                    _mentionStep.value = MentionStep.COURSES
                }
            } else {
                // If there is a space after @, close popup unless in resource step
                if (_mentionStep.value == MentionStep.COURSES) {
                    _showMentionMenu.value = false
                }
            }
        } else {
            _showMentionMenu.value = false
            _activeMentionCourse.value = null
        }
    }

    fun triggerAtMention() {
        val currentText = _inputText.value
        val newText = if (currentText.isEmpty() || currentText.endsWith(" ")) {
            "$currentText@"
        } else {
            "$currentText @"
        }
        _inputText.value = newText
        _mentionQuery.value = ""
        _mentionStep.value = MentionStep.COURSES
        _showMentionMenu.value = true
    }

    fun selectMentionCourse(course: Course) {
        _activeMentionCourse.value = course
        _selectedCourseId.value = course.id
        _mentionStep.value = MentionStep.COURSE_RESOURCES
        _courseModules.value = emptyList()

        // Load assignments, planner items & modules for this course
        viewModelScope.launch {
            canvasRepository.getAssignmentsForCourse(course.id).firstOrNull()?.let {
                _courseAssignments.value = it
            }
            canvasRepository.getPlannerItemsForCourse(course.id).firstOrNull()?.let {
                _coursePlannerItems.value = it
            }
            _courseModules.value = canvasRepository.fetchCourseModules(course.id)
        }
    }

    fun backToCourseSelection() {
        _mentionStep.value = MentionStep.COURSES
        _activeMentionCourse.value = null
        _courseModules.value = emptyList()
    }

    fun applyCourseMention(course: Course) {
        val currentText = _inputText.value
        val lastAtIndex = currentText.lastIndexOf('@')
        val prefix = if (lastAtIndex != -1) currentText.substring(0, lastAtIndex) else currentText
        val tag = "@[${course.courseCode ?: course.name}] "
        _inputText.value = prefix + tag
        _selectedCourseId.value = course.id
        dismissMentionMenu()
    }

    fun applyResourceMention(course: Course, resourceName: String) {
        val currentText = _inputText.value
        val lastAtIndex = currentText.lastIndexOf('@')
        val prefix = if (lastAtIndex != -1) currentText.substring(0, lastAtIndex) else currentText
        val courseLabel = course.courseCode ?: course.name
        val tag = "@[$courseLabel > $resourceName] "
        _inputText.value = prefix + tag
        _selectedCourseId.value = course.id
        dismissMentionMenu()
    }

    fun dismissMentionMenu() {
        _showMentionMenu.value = false
        _mentionStep.value = MentionStep.COURSES
        _activeMentionCourse.value = null
        _mentionQuery.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearConversation() {
        startNewChat()
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _inputText.value).trim()
        if (prompt.isBlank() || _isLoading.value) return

        dismissMentionMenu()

        // Ensure session exists or create one
        val sessionId = _currentSessionId.value ?: UUID.randomUUID().toString().also {
            _currentSessionId.value = it
        }

        val userMessage = CopilotMessageItem(
            role = CopilotRole.USER,
            text = prompt
        )

        val updatedMessages = _messages.value + userMessage
        _messages.value = updatedMessages
        _inputText.value = ""
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // If it's a new session, create session record with first prompt as initial title
            val currentSession = copilotChatRepository.getSessionById(sessionId)
            if (currentSession == null) {
                val title = prompt.take(40) + if (prompt.length > 40) "..." else ""
                copilotChatRepository.createOrUpdateSession(
                    sessionId = sessionId,
                    title = title,
                    courseId = _selectedCourseId.value
                )
            }
            // Save user message
            copilotChatRepository.saveMessage(sessionId, userMessage)

            val history = updatedMessages
                .dropLast(1)
                .map { msg ->
                    OpenRouterMessage(
                        role = if (msg.role == CopilotRole.USER) "user" else "assistant",
                        content = msg.text
                    )
                }

            val result = copilotRepository.queryCopilot(
                history = history,
                userPrompt = prompt,
                selectedCourseId = _selectedCourseId.value
            )

            _isLoading.value = false

            result.fold(
                onSuccess = { response ->
                    val assistantMessage = CopilotMessageItem(
                        role = CopilotRole.ASSISTANT,
                        text = response.reply,
                        sources = response.sources,
                        actionFeedback = response.actionFeedback
                    )
                    _messages.value = _messages.value + assistantMessage
                    copilotChatRepository.saveMessage(sessionId, assistantMessage)
                },
                onFailure = { error ->
                    val errorText = error.message ?: "Ocurrió un error inesperado."
                    _errorMessage.value = errorText
                    val assistantErrorMessage = CopilotMessageItem(
                        role = CopilotRole.ASSISTANT,
                        text = "⚠️ No se pudo procesar tu solicitud: $errorText\n\nPor favor, verifica tu API Key de OpenRouter y tu conexión."
                    )
                    _messages.value = _messages.value + assistantErrorMessage
                    copilotChatRepository.saveMessage(sessionId, assistantErrorMessage)
                }
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
    private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
}
