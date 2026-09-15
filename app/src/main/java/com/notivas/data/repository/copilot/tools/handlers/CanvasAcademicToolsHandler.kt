package com.notivas.data.repository.Ananau.tools.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.Assignment
import com.notivas.data.remote.CanvasApiService
import com.notivas.data.remote.openrouter.OpenRouterFunction
import com.notivas.data.remote.openrouter.OpenRouterParameters
import com.notivas.data.remote.openrouter.OpenRouterProperty
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.AnanauSource
import com.notivas.data.repository.Ananau.ToolExecutionResult
import com.notivas.data.repository.Ananau.tools.AnanauToolHandler
import com.notivas.data.repository.Ananau.tools.HtmlUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasAcademicToolsHandler @Inject constructor(
    private val canvasApiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val assignmentDao: AssignmentDao,
    private val preferencesManager: PreferencesManager
) : AnanauToolHandler {

    private val gson = Gson()

    override val supportedTools: List<OpenRouterTool> = listOf(
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
        )
    )

    override fun canHandle(toolName: String): Boolean {
        return toolName in setOf(
            "get_academic_overview",
            "get_course_assignments",
            "fetch_canvas_assignment_details"
        )
    }

    override suspend fun execute(
        toolName: String,
        args: JsonObject,
        selectedCourseId: Long?
    ): ToolExecutionResult {
        val courses = courseDao.getCourseList()

        return when (toolName) {
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
                val source = AnanauSource(
                    title = "Base de datos académica local",
                    detail = "${courses.size} cursos y ${allAssignments.size} tareas registradas"
                )
                ToolExecutionResult(gson.toJson(courseSummaries), source)
            }

            "get_course_assignments" -> {
                val cid = args.get("course_id")?.asLong ?: selectedCourseId ?: 0L
                val assignments = assignmentDao.getAssignmentsForCourseOnce(cid)
                val courseName = courses.find { it.id == cid }?.name ?: "Curso $cid"
                val source = AnanauSource(
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
                        val cleanDesc = details.description?.let { HtmlUtils.cleanHtml(it) } ?: "Sin descripción"
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
                        val source = AnanauSource(
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
                            val cleanDesc = local.description?.let { HtmlUtils.cleanHtml(it) } ?: "Sin descripción detallada"
                            val source = AnanauSource(
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
                        val cleanDesc = local.description?.let { HtmlUtils.cleanHtml(it) } ?: "Sin descripción"
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

            else -> ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no soportada por CanvasAcademicToolsHandler")))
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
}
