package com.notivas.data.repository.copilot.tools.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.remote.CanvasApiService
import com.notivas.data.remote.openrouter.OpenRouterFunction
import com.notivas.data.remote.openrouter.OpenRouterParameters
import com.notivas.data.remote.openrouter.OpenRouterProperty
import com.notivas.data.remote.openrouter.OpenRouterTool
import com.notivas.data.repository.CopilotSource
import com.notivas.data.repository.copilot.ToolExecutionResult
import com.notivas.data.repository.copilot.tools.CopilotToolHandler
import com.notivas.data.repository.copilot.tools.HtmlUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasDiscussionsToolsHandler @Inject constructor(
    private val canvasApiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val preferencesManager: PreferencesManager
) : CopilotToolHandler {

    private val gson = Gson()

    override val supportedTools: List<OpenRouterTool> = listOf(
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

    override fun canHandle(toolName: String): Boolean {
        return toolName in setOf("get_course_discussions", "fetch_discussion_details")
    }

    override suspend fun execute(
        toolName: String,
        args: JsonObject,
        selectedCourseId: Long?
    ): ToolExecutionResult {
        val courses = courseDao.getCourseList()

        return when (toolName) {
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
                            val cleanMsg = it.message?.let { m -> HtmlUtils.cleanHtml(m) } ?: ""
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
                            val cleanMsg = topic.message?.let { cleanHtml -> HtmlUtils.cleanHtml(cleanHtml) } ?: "Sin consigna o mensaje específico"
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

            else -> ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no soportada por CanvasDiscussionsToolsHandler")))
        }
    }
}
