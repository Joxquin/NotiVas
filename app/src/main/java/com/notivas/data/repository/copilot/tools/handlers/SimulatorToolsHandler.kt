package com.notivas.data.repository.copilot.tools.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.SimulationDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.model.SimulationGroup
import com.notivas.data.remote.openrouter.OpenRouterFunction
import com.notivas.data.remote.openrouter.OpenRouterParameters
import com.notivas.data.remote.openrouter.OpenRouterProperty
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.CopilotSource
import com.notivas.data.repository.copilot.ToolExecutionResult
import com.notivas.data.repository.copilot.tools.CopilotToolHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatorToolsHandler @Inject constructor(
    private val simulationDao: SimulationDao,
    private val courseDao: CourseDao
) : CopilotToolHandler {

    private val gson = Gson()

    override val supportedTools: List<OpenRouterTool> = listOf(
        OpenRouterTool(
            function = OpenRouterFunction(
                name = "create_simulation_group",
                description = "Crea un nuevo grupo de evaluación ponderado en el Simulador de Notas local de NotiVas (ejemplo: 'Laboratorios', 'Exámenes', 'Trabajo Final').",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas del curso al que pertenece el grupo"
                        ),
                        "name" to OpenRouterProperty(
                            type = "string",
                            description = "Nombre del grupo de evaluación"
                        ),
                        "weight_percentage" to OpenRouterProperty(
                            type = "number",
                            description = "Porcentaje de ponderación (ej: 25.0 para 25%)"
                        )
                    ),
                    required = listOf("course_id", "name", "weight_percentage")
                )
            )
        )
    )

    override fun canHandle(toolName: String): Boolean {
        return toolName == "create_simulation_group"
    }

    override suspend fun execute(
        toolName: String,
        args: JsonObject,
        selectedCourseId: Long?
    ): ToolExecutionResult {
        return when (toolName) {
            "create_simulation_group" -> {
                val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                val name = args.get("name")?.asString ?: "Nuevo Grupo"
                val weight = args.get("weight_percentage")?.asFloat ?: 0f
                val newGroupId = simulationDao.insertGroup(
                    SimulationGroup(
                        courseId = cid,
                        name = name,
                        weightPercentage = weight
                    )
                )
                val courses = courseDao.getCourseList()
                val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                val actionFeedback = "Grupo '$name' ($weight%) creado en el Simulador de $courseName."
                val source = CopilotSource(
                    title = "Simulador de Notas",
                    detail = "Creado grupo '$name' con ponderación $weight%"
                )
                val json = gson.toJson(mapOf("success" to true, "group_id" to newGroupId, "message" to actionFeedback))
                ToolExecutionResult(json, source, actionFeedback)
            }
            else -> ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no soportada por SimulatorToolsHandler")))
        }
    }
}
