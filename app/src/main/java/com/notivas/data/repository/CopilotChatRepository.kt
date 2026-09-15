package com.notivas.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notivas.data.local.dao.AnanauChatDao
import com.notivas.data.model.AnanauMessageEntity
import com.notivas.data.model.AnanauSession
import com.notivas.ui.Ananau.AnanauMessageItem
import com.notivas.ui.Ananau.AnanauRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnanauChatRepository @Inject constructor(
    private val AnanauChatDao: AnanauChatDao
) {
    private val gson = Gson()

    val allSessions: Flow<List<AnanauSession>> = AnanauChatDao.getAllSessions()

    suspend fun getSessionById(sessionId: String): AnanauSession? {
        return AnanauChatDao.getSessionById(sessionId)
    }

    suspend fun getMessagesForSession(sessionId: String): List<AnanauMessageItem> {
        val entities = AnanauChatDao.getMessagesForSessionOnce(sessionId)
        return entities.map { entity ->
            val sources = if (!entity.sourcesJson.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<AnanauSource>>() {}.type
                    gson.fromJson<List<AnanauSource>>(entity.sourcesJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            AnanauMessageItem(
                id = entity.id,
                role = if (entity.role == "USER") AnanauRole.USER else AnanauRole.ASSISTANT,
                text = entity.text,
                sources = sources,
                actionFeedback = entity.actionFeedback,
                tokens = entity.tokens,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun createOrUpdateSession(
        sessionId: String,
        title: String,
        courseId: Long?,
        initialTokens: Int = 0
    ) {
        val existing = AnanauChatDao.getSessionById(sessionId)
        if (existing == null) {
            AnanauChatDao.insertSession(
                AnanauSession(
                    id = sessionId,
                    title = title,
                    courseId = courseId,
                    totalTokens = initialTokens,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            AnanauChatDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
        }
    }

    suspend fun updateSessionTokens(sessionId: String, tokens: Int) {
        AnanauChatDao.updateSessionTokens(sessionId, tokens)
    }

    suspend fun saveMessage(
        sessionId: String,
        message: AnanauMessageItem
    ) {
        val sourcesJson = if (message.sources.isNotEmpty()) {
            gson.toJson(message.sources)
        } else {
            null
        }

        val entity = AnanauMessageEntity(
            id = message.id,
            sessionId = sessionId,
            role = if (message.role == AnanauRole.USER) "USER" else "ASSISTANT",
            text = message.text,
            sourcesJson = sourcesJson,
            actionFeedback = message.actionFeedback,
            tokens = message.tokens,
            timestamp = message.timestamp
        )
        AnanauChatDao.insertMessage(entity)
        AnanauChatDao.updateSessionTimestamp(sessionId, System.currentTimeMillis())
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        AnanauChatDao.updateSessionTitle(sessionId, newTitle)
    }

    suspend fun deleteSession(sessionId: String) {
        AnanauChatDao.deleteSession(sessionId)
    }

    suspend fun deleteAllSessions() {
        AnanauChatDao.deleteAllSessions()
    }
}
