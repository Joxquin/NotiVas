package com.notivas.data.repository.copilot

import android.os.Build
import android.text.Html
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.dao.SimulationDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.model.CanvasModule
import com.notivas.data.model.CanvasModuleItem
import com.notivas.data.model.SimulationGroup
import com.notivas.data.remote.CanvasApiService
import com.notivas.data.remote.openrouter.*
import com.notivas.data.repository.CopilotSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class ToolExecutionResult(
    val resultJson: String,
    val source: CopilotSource? = null,
    val actionFeedback: String? = null
)

@Singleton
class CopilotToolExecutor @Inject constructor(
    private val canvasApiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val assignmentDao: AssignmentDao,
    private val simulationDao: SimulationDao,
    private val preferencesManager: PreferencesManager
) {
    private val gson = Gson()

    val tools: List<OpenRouterTool> = listOf(
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

        val courses = courseDao.getCourseList()

        return when (functionName) {
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
                val source = CopilotSource(
                    title = "Base de datos académica local",
                    detail = "${courses.size} cursos y ${allAssignments.size} tareas registradas"
                )
                ToolExecutionResult(gson.toJson(courseSummaries), source)
            }

            "get_course_assignments" -> {
                val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                val assignments = assignmentDao.getAssignmentsForCourseOnce(cid)
                val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                val source = CopilotSource(
                    title = "Tareas de $courseName",
                    detail = "${assignments.size} tareas registradas en base local"
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
                ToolExecutionResult(gson.toJson(simplified), source)
            }

            "fetch_canvas_assignment_details" -> {
                val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                var aid = args.get("assignment_id")?.asLong ?: 0L
                val assignmentNameQuery = args.get("assignment_name")?.asString?.trim()

                if (aid == 0L && !assignmentNameQuery.isNullOrBlank()) {
                    val courseAssignments = assignmentDao.getAssignmentsForCourseOnce(cid)
                    val matched = findMatchingAssignment(courseAssignments, assignmentNameQuery)
                    if (matched != null) {
                        aid = matched.id
                    }
                }

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
                        val source = CopilotSource(
                            title = "Canvas LMS en vivo: ${details.name}",
                            detail = if (hasCommentsOrScore) {
                                "Nota: ${sub?.score ?: "N/A"}/${details.pointsPossible ?: "N/A"} pts con ${submissionComments.size} comentarios del docente"
                            } else {
                                "Rúbrica con ${details.rubric?.size ?: 0} criterios evaluativos"
                            }
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
                        ToolExecutionResult(gson.toJson(result), source)
                    } catch (e: Exception) {
                        val local = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).find { it.id == aid }
                            ?: assignmentDao.getAssignmentList().find { it.id == aid }
                        if (local != null) {
                            val cleanDesc = local.description?.let { cleanHtml(it) } ?: "Sin descripción detallada"
                            val source = CopilotSource(
                                title = "Base local: ${local.name}",
                                detail = "Puntaje: ${local.score ?: "N/A"}/${local.pointsPossible ?: 20} pts"
                            )
                            val json = gson.toJson(
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
                            ToolExecutionResult(json, source)
                        } else {
                            ToolExecutionResult(gson.toJson(mapOf("error" to "No se pudo obtener detalles de la tarea: ${e.message}")))
                        }
                    }
                } else if (aid != 0L) {
                    val local = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).find { it.id == aid }
                        ?: assignmentDao.getAssignmentList().find { it.id == aid }
                    if (local != null) {
                        val cleanDesc = local.description?.let { cleanHtml(it) } ?: "Sin descripción"
                        val json = gson.toJson(
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
                        ToolExecutionResult(json)
                    } else {
                        ToolExecutionResult(gson.toJson(mapOf("error" to "Tarea encontrada con ID $aid pero sin datos disponibles.")))
                    }
                } else {
                    val available = assignmentDao.getAssignmentsForCourseOnce(resolvedCid).map {
                        mapOf("id" to it.id, "name" to it.name, "due_at" to it.dueAt)
                    }
                    val json = gson.toJson(
                        mapOf(
                            "error" to "No se encontró ninguna tarea con el término '$assignmentNameQuery'.",
                            "tareas_disponibles_en_curso" to available
                        )
                    )
                    ToolExecutionResult(json)
                }
            }

            "get_course_modules" -> {
                val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                val rawToken = preferencesManager.accessToken.first()
                if (!rawToken.isNullOrBlank() && cid != 0L) {
                    try {
                        val modules = canvasApiService.getModulesWithItems("Bearer $rawToken", cid)
                        val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                        val source = CopilotSource(
                            title = "Módulos de $courseName",
                            detail = "${modules.size} módulos obtenidos de Canvas"
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
                        ToolExecutionResult(gson.toJson(modulesData), source)
                    } catch (e: Exception) {
                        ToolExecutionResult(gson.toJson(mapOf("error" to "No se pudieron obtener los módulos de Canvas: ${e.message}")))
                    }
                } else {
                    ToolExecutionResult(gson.toJson(mapOf("error" to "No hay token o course_id no válido.")))
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

                        if (!explicitPageUrl.isNullOrBlank()) {
                            val pageDetail = canvasApiService.getPageDetails(token, cid, explicitPageUrl)
                            val cleanBody = pageDetail.body?.let { cleanHtml(it) } ?: "Sin contenido textual disponible"
                            val source = CopilotSource(
                                title = "${pageDetail.title ?: resourceNameQuery} ($courseName)",
                                detail = "Página de Canvas LMS leída"
                            )
                            val json = gson.toJson(
                                mapOf(
                                    "title" to pageDetail.title,
                                    "url" to pageDetail.url,
                                    "content" to cleanBody.take(4000)
                                )
                            )
                            ToolExecutionResult(json, source)
                        } else {
                            val modules = canvasApiService.getModulesWithItems(token, cid)
                            var foundItem: CanvasModuleItem? = null
                            var foundModule: CanvasModule? = null

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
                                        val source = CopilotSource(
                                            title = "${pageDetail.title ?: foundItem.title} ($courseName)",
                                            detail = "Página de módulo '${foundModule?.name}'"
                                        )
                                        val json = gson.toJson(
                                            mapOf(
                                                "module" to foundModule?.name,
                                                "title" to (pageDetail.title ?: foundItem.title),
                                                "type" to "Page",
                                                "content" to cleanBody.take(4000)
                                            )
                                        )
                                        ToolExecutionResult(json, source)
                                    }
                                    "file" -> {
                                        val fileId = foundItem.contentId ?: foundItem.id
                                        val fileDetail = try {
                                            canvasApiService.getFileDetails(token, cid, fileId)
                                        } catch (e: Exception) {
                                            null
                                        }
                                        val source = CopilotSource(
                                            title = "${foundItem.title} ($courseName)",
                                            detail = "Archivo subido por el profesor en módulo '${foundModule?.name}'"
                                        )
                                        val json = gson.toJson(
                                            mapOf(
                                                "module" to foundModule?.name,
                                                "title" to foundItem.title,
                                                "type" to "File",
                                                "download_url" to (fileDetail?.url ?: foundItem.url ?: foundItem.htmlUrl),
                                                "filename" to (fileDetail?.displayName ?: foundItem.title),
                                                "size" to fileDetail?.size
                                            )
                                        )
                                        ToolExecutionResult(json, source)
                                    }
                                    "assignment" -> {
                                        val aid = foundItem.contentId ?: foundItem.id
                                        val assignDetail = try {
                                            canvasApiService.getAssignmentDetails(token, cid, aid)
                                        } catch (e: Exception) {
                                            null
                                        }
                                        val cleanDesc = assignDetail?.description?.let { cleanHtml(it) }
                                        val source = CopilotSource(
                                            title = "${foundItem.title} ($courseName)",
                                            detail = "Tarea en módulo '${foundModule?.name}'"
                                        )
                                        val json = gson.toJson(
                                            mapOf(
                                                "module" to foundModule?.name,
                                                "title" to foundItem.title,
                                                "type" to "Assignment",
                                                "description" to cleanDesc?.take(2000),
                                                "due_at" to assignDetail?.dueAt,
                                                "points_possible" to assignDetail?.pointsPossible
                                            )
                                        )
                                        ToolExecutionResult(json, source)
                                    }
                                    "discussion" -> {
                                        val topicId = foundItem.contentId ?: foundItem.id
                                        val topicDetail = try {
                                            canvasApiService.getDiscussionTopic(token, cid, topicId)
                                        } catch (e: Exception) {
                                            null
                                        }
                                        val cleanMessage = topicDetail?.message?.let { cleanHtml(it) } ?: "Sin mensaje"
                                        val source = CopilotSource(
                                            title = "Foro: ${foundItem.title} ($courseName)",
                                            detail = "Foro/Debate de Canvas en '${foundModule?.name}'"
                                        )
                                        val json = gson.toJson(
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
                                        ToolExecutionResult(json, source)
                                    }
                                    else -> {
                                        val source = CopilotSource(
                                            title = "${foundItem.title} ($courseName)",
                                            detail = "Recurso (${foundItem.type}) en módulo '${foundModule?.name}'"
                                        )
                                        val json = gson.toJson(
                                            mapOf(
                                                "module" to foundModule?.name,
                                                "title" to foundItem.title,
                                                "type" to foundItem.type,
                                                "html_url" to foundItem.htmlUrl,
                                                "url" to foundItem.url
                                            )
                                        )
                                        ToolExecutionResult(json, source)
                                    }
                                }
                            } else {
                                val slug = resourceNameQuery.lowercase()
                                    .replace(Regex("[^a-z0-9\\s-]"), "")
                                    .trim()
                                    .replace(Regex("\\s+"), "-")
                                try {
                                    val pageDetail = canvasApiService.getPageDetails(token, cid, slug)
                                    val cleanBody = pageDetail.body?.let { cleanHtml(it) } ?: "Sin contenido"
                                    val source = CopilotSource(
                                        title = "${pageDetail.title ?: resourceNameQuery} ($courseName)",
                                        detail = "Página de Canvas LMS leída"
                                    )
                                    val json = gson.toJson(
                                        mapOf(
                                            "title" to pageDetail.title,
                                            "type" to "Page",
                                            "content" to cleanBody.take(4000)
                                        )
                                    )
                                    ToolExecutionResult(json, source)
                                } catch (e: Exception) {
                                    val json = gson.toJson(
                                        mapOf(
                                            "error" to "No se encontró el recurso o página con nombre '$resourceNameQuery' en el curso.",
                                            "recursos_disponibles" to modules.map { m ->
                                                mapOf("modulo" to m.name, "items" to m.items?.map { it.title })
                                            }
                                        )
                                    )
                                    ToolExecutionResult(json)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        ToolExecutionResult(gson.toJson(mapOf("error" to "Error al consultar el contenido en Canvas: ${e.message}")))
                    }
                } else {
                    ToolExecutionResult(gson.toJson(mapOf("error" to "No hay token de Canvas configurado o curso no especificado.")))
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
                        val source = CopilotSource(
                            title = "Foros y debates de $courseName",
                            detail = "${discussions.size} foros registrados en Canvas"
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
                        ToolExecutionResult(gson.toJson(simplified), source)
                    } catch (e: Exception) {
                        ToolExecutionResult(gson.toJson(mapOf("error" to "Error al obtener foros: ${e.message}")))
                    }
                } else {
                    ToolExecutionResult(gson.toJson(mapOf("error" to "No hay token de Canvas o curso no seleccionado.")))
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
                            val source = CopilotSource(
                                title = "Foro: ${topic.title} ($courseName)",
                                detail = "Instrucciones y consigna en vivo de Canvas LMS"
                            )
                            val json = gson.toJson(
                                mapOf(
                                    "id" to topic.id,
                                    "title" to topic.title,
                                    "author" to topic.userName,
                                    "posted_at" to topic.postedAt,
                                    "consigna_message" to cleanMsg.take(4000),
                                    "url" to topic.htmlUrl
                                )
                            )
                            ToolExecutionResult(json, source)
                        } else {
                            ToolExecutionResult(gson.toJson(mapOf("error" to "No se encontró el foro '$topicTitleQuery' en $courseName")))
                        }
                    } catch (e: Exception) {
                        ToolExecutionResult(gson.toJson(mapOf("error" to "Error al consultar foro: ${e.message}")))
                    }
                } else {
                    ToolExecutionResult(gson.toJson(mapOf("error" to "No hay token de Canvas o curso no seleccionado.")))
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
                val actionFeedback = "Grupo '$name' ($weight%) creado en el Simulador de $courseName."
                val source = CopilotSource(
                    title = "Simulador de Notas",
                    detail = "Creado grupo '$name' con ponderación $weight%"
                )
                val json = gson.toJson(mapOf("success" to true, "group_id" to newGroupId, "message" to actionFeedback))
                ToolExecutionResult(json, source, actionFeedback)
            }

            else -> ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no reconocida")))
        }
    }

    private fun findMatchingAssignment(assignments: List<Assignment>, query: String): Assignment? {
        assignments.find { it.name.contains(query, ignoreCase = true) }?.let { return it }

        val digitMatch = Regex("(?i)(?:laboratorio|entregable|semana|s|lab|sesion|sesión|ptal|teo)[\\s/_-]*0*(\\d+)").find(query)
            ?: Regex("\\b(\\d+)\\b").find(query)
        val number = digitMatch?.groupValues?.get(1)

        if (number != null) {
            val padded = number.padStart(2, '0')
            val patterns = listOf("S$number", "S$padded", "Semana $number", "Semana $padded", "Lab $number", "Laboratorio $number")
            assignments.find { a ->
                patterns.any { p -> a.name.contains(p, ignoreCase = true) }
            }?.let { return it }
        }

        val keywords = query.lowercase()
            .replace(Regex("[/_,\\-\\.:]"), " ")
            .split(" ")
            .filter { it.length > 1 && it !in listOf("en", "el", "la", "de", "del", "los", "las", "un", "una", "por", "que", "para") }
        if (keywords.isNotEmpty()) {
            assignments.find { a ->
                val aName = a.name.lowercase()
                keywords.all { kw -> aName.contains(kw) }
            }?.let { return it }

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
