package com.notivas.data.repository

import android.os.Build
import android.text.Html
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.dao.SimulationDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.SimulationGroup
import com.notivas.data.remote.CanvasApiService
import com.notivas.data.remote.openrouter.*
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
    val actionFeedback: String? = null
)

@Singleton
class CopilotRepository @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val canvasApiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val assignmentDao: AssignmentDao,
    private val simulationDao: SimulationDao,
    private val preferencesManager: PreferencesManager
) {
    private val gson = Gson()

    private val tools = listOf(
        OpenRouterTool(
            function = OpenRouterFunction(
                name = "get_academic_overview",
                description = "Obtiene la lista de cursos registrados con sus IDs, códigos y resumen de tareas guardadas localmente en el dispositivo.",
                parameters = OpenRouterParameters(
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        ),
        OpenRouterTool(
            function = OpenRouterFunction(
                name = "get_course_assignments",
                description = "Obtiene la lista de tareas de un curso específico por su course_id, indicando fechas de entrega, estado de envío y calificaciones obtenidas.",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas del curso"
                        )
                    ),
                    required = listOf("course_id")
                )
            )
        ),
        OpenRouterTool(
            function = OpenRouterFunction(
                name = "fetch_canvas_assignment_details",
                description = "Consulta directamente a la API de Canvas LMS para obtener la consigna completa y la rúbrica de evaluación detallada (criterios y puntuaciones) de una tarea.",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas del curso"
                        ),
                        "assignment_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas de la tarea"
                        )
                    ),
                    required = listOf("course_id", "assignment_id")
                )
            )
        ),
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

        // 1. Prepare system prompt and injected context
        val courses = courseDao.getCourseList()
        val coursesSummary = courses.joinToString("; ") { "ID: ${it.id} - ${it.name} (${it.courseCode ?: "N/A"})" }

        val systemPrompt = buildString {
            append("Eres NotiVas Copilot, un asistente académico inteligente, empático y estructurado para estudiantes universitarios integrados con Canvas LMS. ")
            append("Respondes en español con formato Markdown limpio (viñetas, negritas, tablas si es necesario). ")
            append("Cuentas con herramientas para consultar información local y en vivo de Canvas LMS. ")
            append("Cursos inscritos del estudiante: [$coursesSummary]. ")
            if (selectedCourseId != null) {
                append("El estudiante tiene seleccionado actualmente el curso ID: $selectedCourseId. Prioriza este curso en tus respuestas a menos que pregunte por otro. ")
            }
            append("Si te piden detalles de rúbricas o consignas específicas, usa fetch_canvas_assignment_details. ")
            append("Si te piden crear grupos de notas, usa create_simulation_group. ")
            append("Sé conciso, directo y útil.")
        }

        val messages = mutableListOf<OpenRouterMessage>()
        messages.add(OpenRouterMessage(role = "system", content = systemPrompt))
        messages.addAll(history)
        messages.add(OpenRouterMessage(role = "user", content = userPrompt))

        val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"

        val sourcesConsulted = mutableListOf<CopilotSource>()
        var actionFeedback: String? = null

        try {
            // First LLM call
            val request = OpenRouterChatRequest(
                model = model,
                messages = messages,
                tools = tools,
                temperature = 0.2
            )

            val response = openRouterApiService.chatCompletion(
                authorization = authHeader,
                request = request
            )

            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string() ?: "Error de conexión"
                return Result.failure(Exception(formatOpenRouterError(response.code(), errBody)))
            }

            val chatResponse = response.body()
            val choice = chatResponse?.choices?.firstOrNull()
                ?: return Result.failure(Exception("Respuesta vacía del modelo."))

            val responseMessage = choice.message
            val toolCalls = responseMessage.toolCalls

            // If no tools needed, return answer directly
            if (toolCalls.isNullOrEmpty()) {
                val content = responseMessage.content ?: "No pude procesar una respuesta."
                return Result.success(CopilotResult(reply = content))
            }

            // Execute tools requested by LLM
            messages.add(responseMessage)

            for (toolCall in toolCalls) {
                val functionName = toolCall.function.name
                val rawArgs = toolCall.function.arguments
                val args = try {
                    gson.fromJson(rawArgs, JsonObject::class.java)
                } catch (e: Exception) {
                    JsonObject()
                }

                val toolResultString = when (functionName) {
                    "get_academic_overview" -> {
                        val allAssignments = assignmentDao.getAssignmentList()
                        val courseSummaries = courses.map { c ->
                            val cAssignments = allAssignments.filter { it.courseId == c.id }
                            mapOf(
                                "id" to c.id,
                                "name" to c.name,
                                "assignments_total" to cAssignments.size,
                                "assignments_pending" to cAssignments.count { !it.isCompleted }
                            )
                        }
                        sourcesConsulted.add(
                            CopilotSource(
                                title = "Base de datos académica local",
                                detail = "${courses.size} cursos y ${allAssignments.size} tareas registradas"
                            )
                        )
                        gson.toJson(courseSummaries)
                    }

                    "get_course_assignments" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        val assignments = assignmentDao.getAssignmentsForCourseOnce(cid)
                        val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                        sourcesConsulted.add(
                            CopilotSource(
                                title = "Tareas de $courseName",
                                detail = "${assignments.size} tareas registradas en base local"
                            )
                        )
                        val simplified = assignments.map {
                            mapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "due_at" to it.dueAt,
                                "points_possible" to it.pointsPossible,
                                "score" to it.score,
                                "completed" to it.isCompleted,
                                "status" to it.status
                            )
                        }
                        gson.toJson(simplified)
                    }

                    "fetch_canvas_assignment_details" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        val aid = args.get("assignment_id")?.asLong ?: 0L
                        val canvasToken = preferencesManager.accessToken.first()
                        if (canvasToken.isNullOrBlank()) {
                            gson.toJson(mapOf("error" to "Token de Canvas no disponible"))
                        } else {
                            val details = canvasApiService.getAssignmentDetails(
                                token = "Bearer $canvasToken",
                                courseId = cid,
                                assignmentId = aid
                            )
                            val cleanDesc = details.description?.let { cleanHtml(it) } ?: "Sin descripción"
                            sourcesConsulted.add(
                                CopilotSource(
                                    title = "Canvas LMS en vivo: ${details.name}",
                                    detail = "Rúbrica con ${details.rubric?.size ?: 0} criterios evaluativos"
                                )
                            )
                            val result = mapOf(
                                "name" to details.name,
                                "description" to cleanDesc.take(1200),
                                "points_possible" to details.pointsPossible,
                                "rubric" to details.rubric?.map { criterion ->
                                    mapOf(
                                        "description" to criterion.description,
                                        "long_description" to criterion.longDescription,
                                        "points" to criterion.points,
                                        "ratings" to criterion.ratings?.map { r ->
                                            "${r.description}: ${r.points} pts"
                                        }
                                    )
                                }
                            )
                            gson.toJson(result)
                        }
                    }

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
                        val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                        actionFeedback = "Grupo '$name' ($weight%) creado en el Simulador de $courseName."
                        sourcesConsulted.add(
                            CopilotSource(
                                title = "Simulador de Notas",
                                detail = "Creado grupo '$name' con ponderación $weight%"
                            )
                        )
                        gson.toJson(mapOf("success" to true, "group_id" to newGroupId, "message" to actionFeedback))
                    }

                    else -> gson.toJson(mapOf("error" to "Herramienta no reconocida"))
                }

                messages.add(
                    OpenRouterMessage(
                        role = "tool",
                        name = functionName,
                        toolCallId = toolCall.id,
                        content = toolResultString
                    )
                )
            }

            // Second LLM call with tools results
            val followUpRequest = OpenRouterChatRequest(
                model = model,
                messages = messages,
                temperature = 0.2
            )

            val followUpResponse = openRouterApiService.chatCompletion(
                authorization = authHeader,
                request = followUpRequest
            )

            if (!followUpResponse.isSuccessful) {
                val err = followUpResponse.errorBody()?.string() ?: "Error procesando resultados"
                return Result.failure(Exception(formatOpenRouterError(followUpResponse.code(), err)))
            }

            val finalReply = followUpResponse.body()?.choices?.firstOrNull()?.message?.content
                ?: "No se obtuvo respuesta final."

            return Result.success(
                CopilotResult(
                    reply = finalReply,
                    sources = sourcesConsulted,
                    actionFeedback = actionFeedback
                )
            )

        } catch (e: Exception) {
            Log.e("CopilotRepository", "Error executing copilot request", e)
            return Result.failure(e)
        }
    }

    private fun formatOpenRouterError(code: Int, rawBody: String): String {
        return try {
            val json = gson.fromJson(rawBody, JsonObject::class.java)
            val errorObj = json.getAsJsonObject("error")
            val message = errorObj?.get("message")?.asString
            if (code == 402) {
                "Saldo insuficiente o límite de tokens excedido en tu cuenta de OpenRouter. Puedes recargar saldo en openrouter.ai/settings/credits o cambiar a un modelo gratuito en tu Perfil."
            } else if (code == 401) {
                "API Key de OpenRouter inválida o expirada. Por favor, verifícala en tu Perfil."
            } else if (!message.isNullOrBlank()) {
                message
            } else {
                "Error OpenRouter ($code)"
            }
        } catch (e: Exception) {
            if (code == 402) "Saldo insuficiente en OpenRouter. Recarga saldo o usa un modelo gratuito."
            else "Error de conexión con el proveedor de IA ($code)"
        }
    }

    private fun cleanHtml(html: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html).toString().trim()
            }
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
        }
    }
}
