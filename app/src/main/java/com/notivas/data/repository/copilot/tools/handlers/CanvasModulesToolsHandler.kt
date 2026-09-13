package com.notivas.data.repository.copilot.tools.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notivas.data.local.dao.CourseDao
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.CanvasModule
import com.notivas.data.model.CanvasModuleItem
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
class CanvasModulesToolsHandler @Inject constructor(
    private val canvasApiService: CanvasApiService,
    private val courseDao: CourseDao,
    private val preferencesManager: PreferencesManager
) : CopilotToolHandler {

    private val gson = Gson()

    override val supportedTools: List<OpenRouterTool> = listOf(
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
        )
    )

    override fun canHandle(toolName: String): Boolean {
        return toolName in setOf("get_course_modules", "fetch_module_item_content")
    }

    override suspend fun execute(
        toolName: String,
        args: JsonObject,
        selectedCourseId: Long?
    ): ToolExecutionResult {
        val courses = courseDao.getCourseList()

        return when (toolName) {
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
                            val cleanBody = pageDetail.body?.let { HtmlUtils.cleanHtml(it) } ?: "Sin contenido textual disponible"
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
                                        val cleanBody = pageDetail.body?.let { HtmlUtils.cleanHtml(it) } ?: "Sin contenido de texto"
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
                                        val cleanDesc = assignDetail?.description?.let { HtmlUtils.cleanHtml(it) }
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
                                        val cleanMessage = topicDetail?.message?.let { HtmlUtils.cleanHtml(it) } ?: "Sin mensaje"
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
                                    val cleanBody = pageDetail.body?.let { HtmlUtils.cleanHtml(it) } ?: "Sin contenido"
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

            else -> ToolExecutionResult(gson.toJson(mapOf("error" to "Herramienta no soportada por CanvasModulesToolsHandler")))
        }
    }
}
