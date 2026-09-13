package com.notivas.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notivas.data.local.dao.CopilotChatDao
import com.notivas.data.model.CopilotMessageEntity
import com.notivas.data.model.CopilotSession
import com.notivas.ui.copilot.CopilotMessageItem
import com.notivas.ui.copilot.CopilotRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CopilotChatRepository @Inject constructor(
    private val copilotChatDao: CopilotChatDao
) {
    private val gson = Gson()

    val allSessions: Flow<List<CopilotSession>> = copilotChatDao.getAllSessions()

    suspend fun getSessionById(sessionId: String): CopilotSession? {
        return copilotChatDao.getSessionById(sessionId)
    }

    suspend fun getMessagesForSession(sessionId: String): List<CopilotMessageItem> {
        val entities = copilotChatDao.getMessagesForSessionOnce(sessionId)
        return entities.map { entity ->
            val sources = if (!entity.sourcesJson.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<CopilotSource>>() {}.type
                    gson.fromJson<List<CopilotSource>>(entity.sourcesJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            CopilotMessageItem(
                id = entity.id,
                role = if (entity.role == "USER") CopilotRole.USER else CopilotRole.ASSISTANT,
                text = entity.text,
                sources = sources,
                actionFeedback = entity.actionFeedback,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun createOrUpdateSession(
        sessionId: String,
        title: String,
        courseId: Long?
    ) {
        val existing = copilotChatDao.getSessionById(sessionId)
        if (existing == null) {
            copilotChatDao.insertSession(
                CopilotSession(
                    id = sessionId,
                    title = title,
                    courseId = courseId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            copilotChatDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
        }
    }

    suspend fun saveMessage(
        sessionId: String,
        message: CopilotMessageItem
    ) {
        val sourcesJson = if (message.sources.isNotEmpty()) {
            gson.toJson(message.sources)
        } else {
            null
        }

        val entity = CopilotMessageEntity(
            id = message.id,
            sessionId = sessionId,
            role = if (message.role == CopilotRole.USER) "USER" else "ASSISTANT",
            text = message.text,
            sourcesJson = sourcesJson,
            actionFeedback = message.actionFeedback,
            timestamp = message.timestamp
        )
        copilotChatDao.insertMessage(entity)
        copilotChatDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        copilotChatDao.updateSessionTitle(sessionId, newTitle)
    }

    suspend fun deleteSession(sessionId: String) {
        copilotChatDao.deleteSession(sessionId)
    }

    suspend fun deleteAllSessions() {
        copilotChatDao.deleteAllSessions()
    }
}
