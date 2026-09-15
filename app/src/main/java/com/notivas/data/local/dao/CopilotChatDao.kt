package com.notivas.data.local.dao

import androidx.room.*
import com.notivas.data.model.AnanauMessageEntity
import com.notivas.data.model.AnanauSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AnanauChatDao {

    @Query("SELECT * FROM Ananau_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<AnanauSession>>

    @Query("SELECT * FROM Ananau_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AnanauSession?

    @Query("SELECT * FROM Ananau_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<AnanauMessageEntity>>

    @Query("SELECT * FROM Ananau_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: String): List<AnanauMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AnanauSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AnanauMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AnanauMessageEntity>)

    @Query("UPDATE Ananau_sessions SET title = :newTitle, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE Ananau_sessions SET totalTokens = :tokens, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTokens(sessionId: String, tokens: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE Ananau_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM Ananau_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM Ananau_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM Ananau_sessions")
    suspend fun deleteAllSessions()
}
