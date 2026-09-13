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
                description = "Consulta directamente la API de Canvas LMS o la base local para obtener la consigna completa, descripción y rúbrica detallada (criterios y puntuaciones) de una tarea. Puedes buscar por nombre de tarea (ej: 'Laboratorio 4') o por su ID numérico.",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID numérico de Canvas del curso. Si el usuario menciona el nombre del curso (ej: 'Tecnologías Emergentes'), usa el ID correspondiente de la lista de cursos."
                        ),
                        "assignment_name" to OpenRouterProperty(
                            type = "string",
                            description = "Nombre o parte del nombre de la tarea (ej: 'Laboratorio 4', 'Semana 3', 'Examen Parcial'). Úsalo cuando no tengas el ID exacto."
                        ),
                        "assignment_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID numérico de Canvas de la tarea (si ya se conoce)."
                        )
                    ),
                    required = listOf("course_id")
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
            append("Eres NotiVas Copilot, un asistente académico inteligente, autónomo y proactivo para estudiantes universitarios integrados con Canvas LMS. ")
            append("Respondes en español con formato Markdown limpio (viñetas, negritas, tablas si es necesario). ")
            append("Cuentas con herramientas para consultar información local y en vivo de Canvas LMS. ")
            append("LISTA DE CURSOS INSCRITOS: [$coursesSummary]. ")
            if (selectedCourseId != null) {
                append("El estudiante tiene seleccionado actualmente el curso ID: $selectedCourseId en la barra superior. ")
            }
            append("INSTRUCCIONES CLAVE DE AUTONOMÍA E INTELIGENCIA: ")
            append("1. NUNCA pidas al usuario confirmación o IDs técnicos (como course_id o assignment_id). Tú tienes la lista de cursos arriba con sus nombres e IDs. Si el usuario usa un nombre abreviado o parcial (ej: 'Innovación tecnológica' -> 'Investigación e Innovación Tecnológica', 'Web' -> 'Desarrollo de Aplicaciones Web', 'Móviles' -> 'Programación en Móviles'), asúmelo directamente e identifica su ID sin preguntarle. ")
            append("2. Si el usuario se refiere a una tarea como 'laboratorio 2', 'entregable 2', 'semana 2' o similar: ")
            append("   a) Ten en cuenta que en Canvas los nombres suelen codificarse con siglas de la semana o tipo (ej: 'PTAL-S02', 'Laboratorio S2', 'TEO-S2', 'S02'). El número '2' o 'S02' o 'S2' identifica la semana/laboratorio 2. ")
            append("   b) Llama a 'fetch_canvas_assignment_details' pasando el course_id y el assignment_name (ej: 'S02' o '2' o 'Laboratorio 2'). Si no estás 100% seguro del nombre exacto, primero llama a 'get_course_assignments' para ver los nombres reales de las tareas del curso y elige automáticamente la que corresponde a esa semana/entregable. ¡No le pidas al alumno que te diga el código de la tarea! ")
            append("3. Si obtienes los detalles de la consigna o rúbrica, explica clara y resumidamente: objetivo de la entrega, qué debe presentar el estudiante, criterios de la rúbrica y fecha límite si la tiene. ")
            append("4. Si el estudiante pregunta por qué obtuvo cierta calificación, por qué tuvo X nota (ej: '¿por qué tuve 15 en tal tarea?'): ")
            append("   a) Consulta 'fetch_canvas_assignment_details' para obtener la entrega del alumno ('student_submission'), los comentarios del docente ('teacher_comments') y la evaluación por rúbrica ('rubric_assessment'). ")
            append("   b) Cita textualmente la retroalimentación y comentarios que haya dejado el docente. ")
            append("   c) Compara los puntos obtenidos en cada criterio de la rúbrica ('student_points_obtained' vs 'points') e indica con exactitud en qué criterios perdió puntos o qué comentarios específicos dejó el profesor en cada criterio. ")
            append("5. Si te piden crear grupos de notas para simulaciones, usa create_simulation_group. ")
            append("Sé siempre proactivo, empático, directo y resuelve las consultas por tu cuenta usando tus herramientas sin repreguntar cosas que puedes deducir.")
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
                        var aid = args.get("assignment_id")?.asLong ?: 0L
                        val assignmentNameQuery = args.get("assignment_name")?.asString?.trim()

                        // Helper to match assignment by query including abbreviations and week numbers (e.g. "laboratorio 2", "entregable 2" -> S02, S2)
                        fun findMatchingAssignment(assignments: List<com.notivas.data.model.Assignment>, query: String): com.notivas.data.model.Assignment? {
                            // 1. Direct contains (case insensitive)
                            assignments.find { it.name.contains(query, ignoreCase = true) }?.let { return it }

                            // 2. Extract digits (e.g., "2" from "laboratorio/entregable 2" or "semana 2")
                            val digitMatch = Regex("(?i)(?:laboratorio|entregable|semana|s|lab|sesion|sesión|ptal|teo)[\\s/_-]*0*(\\d+)").find(query)
                                ?: Regex("\\b(\\d+)\\b").find(query)
                            val number = digitMatch?.groupValues?.get(1)

                            if (number != null) {
                                val padded = number.padStart(2, '0') // "02"
                                val patterns = listOf("S$number", "S$padded", "Semana $number", "Semana $padded", "Lab $number", "Laboratorio $number")
                                // Search with week/lab patterns
                                assignments.find { a ->
                                    patterns.any { p -> a.name.contains(p, ignoreCase = true) }
                                }?.let { return it }
                            }

                            // 3. Keyword matching
                            val keywords = query.lowercase()
                                .replace(Regex("[/_,\\-\\.:]"), " ")
                                .split(" ")
                                .filter { it.length > 1 && it !in listOf("en", "el", "la", "de", "del", "los", "las", "un", "una", "por", "que", "para") }
                            if (keywords.isNotEmpty()) {
                                assignments.find { a ->
                                    val aName = a.name.lowercase()
                                    keywords.all { kw -> aName.contains(kw) }
                                }?.let { return it }

                                // At least significant keyword match
                                assignments.maxByOrNull { a ->
                                    val aName = a.name.lowercase()
                                    keywords.count { kw -> aName.contains(kw) }
                                }?.takeIf { a ->
                                    val aName = a.name.lowercase()
                                    keywords.count { kw -> aName.contains(kw) } >= 2
                                }?.let { return it }
                            }

                            return null
                        }

                        // If aid not provided or 0, search for it locally or by name
                        if (aid == 0L && !assignmentNameQuery.isNullOrBlank()) {
                            val courseAssignments = assignmentDao.getAssignmentsForCourseOnce(cid)
                            val matched = findMatchingAssignment(courseAssignments, assignmentNameQuery)
                            if (matched != null) {
                                aid = matched.id
                            }
                        }

                        // If still not found in specified course, check all assignments across all courses in DB
                        var resolvedCid = cid
                        if (aid == 0L && !assignmentNameQuery.isNullOrBlank()) {
                            val allAssignments = assignmentDao.getAssignmentList()
                            val matched = findMatchingAssignment(allAssignments, assignmentNameQuery)
                            if (matched != null) {
                                aid = matched.id
                                resolvedCid = matched.courseId
                            }
                        }

                        val canvasToken = preferencesManager.accessToken.first()
                        if (aid != 0L && !canvasToken.isNullOrBlank()) {
                            try {
                                val details = canvasApiService.getAssignmentDetails(
                                    token = "Bearer $canvasToken",
                                    courseId = resolvedCid,
                                    assignmentId = aid
                                )
                                val cleanDesc = details.description?.let { cleanHtml(it) } ?: "Sin descripción"
                                val sub = details.submission
                                val submissionComments = sub?.submissionComments?.map { c ->
                                    mapOf(
                                        "author" to (c.authorName ?: "Docente"),
                                        "comment" to (c.comment ?: ""),
                                        "created_at" to (c.createdAt ?: "")
                                    )
                                } ?: emptyList()

                                val rubricAssessments = sub?.rubricAssessment?.map { (criterionId, assessment) ->
                                    mapOf(
                                        "criterion_id" to criterionId,
                                        "points_obtained" to assessment.points,
                                        "comments" to assessment.comments
                                    )
                                } ?: emptyList()

                                val hasCommentsOrScore = (sub?.score != null) || submissionComments.isNotEmpty()
                                sourcesConsulted.add(
                                    CopilotSource(
                                        title = "Canvas LMS en vivo: ${details.name}",
                                        detail = if (hasCommentsOrScore) {
                                            "Nota: ${sub?.score ?: "N/A"}/${details.pointsPossible ?: "N/A"} pts con ${submissionComments.size} comentarios del docente"
                                        } else {
                                            "Rúbrica con ${details.rubric?.size ?: 0} criterios evaluativos"
                                        }
                                    )
                                )
                                val result = mapOf(
                                    "id" to details.id,
                                    "name" to details.name,
                                    "due_at" to details.dueAt,
                                    "description" to cleanDesc.take(1500),
                                    "points_possible" to details.pointsPossible,
                                    "student_submission" to if (sub != null) {
                                        mapOf(
                                            "score" to sub.score,
                                            "grade" to sub.grade,
                                            "workflow_state" to sub.workflowState,
                                            "submitted_at" to sub.submittedAt,
                                            "graded_at" to sub.gradedAt,
                                            "late" to sub.late,
                                            "missing" to sub.missing,
                                            "teacher_comments" to submissionComments,
                                            "rubric_assessment" to rubricAssessments
                                        )
                                    } else null,
                                    "rubric" to details.rubric?.map { criterion ->
                                        val assessmentForThis = sub?.rubricAssessment?.get(criterion.id)
                                        mapOf(
                                            "id" to criterion.id,
                                            "description" to criterion.description,
                                            "long_description" to criterion.longDescription,
                                            "points" to criterion.points,
                                            "student_points_obtained" to assessmentForThis?.points,
                                            "teacher_criterion_comment" to assessmentForThis?.comments,
                                            "ratings" to criterion.ratings?.map { r ->
                                                "${r.description}: ${r.points} pts"
                                            }
                                        )
                                    }
                                )
                                gson.toJson(result)
                            } catch (e: Exception) {
                                // Fallback to local DB if Canvas API call fails
                                val local = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).find { it.id == aid }
                                    ?: assignmentDao.getAssignmentList().find { it.id == aid }
                                if (local != null) {
                                    val cleanDesc = local.description?.let { cleanHtml(it) } ?: "Sin descripción detallada"
                                    sourcesConsulted.add(
                                        CopilotSource(
                                            title = "Base local: ${local.name}",
                                            detail = "Puntaje: ${local.score ?: "N/A"}/${local.pointsPossible ?: 20} pts"
                                        )
                                    )
                                    gson.toJson(
                                        mapOf(
                                            "id" to local.id,
                                            "name" to local.name,
                                            "due_at" to local.dueAt,
                                            "description" to cleanDesc.take(1500),
                                            "points_possible" to local.pointsPossible,
                                            "score" to local.score,
                                            "grade" to local.grade,
                                            "status" to local.status
                                        )
                                    )
                                } else {
                                    gson.toJson(mapOf("error" to "No se pudo obtener detalles de la tarea: ${e.message}"))
                                }
                            }
                        } else if (aid != 0L) {
                            // Fallback to local DB without token
                            val local = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).find { it.id == aid }
                                ?: assignmentDao.getAssignmentList().find { it.id == aid }
                            if (local != null) {
                                val cleanDesc = local.description?.let { cleanHtml(it) } ?: "Sin descripción"
                                gson.toJson(
                                    mapOf(
                                        "id" to local.id,
                                        "name" to local.name,
                                        "due_at" to local.dueAt,
                                        "description" to cleanDesc.take(1500),
                                        "points_possible" to local.pointsPossible,
                                        "score" to local.score,
                                        "grade" to local.grade
                                    )
                                )
                            } else {
                                gson.toJson(mapOf("error" to "Tarea encontrada con ID $aid pero sin datos disponibles."))
                            }
                        } else {
                            // Assignment couldn't be found by name: return available assignments in that course so LLM can pick or suggest!
                            val available = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).map {
                                mapOf("id" to it.id, "name" to it.name, "due_at" to it.dueAt)
                            }
                            gson.toJson(
                                mapOf(
                                    "error" to "No se encontró ninguna tarea con el término '$assignmentNameQuery'.",
                                    "tareas_disponibles_en_curso" to available
                                )
                            )
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
