package com.notivas.ui.copilot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Course
import com.notivas.data.remote.openrouter.OpenRouterMessage
import com.notivas.data.repository.CanvasRepository
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
    val errorMessage: String? = null
)

@HiltViewModel
class CopilotViewModel @Inject constructor(
    private val copilotRepository: CopilotRepository,
    private val canvasRepository: CanvasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    private val _messages = MutableStateFlow<List<CopilotMessageItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _inputText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CopilotUiState> = combine(
        canvasRepository.allCourses,
        _selectedCourseId,
        _messages,
        _isLoading,
        _inputText
    ) { courses, selectedCourseId, messages, isLoading, inputText ->
        Tuple5(courses, selectedCourseId, messages, isLoading, inputText)
    }.combine(
        combine(
            preferencesManager.copilotEnabled,
            preferencesManager.openRouterApiKey,
            preferencesManager.openRouterModel,
            _errorMessage
        ) { enabled, apiKey, model, error ->
            Tuple4(enabled, !apiKey.isNullOrBlank(), model, error)
        }
    ) { (courses, selectedCourseId, messages, isLoading, inputText),
        (enabled, hasApiKey, model, error) ->
        CopilotUiState(
            courses = courses,
            selectedCourseId = selectedCourseId,
            messages = messages,
            isLoading = isLoading,
            isCopilotEnabled = enabled,
            hasApiKey = hasApiKey,
            currentModel = model,
            inputText = inputText,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CopilotUiState()
    )

    fun selectCourse(courseId: Long?) {
        _selectedCourseId.value = courseId
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearConversation() {
        _messages.value = emptyList()
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _inputText.value).trim()
        if (prompt.isBlank() || _isLoading.value) return

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
                },
                onFailure = { error ->
                    val errorText = error.message ?: "Ocurrió un error inesperado."
                    _errorMessage.value = errorText
                    val assistantErrorMessage = CopilotMessageItem(
                        role = CopilotRole.ASSISTANT,
                        text = "⚠️ No se pudo procesar tu solicitud: $errorText\n\nPor favor, verifica tu API Key de OpenRouter y tu conexión."
                    )
                    _messages.value = _messages.value + assistantErrorMessage
                }
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
