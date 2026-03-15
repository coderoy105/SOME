package com.example.replybubble.correction

import com.example.replybubble.recommendation.OpenRouterApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterTextCorrectionEngine @Inject constructor(
    private val openRouterApiClient: OpenRouterApiClient,
    private val builtInTextCorrectionEngine: BuiltInTextCorrectionEngine,
) : TextCorrectionEngine {
    override suspend fun correct(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ""

        val corrected = openRouterApiClient.correctText(trimmed)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return corrected ?: builtInTextCorrectionEngine.correct(trimmed)
    }
}
