package com.notivas.data.remote.openrouter

import com.google.gson.annotations.SerializedName

data class OpenRouterChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OpenRouterMessage>,
    @SerializedName("tools") val tools: List<OpenRouterTool>? = null,
    @SerializedName("temperature") val temperature: Double? = 0.3,
    @SerializedName("max_tokens") val maxTokens: Int? = 1500
)

data class OpenRouterMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<OpenRouterToolCall>? = null
)

data class OpenRouterTool(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: OpenRouterFunction
)

data class OpenRouterFunction(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: OpenRouterParameters
)

data class OpenRouterParameters(
    @SerializedName("type") val type: String = "object",
    @SerializedName("properties") val properties: Map<String, OpenRouterProperty>,
    @SerializedName("required") val required: List<String> = emptyList()
)

data class OpenRouterProperty(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("enum") val enumValues: List<String>? = null
)

data class OpenRouterToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: OpenRouterFunctionCall
)

data class OpenRouterFunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
)

data class OpenRouterChatResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("choices") val choices: List<OpenRouterChoice>? = null,
    @SerializedName("usage") val usage: OpenRouterUsage? = null,
    @SerializedName("error") val error: OpenRouterError? = null
)

data class OpenRouterChoice(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("message") val message: OpenRouterMessage,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class OpenRouterUsage(
    @SerializedName("prompt_tokens") val promptTokens: Int? = null,
    @SerializedName("completion_tokens") val completionTokens: Int? = null,
    @SerializedName("total_tokens") val totalTokens: Int? = null
)

data class OpenRouterError(
    @SerializedName("code") val code: Any? = null,
    @SerializedName("message") val message: String? = null
)

data class OpenRouterCreditsResponse(
    @SerializedName("data") val data: OpenRouterCreditsData? = null
)

data class OpenRouterCreditsData(
    @SerializedName("total_credits") val totalCredits: Double? = null,
    @SerializedName("total_usage") val totalUsage: Double? = null
)

data class OpenRouterKeyResponse(
    @SerializedName("data") val data: OpenRouterKeyData? = null
)

data class OpenRouterKeyData(
    @SerializedName("label") val label: String? = null,
    @SerializedName("usage") val usage: Double? = null,
    @SerializedName("limit") val limit: Double? = null,
    @SerializedName("is_free_tier") val isFreeTier: Boolean? = null
)

