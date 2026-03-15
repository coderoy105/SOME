package com.example.replybubble.correction

object OpenRouterCorrectionPromptBuilder {
    fun buildSystemPrompt(): String {
        return """
            You correct Korean DM drafts.
            Your job is to fix spelling and spacing only.
            Preserve the user's tone, slang, politeness level, emojis, laughter tokens like ㅎㅎ/ㅋㅋ, and overall vibe.
            Do not rewrite the meaning.
            Do not make the sentence more formal unless it already is.
            Output only JSON in this exact shape:
            {"correctedText":"..."}
        """.trimIndent()
    }

    fun buildUserPrompt(text: String): String {
        return buildString {
            appendLine("아래 문장을 맞춤법과 띄어쓰기만 자연스럽게 교정해 줘.")
            appendLine("말투, 분위기, 반말/존댓말, ㅎㅎ/ㅋㅋ, 이모지는 최대한 유지해.")
            appendLine("문장 의미를 바꾸지 말고, 과하게 매끈하게 바꾸지도 마.")
            appendLine()
            appendLine("[원문]")
            appendLine(text)
        }
    }
}
