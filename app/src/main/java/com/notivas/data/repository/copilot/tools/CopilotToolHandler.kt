package com.notivas.data.repository.Ananau.tools

import com.google.gson.JsonObject
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.Ananau.ToolExecutionResult

interface AnanauToolHandler {
    val supportedTools: List<OpenRouterTool>
    fun canHandle(toolName: String): Boolean
    suspend fun execute(toolName: String, args: JsonObject, selectedCourseId: Long?): ToolExecutionResult
}
