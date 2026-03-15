package com.example.replybubble.recommendation

import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.ConversationContext
import com.example.replybubble.domain.model.NormalizedOcrResult
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.model.StyleTrainingSample

data class RecommendationRequest(
    val profile: ContactProfile?,
    val settings: AppSettings,
    val normalized: NormalizedOcrResult,
    val context: ConversationContext,
    val adjustments: Set<StyleAdjustment> = emptySet(),
    val styleSamples: List<StyleTrainingSample> = emptyList(),
    val targetCategory: ReplyCategory? = null,
    val excludedContentByCategory: Map<ReplyCategory, String> = emptyMap(),
    val generationNonce: Long = 0L,
)

data class GeneratedReply(
    val category: ReplyCategory,
    val content: String,
)

interface RecommendationEngine {
    suspend fun generate(request: RecommendationRequest): List<GeneratedReply>
}
