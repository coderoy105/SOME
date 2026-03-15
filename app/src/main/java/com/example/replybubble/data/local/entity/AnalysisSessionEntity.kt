package com.example.replybubble.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ContactProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("contactId")],
)
data class AnalysisSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long?,
    val source: String,
    val rawOcrText: String,
    val cleanedOcrText: String,
    val recentMessages: List<String>,
    val lastMessage: String,
    val lastQuestion: String?,
    val questionDetected: Boolean,
    val emotionalTone: String,
    val vibe: String,
    val wasFallback: Boolean,
    val createdAt: Long,
)
