package com.notivas.data.repository.copilot.tools

import com.google.gson.JsonObject
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.copilot.ToolExecutionResult

interface CopilotToolHandler {
    val supportedTools: List<OpenRouterTool>
    fun canHandle(toolName: String): Boolean
    suspend fun execute(toolName: String, args: JsonObject, selectedCourseId: Long?): ToolExecutionResult
}
