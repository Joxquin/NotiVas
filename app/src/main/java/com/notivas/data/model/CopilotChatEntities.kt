package com.notivas.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "copilot_sessions"
)
data class CopilotSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val courseId: Long? = null,
    val totalTokens: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "copilot_messages",
    foreignKeys = [
        ForeignKey(
            entity = CopilotSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"])
    ]
)
data class CopilotMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "USER" or "ASSISTANT"
    val text: String,
    val sourcesJson: String? = null,
    val actionFeedback: String? = null,
    val tokens: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
