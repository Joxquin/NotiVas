package com.notivas.data.repository

import android.util.Log
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.remote.openrouter.OpenRouterApiService
import com.notivas.data.remote.openrouter.OpenRouterMessage
import com.notivas.data.repository.copilot.CopilotPromptBuilder
import com.notivas.data.repository.copilot.CopilotStreamHandler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class CopilotSource(
    val title: String,
    val detail: String
)

data class CopilotResult(
    val reply: String,
    val sources: List<CopilotSource> = emptyList(),
    val actionFeedback: String? = null,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

data class OpenRouterAccountBalance(
    val totalCredits: Double? = null,
    val totalUsage: Double? = null,
    val remainingCredits: Double? = null,
    val isFreeTier: Boolean? = null,
    val limit: Double? = null
)

@Singleton
class CopilotRepository @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val courseDao: CourseDao,
    private val preferencesManager: PreferencesManager,
    private val promptBuilder: CopilotPromptBuilder,
    private val streamHandler: CopilotStreamHandler
) {

    suspend fun queryCopilot(
        history: List<OpenRouterMessage>,
        userPrompt: String,
        selectedCourseId: Long? = null
    ): Result<CopilotResult> {
        val apiKey = preferencesManager.openRouterApiKey.first()
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("OpenRouter API Key no configurada. Ve a tu Perfil para agregarla."))
        }

        val model = preferencesManager.openRouterModel.first()
        val courses = courseDao.getCourseList()

        val systemPrompt = promptBuilder.buildSystemPrompt(
            courses = courses,
            selectedCourseId = selectedCourseId
        )

        val messages = promptBuilder.buildConversationMessages(
            systemPrompt = systemPrompt,
            history = history,
            userPrompt = userPrompt
        )

        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

        val result = streamHandler.executeChatLoop(
            authHeader = authHeader,
            model = model,
            messages = messages,
            selectedCourseId = selectedCourseId
        )

        result.onSuccess { copilotResult ->
            preferencesManager.addCopilotTokens(copilotResult.totalTokens.toLong())
        }

        return result
    }

    suspend fun getOpenRouterBalance(): OpenRouterAccountBalance? {
        val apiKey = preferencesManager.openRouterApiKey.first() ?: return null
        if (apiKey.isBlank()) return null
        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

        return try {
            val creditsResponse = openRouterApiService.getCredits(authHeader)
            val keyResponse = openRouterApiService.getKeyInfo(authHeader)

            val creditsData = if (creditsResponse.isSuccessful) creditsResponse.body()?.data else null
            val keyData = if (keyResponse.isSuccessful) keyResponse.body()?.data else null

            val totalCredits = creditsData?.totalCredits
            val totalUsage = creditsData?.totalUsage ?: keyData?.usage
            val remaining = if (totalCredits != null && totalUsage != null) {
                (totalCredits - totalUsage).coerceAtLeast(0.0)
            } else if (keyData?.limit != null && keyData.usage != null) {
                (keyData.limit - keyData.usage).coerceAtLeast(0.0)
            } else null

            OpenRouterAccountBalance(
                totalCredits = totalCredits,
                totalUsage = totalUsage,
                remainingCredits = remaining,
                isFreeTier = keyData?.isFreeTier,
                limit = keyData?.limit
            )
        } catch (e: Exception) {
            Log.e("CopilotRepository", "Error fetching OpenRouter balance", e)
            null
        }
    }
}
