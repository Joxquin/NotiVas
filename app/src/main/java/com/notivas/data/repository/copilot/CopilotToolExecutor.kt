package com.notivas.data.repository.Ananau

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.AnanauSource
import com.notivas.data.repository.Ananau.tools.handlers.CanvasAcademicToolsHandler
import com.notivas.data.repository.Ananau.tools.handlers.CanvasDiscussionsToolsHandler
import com.notivas.data.repository.Ananau.tools.handlers.CanvasModulesToolsHandler
import com.notivas.data.repository.Ananau.tools.handlers.SimulatorToolsHandler
import javax.inject.Inject
import javax.inject.Singleton

data class ToolExecutionResult(
    val resultJson: String,
    val source: AnanauSource? = null,
    val actionFeedback: String? = null
)

@Singleton
class AnanauToolExecutor @Inject constructor(
    canvasAcademicToolsHandler: CanvasAcademicToolsHandler,
    canvasModulesToolsHandler: CanvasModulesToolsHandler,
    canvasDiscussionsToolsHandler: CanvasDiscussionsToolsHandler,
    simulatorToolsHandler: SimulatorToolsHandler
) {
    private val gson = Gson()

    private val handlers = listOf(
        canvasAcademicToolsHandler,
        canvasModulesToolsHandler,
        canvasDiscussionsToolsHandler,
        simulatorToolsHandler
    )

    val tools: List<OpenRouterTool> = handlers.flatMap { it.supportedTools }

    suspend fun executeTool(
        functionName: String,
        rawArgs: String,
        selectedCourseId: Long?
    ): ToolExecutionResult {
        val args = try {
            gson.fromJson(rawArgs, JsonObject::class.java)
        } catch (e: Exception) {
            JsonObject()
        }

        val handler = handlers.firstOrNull { it.canHandle(functionName) }
        return handler?.execute(functionName, args, selectedCourseId)
            ?: ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no reconocida: $functionName")))
    }
}
