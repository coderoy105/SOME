package com.example.replybubble.recommendation

import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.StyleTrainingSample

internal data class LearnedStyleProfile(
    val preferredEnding: String? = null,
    val averageLength: Int = 0,
    val usesLaughing: Boolean = false,
    val laughToken: String? = null,
    val usesExclamation: Boolean = false,
    val commonPhrases: List<String> = emptyList(),
)

internal object StyleLearningHelper {
    fun buildProfile(samples: List<StyleTrainingSample>): LearnedStyleProfile {
        if (samples.isEmpty()) return LearnedStyleProfile()

        val answers = samples.map { it.answer.trim() }.filter { it.isNotBlank() }
        if (answers.isEmpty()) return LearnedStyleProfile()

        val endings = answers
            .mapNotNull { extractEnding(it) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val laughToken = answers
            .flatMap { extractLaughTokens(it) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val commonPhrases = answers
            .flatMap { extractCommonPhrases(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        return LearnedStyleProfile(
            preferredEnding = endings,
            averageLength = answers.map { it.length }.average().toInt(),
            usesLaughing = answers.any { it.contains("ㅋ") || it.contains("ㅎ") },
            laughToken = laughToken,
            usesExclamation = answers.any { it.contains("!") },
            commonPhrases = commonPhrases,
        )
    }

    fun buildPromptHint(profile: LearnedStyleProfile): String {
        if (profile == LearnedStyleProfile()) return "저장된 말투 예시가 아직 충분하지 않습니다."

        val parts = mutableListOf<String>()
        profile.preferredEnding?.let { parts += "자주 쓰는 말끝: $it" }
        if (profile.averageLength > 0) {
            parts += if (profile.averageLength <= 16) {
                "답변 길이는 짧고 간결한 편"
            } else if (profile.averageLength <= 32) {
                "답변 길이는 보통"
            } else {
                "답변 길이는 비교적 긴 편"
            }
        }
        if (profile.usesLaughing) {
            parts += "웃음 표현 사용: ${profile.laughToken ?: "있음"}"
        } else {
            parts += "웃음 표현은 거의 사용하지 않음"
        }
        parts += if (profile.usesExclamation) "느낌표를 가끔 사용함" else "느낌표를 거의 사용하지 않음"
        if (profile.commonPhrases.isNotEmpty()) {
            parts += "자주 쓰는 표현: ${profile.commonPhrases.joinToString(", ")}"
        }
        return parts.joinToString(", ")
    }

    fun applyStyle(
        replies: List<GeneratedReply>,
        request: RecommendationRequest,
    ): List<GeneratedReply> {
        val styleProfile = buildProfile(request.styleSamples)
        return replies.map { reply ->
            reply.copy(
                content = applyStyleToText(
                    text = reply.content,
                    styleProfile = styleProfile,
                    settings = request.settings,
                    category = reply.category,
                ),
            )
        }
    }

    private fun applyStyleToText(
        text: String,
        styleProfile: LearnedStyleProfile,
        settings: AppSettings,
        category: ReplyCategory,
    ): String {
        var result = text.trim().replace(Regex("\\s+"), " ")

        if (
            styleProfile.preferredEnding != null &&
            category != ReplyCategory.SHORT &&
            styleProfile.preferredEnding in SAFE_APPENDABLE_ENDINGS
        ) {
            result = alignEnding(result, styleProfile.preferredEnding)
        }

        if (styleProfile.usesLaughing && !settings.limitLaughOveruse) {
            val token = styleProfile.laughToken
            if (token != null && token !in result && category == ReplyCategory.WITTY) {
                result = result.removeSuffix(".").trimEnd() + " $token"
            }
        }

        if (!styleProfile.usesExclamation || settings.limitPunctuationOveruse) {
            result = result.replace(Regex("!+"), "!")
        }
        if (!styleProfile.usesExclamation) {
            result = result.replace("!", "")
        }

        if (settings.limitPunctuationOveruse) {
            result = result
                .replace(Regex(",{2,}"), ",")
                .replace(Regex("!{2,}"), "!")
                .replace(Regex("\\s*,\\s*"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        if (settings.limitLaughOveruse) {
            result = result
                .replace(Regex("ㅋ{2,}"), "ㅋ")
                .replace(Regex("ㅎ{2,}"), "ㅎ")
                .replace(Regex("(ㅋ\\s*){2,}"), "ㅋ ")
                .replace(Regex("(ㅎ\\s*){2,}"), "ㅎ ")
                .trim()
        } else if (!styleProfile.usesLaughing) {
            result = result
                .replace(Regex("(ㅋ|ㅎ){2,}"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        if (styleProfile.averageLength in 1..18 && result.length > 26 && category != ReplyCategory.FOLLOW_UP) {
            result = shortenToLength(result, 24)
        }

        return result
    }

    private fun alignEnding(
        text: String,
        ending: String,
    ): String {
        val trimmedEnding = ending.trim()
        if (trimmedEnding.isBlank()) return text
        if (text.endsWith(trimmedEnding)) return text

        val base = text
            .removeSuffix(".")
            .removeSuffix("!")
            .removeSuffix("?")
            .trim()

        return when {
            trimmedEnding == "ㅎㅎ" || trimmedEnding == "ㅋㅋ" -> "$base $trimmedEnding"
            trimmedEnding == "요" && !base.endsWith("요") -> "${base}요"
            else -> text
        }
    }

    private fun extractEnding(text: String): String? {
        val cleaned = text
            .trim()
            .replace(Regex("\\s+"), " ")
            .trimEnd('.', '!', '?')
        if (cleaned.isBlank()) return null
        return when {
            cleaned.endsWith("ㅎㅎ") -> "ㅎㅎ"
            cleaned.endsWith("ㅋㅋ") -> "ㅋㅋ"
            cleaned.endsWith("요") -> "요"
            else -> null
        }
    }

    private fun extractLaughTokens(text: String): List<String> {
        return Regex("(ㅋ{1,4}|ㅎ{1,4}|ㅋㅋ+|ㅎㅎ+)").findAll(text)
            .map { it.value }
            .toList()
    }

    private fun extractCommonPhrases(text: String): List<String> {
        return text.split(Regex("[\\s,.!?]+"))
            .map { it.trim() }
            .filter { token ->
                token.length in 2..6 &&
                    token.none(Char::isDigit) &&
                    token !in COMMON_STYLE_STOPWORDS
            }
    }

    private fun shortenToLength(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength).trimEnd() + "..."
    }

    private val COMMON_STYLE_STOPWORDS = setOf(
        "나는",
        "나도",
        "너는",
        "저는",
        "저도",
        "그냥",
        "진짜",
        "지금",
        "오늘",
        "약간",
        "조금",
        "근데",
        "그래서",
        "이거",
        "그거",
        "뭔가",
        "그러게",
    )

    private val SAFE_APPENDABLE_ENDINGS = setOf(
        "요",
        "ㅎㅎ",
        "ㅋㅋ",
    )
}
