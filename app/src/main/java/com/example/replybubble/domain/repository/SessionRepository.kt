package com.example.replybubble.domain.repository

import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.model.SessionDetail
import com.example.replybubble.domain.model.SessionPreview
import com.example.replybubble.domain.model.StyleAdjustment
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeRecentSessions(): Flow<List<SessionPreview>>
    fun observeLatestSession(): Flow<SessionPreview?>
    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?>
    suspend fun getSessionDetail(sessionId: Long): SessionDetail?
    suspend fun processCapturedText(
        contactId: Long?,
        rawText: String,
        source: AnalysisSource,
        adjustments: Set<StyleAdjustment> = emptySet(),
    ): Long

    suspend fun createDemoSession(contactId: Long?): Long
    suspend fun regenerateSuggestions(sessionId: Long, adjustments: Set<StyleAdjustment>)
    suspend fun regenerateSuggestion(replyId: Long, adjustments: Set<StyleAdjustment> = emptySet())
    suspend fun incrementCopyCount(replyId: Long)
    suspend fun deleteSession(sessionId: Long)
    suspend fun clearHistory()
}
