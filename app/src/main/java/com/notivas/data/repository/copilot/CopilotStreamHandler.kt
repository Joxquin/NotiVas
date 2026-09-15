package com.notivas.data.repository.Ananau

import android.util.Log
import com.notivas.data.remote.openrouter.OpenRouterApiService
import com.notivas.data.remote.openrouter.OpenRouterChatRequest
import com.notivas.data.remote.openrouter.OpenRouterMessage
import com.notivas.data.repository.AnanauResult
import com.notivas.data.repository.AnanauSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnanauStreamHandler @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val toolExecutor: AnanauToolExecutor,
    private val messageMapper: AnanauMessageMapper
) {

    suspend fun executeChatLoop(
        authHeader: String,
        model: String,
        messages: MutableList<OpenRouterMessage>,
        selectedCourseId: Long?
    ): Result<AnanauResult> {
        val sourcesConsulted = mutableListOf<AnanauSource>()
        var actionFeedback: String? = null

        var promptTokensAccumulated = 0
        var completionTokensAccumulated = 0
        var totalTokensAccumulated = 0
        var finalReply: String? = null

        val maxTurns = 3
        var turn = 0

        try {
            while (turn < maxTurns) {
                turn++

                val request = OpenRouterChatRequest(
                    model = model,
                    messages = messages,
                    tools = toolExecutor.tools,
                    temperature = 0.2
                )

                val response = openRouterApiService.chatCompletion(
                    authorization = authHeader,
                    request = request
                )

                if (!response.isSuccessful) {
                    val errBody = response.errorBody()?.string() ?: "Error de conexión"
                    return Result.failure(Exception(messageMapper.formatOpenRouterError(response.code(), errBody)))
                }

                val chatResponse = response.body()
                val choice = chatResponse?.choices?.firstOrNull()
                    ?: return Result.failure(Exception("Respuesta vacía del modelo."))

                val responseMessage = choice.message
                val toolCalls = responseMessage.toolCalls
                val usage = chatResponse.usage

                val pTok = usage?.promptTokens ?: 0
                val cTok = usage?.completionTokens ?: 0
                val tTok = usage?.totalTokens ?: (pTok + cTok)

                promptTokensAccumulated += pTok
                completionTokensAccumulated += cTok
                totalTokensAccumulated += tTok

                // If no tools needed, this is the final response
                if (toolCalls.isNullOrEmpty()) {
                    finalReply = responseMessage.content ?: "No pude procesar una respuesta."
                    break
                }

                // Execute tools requested by LLM
                messages.add(responseMessage)

                for (toolCall in toolCalls) {
                    val functionName = toolCall.function.name
                    val rawArgs = toolCall.function.arguments

                    val execResult = toolExecutor.executeTool(
                        functionName = functionName,
                        rawArgs = rawArgs,
                        selectedCourseId = selectedCourseId
                    )

                    execResult.source?.let { sourcesConsulted.add(it) }
                    if (execResult.actionFeedback != null) {
                        actionFeedback = execResult.actionFeedback
                    }

                    messages.add(
                        OpenRouterMessage(
                            role = "tool",
                            name = functionName,
                            toolCallId = toolCall.id,
                            content = execResult.resultJson
                        )
                    )
                }
            }

            if (finalReply.isNullOrBlank()) {
                if (sourcesConsulted.isNotEmpty()) {
                    finalReply = buildString {
                        append("He consultado la siguiente información de Canvas LMS:\n\n")
                        sourcesConsulted.forEach { src ->
                            append("* **${src.title}**: ${src.detail}\n")
                        }
                    }
                } else {
                    finalReply = "Se procesó la consulta con Canvas LMS, pero no se generó texto de respuesta adicional."
                }
            }

            return Result.success(
                AnanauResult(
                    reply = finalReply,
                    sources = sourcesConsulted,
                    actionFeedback = actionFeedback,
                    promptTokens = promptTokensAccumulated,
                    completionTokens = completionTokensAccumulated,
                    totalTokens = totalTokensAccumulated
                )
            )
        } catch (e: Exception) {
            Log.e("AnanauStreamHandler", "Error during chat execution loop", e)
            return Result.failure(e)
        }
    }
}
