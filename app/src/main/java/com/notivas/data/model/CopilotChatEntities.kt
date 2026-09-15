package com.notivas.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "Ananau_sessions"
)
data class AnanauSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val courseId: Long? = null,
    val totalTokens: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "Ananau_messages",
    foreignKeys = [
        ForeignKey(
            entity = AnanauSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"])
    ]
)
data class AnanauMessageEntity(
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
