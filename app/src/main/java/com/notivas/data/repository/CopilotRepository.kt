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
                name = "get_course_modules",
                description = "Obtiene los módulos del curso y todos los recursos, lecturas, enlaces y diapositivas subidos por el profesor en Canvas LMS.",
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
                name = "fetch_module_item_content",
                description = "Consulta y lee el contenido detallado o texto completo de un recurso de módulo en Canvas LMS (por ejemplo una página de lectura, sistema de evaluación, temario, guía, enlace o archivo). Puedes buscar por nombre del recurso o proporcionar su id/url.",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas del curso"
                        ),
                        "resource_name" to OpenRouterProperty(
                            type = "string",
                            description = "Título o nombre del recurso a leer (ej: 'Sistema de Evaluación', 'Silabo', 'Guía de laboratorio')"
                        ),
                        "page_url" to OpenRouterProperty(
                            type = "string",
                            description = "URL o slug de la página en Canvas si se conoce (opcional)"
                        )
                    ),
                    required = listOf("course_id", "resource_name")
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
        ),
        OpenRouterTool(
            function = OpenRouterFunction(
                name = "get_course_discussions",
                description = "Obtiene la lista de foros y debates (discussions) publicados en Canvas LMS para un curso por su course_id.",
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
                name = "fetch_discussion_details",
                description = "Obtiene las instrucciones completas, consigna, preguntas del profesor y detalles de un foro específico de Canvas LMS. Puedes buscar por topic_id o por nombre del foro.",
                parameters = OpenRouterParameters(
                    properties = mapOf(
                        "course_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID de Canvas del curso"
                        ),
                        "topic_id" to OpenRouterProperty(
                            type = "integer",
                            description = "ID del foro/debate en Canvas (opcional si proporcionas topic_title)"
                        ),
                        "topic_title" to OpenRouterProperty(
                            type = "string",
                            description = "Título o parte del nombre del foro (ej: 'Foro IA', 'Sustentación')"
                        )
                    ),
                    required = listOf("course_id")
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
            append("1. NUNCA pidas al usuario confirmación o IDs técnicos (como course_id o assignment_id). Tú tienes la lista de cursos arriba con sus nombres e IDs. Si el usuario usa un nombre abreviado o parcial (ej: 'Innovación tecnológica' -> 'Investigación e Innovación Tecnológica', 'Tecnologías emergentes' -> 'Tecnologías Emergentes', 'Web' -> 'Desarrollo de Aplicaciones Web'), asúmelo directamente e identifica su ID sin preguntarle. ")
            append("2. Si el usuario pregunta por 'el último laboratorio', 'la última tarea', 'la próxima entrega', 'qué tengo que hacer', 'laboratorio X' o similar de un curso: ")
            append("   a) Si pregunta por el 'último' o no sabes el nombre exacto, primero llama a 'get_course_assignments' con el course_id para ver todas las tareas, sus fechas de entrega y sus nombres reales. ")
            append("   b) Identifica cuál es la tarea/laboratorio más reciente o pendiente según su fecha o numeración (ej: S4 > S3 > S2). ")
            append("   c) Llama de inmediato a 'fetch_canvas_assignment_details' con el course_id y assignment_name o assignment_id de esa tarea para obtener la consigna completa en vivo de Canvas LMS y responder detalladamente. ¡NUNCA respondas con 'No se obtuvo respuesta final' ni digas que necesitas el ID! ")
            append("3. Si el mensaje del estudiante incluye etiquetas de mención como @[Curso > Tarea] o @[Curso > Módulo: Recurso] o @[   > Recurso]: ")
            append("   a) Extrae el nombre del recurso y del curso de la etiqueta. Si el curso no está especificado en la etiqueta, busca el curso correspondiente en tu lista de cursos inscritos. ")
            append("   b) Si es una tarea o laboratorio, llama a 'fetch_canvas_assignment_details'. Si es un recurso, página o foro de módulo, llama a 'fetch_module_item_content' o 'fetch_discussion_details'. ")
            append("4. Si obtienes los detalles de la consigna o rúbrica, explica clara y resumidamente: objetivo de la entrega, qué debe presentar el estudiante, procedimientos, formato (ej. PDF, individual/grupal), medio de entrega y fecha límite con hora si la tiene. ")
            append("5. Si el estudiante pregunta por qué obtuvo cierta calificación, por qué tuvo X nota (ej: '¿por qué tuve 15 en tal tarea?'): ")
            append("   a) Consulta 'fetch_canvas_assignment_details' para obtener la entrega del alumno ('student_submission'), los comentarios del docente ('teacher_comments') y la evaluación por rúbrica ('rubric_assessment'). ")
            append("   b) Cita textualmente la retroalimentación y comentarios que haya dejado el docente. ")
            append("   c) Compara los puntos obtenidos en cada criterio de la rúbrica ('student_points_obtained' vs 'points') e indica con exactitud en qué criterios perdió puntos o qué comentarios específicos dejó el profesor en cada criterio. ")
            append("6. Si te preguntan por módulos, lecturas, enlaces, diapositivas o recursos subidos por el profesor: ")
            append("   a) Si necesitas ver la lista de módulos y qué recursos hay, llama a 'get_course_modules'. ")
            append("   b) Si el usuario menciona un recurso específico o pide que le expliques o detalles su contenido (por ejemplo 'Sistema de Evaluación', 'Guía', 'Lectura S1', 'Temario'): DEBES llamar a 'fetch_module_item_content' pasando el course_id y el resource_name. ¡NUNCA le digas que no puedes leer la página o que solo ves el título! ")
            append("7. Si el usuario pregunta por un FORO, debate o 'último foro' (ej: 'último foro de Móviles', 'de qué trata el foro y cómo lo respondo'): ")
            append("   a) Si no conoces el foro o pide el 'último foro', primero llama a 'get_course_discussions' o 'get_course_modules' con el course_id para encontrar el foro más reciente o con la semana más alta. ")
            append("   b) Inmediatamente llama a 'fetch_discussion_details' (o 'fetch_module_item_content') para leer el MENSAJE/CONSIGNA COMPLETA del docente en dicho foro. ")
            append("   c) Responde explicando con claridad: DE QUÉ TRATA exactamente el foro según las indicaciones del profesor, y CÓMO DEBE RESPONDERLO (estructura sugerida, puntos clave a responder, formato o argumentos a incluir). ¡NUNCA te limites a dar solo un link o decir 'entra para ver las indicaciones'! ")
            append("8. Si te piden crear grupos de notas para simulaciones, usa create_simulation_group. ")
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
            var promptTokensAccumulated = 0
            var completionTokensAccumulated = 0
            var totalTokensAccumulated = 0
            var finalReply: String? = null

            val maxTurns = 3
            var turn = 0

            while (turn < maxTurns) {
                turn++

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

                    "get_course_modules" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        val rawToken = preferencesManager.accessToken.first()
                        if (!rawToken.isNullOrBlank() && cid != 0L) {
                            try {
                                val modules = canvasApiService.getModulesWithItems("Bearer $rawToken", cid)
                                val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                                sourcesConsulted.add(
                                    CopilotSource(
                                        title = "Módulos de $courseName",
                                        detail = "${modules.size} módulos obtenidos de Canvas"
                                    )
                                )
                                val modulesData = modules.map { mod ->
                                    mapOf(
                                        "module_id" to mod.id,
                                        "name" to mod.name,
                                        "items" to (mod.items ?: emptyList()).map { item ->
                                            mapOf(
                                                "id" to item.id,
                                                "title" to item.title,
                                                "type" to item.type,
                                                "html_url" to item.htmlUrl,
                                                "url" to item.url
                                            )
                                        }
                                    )
                                }
                                gson.toJson(modulesData)
                            } catch (e: Exception) {
                                gson.toJson(mapOf("error" to "No se pudieron obtener los módulos de Canvas: ${e.message}"))
                            }
                        } else {
                            gson.toJson(mapOf("error" to "No hay token o course_id no válido."))
                        }
                    }

                    "fetch_module_item_content" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        val resourceNameQuery = args.get("resource_name")?.asString?.trim() ?: ""
                        val explicitPageUrl = args.get("page_url")?.asString?.trim()
                        val rawToken = preferencesManager.accessToken.first()

                        if (!rawToken.isNullOrBlank() && cid != 0L) {
                            try {
                                val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                                val token = "Bearer $rawToken"

                                // If explicit page_url provided, fetch page directly
                                if (!explicitPageUrl.isNullOrBlank()) {
                                    val pageDetail = canvasApiService.getPageDetails(token, cid, explicitPageUrl)
                                    val cleanBody = pageDetail.body?.let { cleanHtml(it) } ?: "Sin contenido textual disponible"
                                    sourcesConsulted.add(
                                        CopilotSource(
                                            title = "${pageDetail.title ?: resourceNameQuery} ($courseName)",
                                            detail = "Página de Canvas LMS leída"
                                        )
                                    )
                                    gson.toJson(
                                        mapOf(
                                            "title" to pageDetail.title,
                                            "url" to pageDetail.url,
                                            "content" to cleanBody.take(4000)
                                        )
                                    )
                                } else {
                                    // 1. Fetch modules to find matching item by resourceNameQuery
                                    val modules = canvasApiService.getModulesWithItems(token, cid)
                                    var foundItem: com.notivas.data.model.CanvasModuleItem? = null
                                    var foundModule: com.notivas.data.model.CanvasModule? = null

                                    for (mod in modules) {
                                        val items = mod.items ?: continue
                                        val match = items.find { it.title.contains(resourceNameQuery, ignoreCase = true) }
                                            ?: items.find { resourceNameQuery.contains(it.title, ignoreCase = true) }
                                            ?: items.find { item ->
                                                val qWords = resourceNameQuery.lowercase().split(" ").filter { it.length > 2 }
                                                qWords.isNotEmpty() && qWords.all { item.title.lowercase().contains(it) }
                                            }
                                        if (match != null) {
                                            foundItem = match
                                            foundModule = mod
                                            break
                                        }
                                    }

                                    if (foundItem != null) {
                                        when (foundItem.type.lowercase()) {
                                            "page" -> {
                                                val pageSlug = foundItem.pageUrl
                                                    ?: foundItem.url?.substringAfterLast("/pages/")
                                                    ?: foundItem.title.lowercase().replace(" ", "-")
                                                val pageDetail = canvasApiService.getPageDetails(token, cid, pageSlug)
                                                val cleanBody = pageDetail.body?.let { cleanHtml(it) } ?: "Sin contenido de texto"
                                                sourcesConsulted.add(
                                                    CopilotSource(
                                                        title = "${pageDetail.title ?: foundItem.title} ($courseName)",
                                                        detail = "Página de módulo '${foundModule?.name}'"
                                                    )
                                                )
                                                gson.toJson(
                                                    mapOf(
                                                        "module" to foundModule?.name,
                                                        "title" to (pageDetail.title ?: foundItem.title),
                                                        "type" to "Page",
                                                        "content" to cleanBody.take(4000)
                                                    )
                                                )
                                            }
                                            "file" -> {
                                                val fileId = foundItem.contentId ?: foundItem.id
                                                val fileDetail = try {
                                                    canvasApiService.getFileDetails(token, cid, fileId)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                sourcesConsulted.add(
                                                    CopilotSource(
                                                        title = "${foundItem.title} ($courseName)",
                                                        detail = "Archivo subido por el profesor en módulo '${foundModule?.name}'"
                                                    )
                                                )
                                                gson.toJson(
                                                    mapOf(
                                                        "module" to foundModule?.name,
                                                        "title" to foundItem.title,
                                                        "type" to "File",
                                                        "download_url" to (fileDetail?.url ?: foundItem.url ?: foundItem.htmlUrl),
                                                        "filename" to (fileDetail?.displayName ?: foundItem.title),
                                                        "size" to fileDetail?.size
                                                    )
                                                )
                                            }
                                            "assignment" -> {
                                                val aid = foundItem.contentId ?: foundItem.id
                                                val assignDetail = try {
                                                    canvasApiService.getAssignmentDetails(token, cid, aid)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                val cleanDesc = assignDetail?.description?.let { cleanHtml(it) }
                                                sourcesConsulted.add(
                                                    CopilotSource(
                                                        title = "${foundItem.title} ($courseName)",
                                                        detail = "Tarea en módulo '${foundModule?.name}'"
                                                    )
                                                )
                                                gson.toJson(
                                                    mapOf(
                                                        "module" to foundModule?.name,
                                                        "title" to foundItem.title,
                                                        "type" to "Assignment",
                                                        "description" to cleanDesc?.take(2000),
                                                        "due_at" to assignDetail?.dueAt,
                                                        "points_possible" to assignDetail?.pointsPossible
                                                    )
                                                )
                                            }
                                            "discussion" -> {
                                                val topicId = foundItem.contentId ?: foundItem.id
                                                val topicDetail = try {
                                                    canvasApiService.getDiscussionTopic(token, cid, topicId)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                val cleanMessage = topicDetail?.message?.let { cleanHtml(it) } ?: "Sin mensaje"
                                                sourcesConsulted.add(
                                                    CopilotSource(
                                                        title = "Foro: ${foundItem.title} ($courseName)",
                                                        detail = "Foro/Debate de Canvas en '${foundModule?.name}'"
                                                    )
                                                )
                                                gson.toJson(
                                                    mapOf(
                                                        "module" to foundModule?.name,
                                                        "title" to (topicDetail?.title ?: foundItem.title),
                                                        "type" to "Discussion",
                                                        "author" to topicDetail?.userName,
                                                        "posted_at" to topicDetail?.postedAt,
                                                        "message" to cleanMessage.take(4000),
                                                        "url" to (topicDetail?.htmlUrl ?: foundItem.htmlUrl ?: foundItem.url)
                                                    )
                                                )
                                            }
                                            else -> {
                                                sourcesConsulted.add(
                                                    CopilotSource(
                                                        title = "${foundItem.title} ($courseName)",
                                                        detail = "Recurso (${foundItem.type}) en módulo '${foundModule?.name}'"
                                                    )
                                                )
                                                gson.toJson(
                                                    mapOf(
                                                        "module" to foundModule?.name,
                                                        "title" to foundItem.title,
                                                        "type" to foundItem.type,
                                                        "html_url" to foundItem.htmlUrl,
                                                        "url" to foundItem.url
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        // Not found in modules, try looking for a page directly by title
                                        val slug = resourceNameQuery.lowercase()
                                            .replace(Regex("[^a-z0-9\\s-]"), "")
                                            .trim()
                                            .replace(Regex("\\s+"), "-")
                                        try {
                                            val pageDetail = canvasApiService.getPageDetails(token, cid, slug)
                                            val cleanBody = pageDetail.body?.let { cleanHtml(it) } ?: "Sin contenido"
                                            sourcesConsulted.add(
                                                CopilotSource(
                                                    title = "${pageDetail.title ?: resourceNameQuery} ($courseName)",
                                                    detail = "Página de Canvas LMS leída"
                                                )
                                            )
                                            gson.toJson(
                                                mapOf(
                                                    "title" to pageDetail.title,
                                                    "type" to "Page",
                                                    "content" to cleanBody.take(4000)
                                                )
                                            )
                                        } catch (e: Exception) {
                                            gson.toJson(
                                                mapOf(
                                                    "error" to "No se encontró el recurso o página con nombre '$resourceNameQuery' en el curso.",
                                                    "recursos_disponibles" to modules.map { m ->
                                                        mapOf("modulo" to m.name, "items" to m.items?.map { it.title })
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                gson.toJson(mapOf("error" to "Error al consultar el contenido en Canvas: ${e.message}"))
                            }
                        } else {
                            gson.toJson(mapOf("error" to "No hay token de Canvas configurado o curso no especificado."))
                        }
                    }

                    "get_course_discussions" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        val canvasToken = preferencesManager.accessToken.first()
                        val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"

                        if (!canvasToken.isNullOrBlank() && cid != 0L) {
                            try {
                                val token = "Bearer $canvasToken"
                                val discussions = canvasApiService.getDiscussionTopics(token, cid)
                                sourcesConsulted.add(
                                    CopilotSource(
                                        title = "Foros y debates de $courseName",
                                        detail = "${discussions.size} foros registrados en Canvas"
                                    )
                                )
                                val simplified = discussions.map {
                                    val cleanMsg = it.message?.let { m -> cleanHtml(m) } ?: ""
                                    mapOf(
                                        "id" to it.id,
                                        "title" to it.title,
                                        "author" to it.userName,
                                        "posted_at" to it.postedAt,
                                        "subentry_count" to it.discussionSubentryCount,
                                        "message_preview" to cleanMsg.take(200)
                                    )
                                }
                                gson.toJson(simplified)
                            } catch (e: Exception) {
                                gson.toJson(mapOf("error" to "Error al obtener foros: ${e.message}"))
                            }
                        } else {
                            gson.toJson(mapOf("error" to "No hay token de Canvas o curso no seleccionado."))
                        }
                    }

                    "fetch_discussion_details" -> {
                        val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                        var topicId = args.get("topic_id")?.asLong ?: 0L
                        val topicTitleQuery = args.get("topic_title")?.asString?.trim() ?: ""
                        val canvasToken = preferencesManager.accessToken.first()
                        val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"

                        if (!canvasToken.isNullOrBlank() && cid != 0L) {
                            try {
                                val token = "Bearer $canvasToken"
                                if (topicId == 0L && topicTitleQuery.isNotBlank()) {
                                    val allTopics = canvasApiService.getDiscussionTopics(token, cid)
                                    val match = allTopics.find { it.title.contains(topicTitleQuery, ignoreCase = true) }
                                        ?: allTopics.find { topicTitleQuery.contains(it.title, ignoreCase = true) }
                                        ?: allTopics.find { t ->
                                            val words = topicTitleQuery.lowercase().split(" ").filter { it.length > 2 }
                                            words.isNotEmpty() && words.all { t.title.lowercase().contains(it) }
                                        }
                                    if (match != null) {
                                        topicId = match.id
                                    }
                                }

                                if (topicId != 0L) {
                                    val topic = canvasApiService.getDiscussionTopic(token, cid, topicId)
                                    val cleanMsg = topic.message?.let { cleanHtml(it) } ?: "Sin consigna o mensaje específico"
                                    sourcesConsulted.add(
                                        CopilotSource(
                                            title = "Foro: ${topic.title} ($courseName)",
                                            detail = "Instrucciones y consigna en vivo de Canvas LMS"
                                        )
                                    )
                                    gson.toJson(
                                        mapOf(
                                            "id" to topic.id,
                                            "title" to topic.title,
                                            "author" to topic.userName,
                                            "posted_at" to topic.postedAt,
                                            "consigna_message" to cleanMsg.take(4000),
                                            "url" to topic.htmlUrl
                                        )
                                    )
                                } else {
                                    gson.toJson(mapOf("error" to "No se encontró el foro '$topicTitleQuery' en $courseName"))
                                }
                            } catch (e: Exception) {
                                gson.toJson(mapOf("error" to "Error al consultar foro: ${e.message}"))
                            }
                        } else {
                            gson.toJson(mapOf("error" to "No hay token de Canvas o curso no seleccionado."))
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

            // Accumulate to global preferences
            preferencesManager.addCopilotTokens(totalTokensAccumulated.toLong())

            return Result.success(
                CopilotResult(
                    reply = finalReply,
                    sources = sourcesConsulted,
                    actionFeedback = actionFeedback,
                    promptTokens = promptTokensAccumulated,
                    completionTokens = completionTokensAccumulated,
                    totalTokens = totalTokensAccumulated
                )
            )

        } catch (e: Exception) {
            Log.e("CopilotRepository", "Error executing copilot request", e)
            return Result.failure(e)
        }
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
