package com.notivas.data.local.dao

import androidx.room.*
import com.notivas.data.model.CopilotMessageEntity
import com.notivas.data.model.CopilotSession
import kotlinx.coroutines.flow.Flow

@Dao
interface CopilotChatDao {

    @Query("SELECT * FROM copilot_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<CopilotSession>>

    @Query("SELECT * FROM copilot_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): CopilotSession?

    @Query("SELECT * FROM copilot_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<CopilotMessageEntity>>

    @Query("SELECT * FROM copilot_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: String): List<CopilotMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CopilotSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CopilotMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CopilotMessageEntity>)

    @Query("UPDATE copilot_sessions SET title = :newTitle, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE copilot_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM copilot_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM copilot_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM copilot_sessions")
    suspend fun deleteAllSessions()
}
