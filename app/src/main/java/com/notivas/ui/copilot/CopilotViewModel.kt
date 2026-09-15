package com.notivas.ui.Ananau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.model.CanvasModule
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.remote.openrouter.OpenRouterMessage
import com.notivas.data.model.AnanauSession
import com.notivas.data.repository.CanvasRepository
import com.notivas.data.repository.AnanauChatRepository
import com.notivas.data.repository.AnanauRepository
import com.notivas.data.repository.AnanauSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class AnanauRole {
    USER, ASSISTANT
}

enum class MentionStep {
    COURSES,
    COURSE_RESOURCES
}

data class AnanauMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val role: AnanauRole,
    val text: String,
    val sources: List<AnanauSource> = emptyList(),
    val actionFeedback: String? = null,
    val tokens: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class AnanauUiState(
    val courses: List<Course> = emptyList(),
    val selectedCourseId: Long? = null,
    val messages: List<AnanauMessageItem> = emptyList(),
    val isLoading: Boolean = false,
    val isAnanauEnabled: Boolean = false,
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
    val savedSessions: List<AnanauSession> = emptyList(),
    val showHistorySheet: Boolean = false,
    // Token & Credit stats
    val sessionTokens: Int = 0,
    val totalAccountTokens: Long = 0L,
    val openRouterBalance: com.notivas.data.repository.OpenRouterAccountBalance? = null
)

@HiltViewModel
class AnanauViewModel @Inject constructor(
    private val AnanauRepository: AnanauRepository,
    private val canvasRepository: CanvasRepository,
    private val AnanauChatRepository: AnanauChatRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    private val _messages = MutableStateFlow<List<AnanauMessageItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _inputText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _currentSessionId = MutableStateFlow<String?>(null)
    private val _showHistorySheet = MutableStateFlow(false)
    private val _sessionTokens = MutableStateFlow(0)
    private val _openRouterBalance = MutableStateFlow<com.notivas.data.repository.OpenRouterAccountBalance?>(null)

    // @ Mention State
    private val _showMentionMenu = MutableStateFlow(false)
    private val _mentionStep = MutableStateFlow(MentionStep.COURSES)
    private val _mentionQuery = MutableStateFlow("")
    private val _activeMentionCourse = MutableStateFlow<Course?>(null)
    private val _courseAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    private val _coursePlannerItems = MutableStateFlow<List<PlannerItem>>(emptyList())
    private val _courseModules = MutableStateFlow<List<CanvasModule>>(emptyList())

    init {
        refreshOpenRouterBalance()
    }

    fun refreshOpenRouterBalance() {
        viewModelScope.launch {
            _openRouterBalance.value = AnanauRepository.getOpenRouterBalance()
        }
    }

    val uiState: StateFlow<AnanauUiState> = combine(
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
            preferencesManager.AnanauEnabled,
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
            combine(
                _currentSessionId,
                AnanauChatRepository.allSessions,
                _showHistorySheet
            ) { currentSessionId, savedSessions, showHistory ->
                Triple(currentSessionId, savedSessions, showHistory)
            },
            combine(
                _sessionTokens,
                preferencesManager.totalAnanauTokens,
                _openRouterBalance
            ) { sTokens, tTokens, balance ->
                Triple(sTokens, tTokens, balance)
            }
        ) { resources, sessionInfo, tokenInfo ->
            Triple(resources, sessionInfo, tokenInfo)
        }
    ) { (courses, selectedCourseId, messages, isLoading, inputText),
        (enabled, hasApiKey, model, error),
        (showMenu, step, query, activeCourse),
        (resources, sessionInfo, tokenInfo) ->
        val (assignments, plannerItems, modules) = resources
        val (currentSessionId, savedSessions, showHistory) = sessionInfo
        val (sessionTokens, totalTokens, balance) = tokenInfo
        AnanauUiState(
            courses = courses,
            selectedCourseId = selectedCourseId,
            messages = messages,
            isLoading = isLoading,
            isAnanauEnabled = enabled,
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
            showHistorySheet = showHistory,
            sessionTokens = sessionTokens,
            totalAccountTokens = totalTokens,
            openRouterBalance = balance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnanauUiState()
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
        _sessionTokens.value = 0
        _inputText.value = ""
        _errorMessage.value = null
        _showHistorySheet.value = false
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = AnanauChatRepository.getSessionById(sessionId)
            val messages = AnanauChatRepository.getMessagesForSession(sessionId)
            _currentSessionId.value = sessionId
            _selectedCourseId.value = session?.courseId
            _messages.value = messages
            // Restore total tokens used in this session
            val sessionTokens = if (session != null && session.totalTokens > 0) {
                session.totalTokens
            } else {
                messages.sumOf { it.tokens }
            }
            _sessionTokens.value = sessionTokens
            _showHistorySheet.value = false
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                AnanauChatRepository.renameSession(sessionId, trimmed)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            AnanauChatRepository.deleteSession(sessionId)
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

        val userMessage = AnanauMessageItem(
            role = AnanauRole.USER,
            text = prompt
        )

        val updatedMessages = _messages.value + userMessage
        _messages.value = updatedMessages
        _inputText.value = ""
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // If it's a new session, create session record with first prompt as initial title
            val currentSession = AnanauChatRepository.getSessionById(sessionId)
            if (currentSession == null) {
                val title = prompt.take(40) + if (prompt.length > 40) "..." else ""
                AnanauChatRepository.createOrUpdateSession(
                    sessionId = sessionId,
                    title = title,
                    courseId = _selectedCourseId.value
                )
            }
            // Save user message
            AnanauChatRepository.saveMessage(sessionId, userMessage)

            val history = updatedMessages
                .dropLast(1)
                .map { msg ->
                    OpenRouterMessage(
                        role = if (msg.role == AnanauRole.USER) "user" else "assistant",
                        content = msg.text
                    )
                }

            val result = AnanauRepository.queryAnanau(
                history = history,
                userPrompt = prompt,
                selectedCourseId = _selectedCourseId.value
            )

            _isLoading.value = false

            result.fold(
                onSuccess = { response ->
                    val assistantMessage = AnanauMessageItem(
                        role = AnanauRole.ASSISTANT,
                        text = response.reply,
                        sources = response.sources,
                        actionFeedback = response.actionFeedback,
                        tokens = response.totalTokens
                    )
                    _messages.value = _messages.value + assistantMessage
                    val newSessionTokens = _sessionTokens.value + response.totalTokens
                    _sessionTokens.value = newSessionTokens
                    AnanauChatRepository.saveMessage(sessionId, assistantMessage)
                    AnanauChatRepository.updateSessionTokens(sessionId, newSessionTokens)
                    refreshOpenRouterBalance()
                },
                onFailure = { error ->
                    val errorText = error.message ?: "Ocurrió un error inesperado."
                    _errorMessage.value = errorText
                    val assistantErrorMessage = AnanauMessageItem(
                        role = AnanauRole.ASSISTANT,
                        text = "⚠️ No se pudo procesar tu solicitud: $errorText\n\nPor favor, verifica tu API Key de OpenRouter y tu conexión."
                    )
                    _messages.value = _messages.value + assistantErrorMessage
                    AnanauChatRepository.saveMessage(sessionId, assistantErrorMessage)
                }
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
    private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
}
