package com.example.replybubble.domain.model

data class NormalizedOcrResult(
    val rawText: String,
    val cleanedText: String,
    val recentMessages: List<String>,
    val lastMessage: String,
    val lastQuestion: String?,
    val questionDetected: Boolean,
    val isLowConfidence: Boolean,
)

data class ConversationContext(
    val questionDetected: Boolean,
    val emotionalTone: EmotionalTone,
    val vibe: ConversationVibe,
    val lastMessage: String,
    val lastQuestion: String?,
    val topicKeyword: String?,
    val shortMessageBias: Boolean,
)

data class AnalysisSession(
    val id: Long,
    val contactId: Long?,
    val source: AnalysisSource,
    val rawOcrText: String,
    val cleanedOcrText: String,
    val recentMessages: List<String>,
    val lastMessage: String,
    val lastQuestion: String?,
    val questionDetected: Boolean,
    val emotionalTone: EmotionalTone,
    val vibe: ConversationVibe,
    val wasFallback: Boolean,
    val createdAt: Long,
)

data class ReplySuggestion(
    val id: Long,
    val sessionId: Long,
    val category: ReplyCategory,
    val content: String,
    val copiedCount: Int,
    val createdAt: Long,
)

data class SessionPreview(
    val id: Long,
    val contactName: String,
    val lastMessage: String,
    val questionDetected: Boolean,
    val vibe: ConversationVibe,
    val createdAt: Long,
    val source: AnalysisSource,
)

data class SessionDetail(
    val session: AnalysisSession,
    val contact: ContactProfile?,
    val suggestions: List<ReplySuggestion>,
)
