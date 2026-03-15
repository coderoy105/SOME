package com.example.replybubble.recommendation

import android.util.Log
import com.example.replybubble.domain.model.ReplyCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterRecommendationEngine @Inject constructor(
    private val openRouterApiClient: OpenRouterApiClient,
    private val builtInRecommendationEngine: BuiltInRecommendationEngine,
) : RecommendationEngine {
    override suspend fun generate(request: RecommendationRequest): List<GeneratedReply> {
        val fallbackReplies = builtInRecommendationEngine.generate(request)
        val aiReplies = openRouterApiClient.generateReplies(request).orEmpty()
        if (aiReplies.isEmpty()) {
            Log.w(TAG, "Falling back to built-in recommendation engine")
        }

        val merged = LinkedHashMap<ReplyCategory, GeneratedReply>()
        aiReplies.forEach { reply ->
            if (reply.content.isNotBlank()) {
                merged[reply.category] = reply.copy(content = reply.content.trim())
            }
        }
        fallbackReplies.forEach { reply ->
            merged.putIfAbsent(reply.category, reply)
        }

        val mergedReplies = listOf(
            ReplyCategory.SAFE,
            ReplyCategory.WITTY,
            ReplyCategory.SWEET,
            ReplyCategory.SHORT,
            ReplyCategory.FOLLOW_UP,
        ).mapNotNull { merged[it] }

        return StyleLearningHelper.applyStyle(mergedReplies, request)
    }

    companion object {
        private const val TAG = "OpenRouterEngine"
    }
}
