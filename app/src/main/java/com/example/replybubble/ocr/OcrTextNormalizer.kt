package com.example.replybubble.ocr

import com.example.replybubble.domain.model.NormalizedOcrResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrTextNormalizer @Inject constructor() {
    fun normalize(rawText: String): NormalizedOcrResult {
        val sanitized = rawText
            .replace("\r", "\n")
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t ]+"), " ")
            .trim()

        if (sanitized.isBlank()) {
            return emptyResult(rawText)
        }

        val normalizedLines = sanitized
            .split('\n')
            .map { normalizeLine(it) }
            .filter { it.content.isNotBlank() }
            .filterNot { isUiNoise(it.content) }

        val hasTaggedSpeaker = normalizedLines.any { it.speaker != Speaker.UNKNOWN }
        val filteredLines = if (hasTaggedSpeaker) {
            normalizedLines.filter { it.speaker != Speaker.UNKNOWN }
        } else {
            normalizedLines
        }

        if (filteredLines.isEmpty()) {
            return emptyResult(rawText)
        }

        val merged = mutableListOf<MessageLine>()
        filteredLines.forEach { current ->
            val previous = merged.lastOrNull()
            val shouldMerge = previous != null &&
                previous.speaker == current.speaker &&
                previous.content.length < 28 &&
                current.content.length < 28 &&
                !looksLikeSentenceEnd(previous.content)

            if (shouldMerge) {
                merged[merged.lastIndex] = previous.copy(
                    content = "${previous.content} ${current.content}".trim(),
                )
            } else if (previous?.content != current.content || previous.speaker != current.speaker) {
                merged += current
            }
        }

        val recentLines = merged.takeLast(12)
        val taggedMessages = recentLines.map { line ->
            when (line.speaker) {
                Speaker.OTHER -> "상대: ${line.content}"
                Speaker.ME -> "나: ${line.content}"
                Speaker.UNKNOWN -> line.content
            }
        }

        val lastIncoming = recentLines
            .asReversed()
            .firstOrNull { it.speaker == Speaker.OTHER }
            ?.content
            .orEmpty()

        val lastVisible = recentLines.lastOrNull()?.content.orEmpty()
        val lastIncomingQuestion = recentLines
            .asReversed()
            .firstOrNull { it.speaker == Speaker.OTHER && looksLikeQuestion(it.content) }
            ?.content

        val fallbackQuestion = recentLines
            .asReversed()
            .firstOrNull { it.speaker != Speaker.ME && looksLikeQuestion(it.content) }
            ?.content

        val cleanedText = taggedMessages.joinToString("\n")
        val opponentCount = recentLines.count { it.speaker == Speaker.OTHER }
        val lowConfidence = opponentCount == 0 || cleanedText.length < 18

        return NormalizedOcrResult(
            rawText = rawText,
            cleanedText = cleanedText,
            recentMessages = taggedMessages,
            lastMessage = lastIncoming.ifBlank { lastVisible },
            lastQuestion = lastIncomingQuestion ?: fallbackQuestion,
            questionDetected = lastIncomingQuestion != null || fallbackQuestion != null,
            isLowConfidence = lowConfidence,
        )
    }

    private fun normalizeLine(rawLine: String): MessageLine {
        val trimmed = rawLine.trim().replace(Regex("\\s+"), " ")
        val speaker = when {
            trimmed.startsWith("상대:") -> Speaker.OTHER
            trimmed.startsWith("나:") -> Speaker.ME
            else -> Speaker.UNKNOWN
        }

        val content = stripDecorations(
            trimmed
                .removePrefix("상대:")
                .removePrefix("나:")
                .trim(),
        )

        return MessageLine(
            speaker = speaker,
            content = content,
        )
    }

    private fun emptyResult(rawText: String): NormalizedOcrResult {
        return NormalizedOcrResult(
            rawText = rawText,
            cleanedText = "",
            recentMessages = emptyList(),
            lastMessage = "",
            lastQuestion = null,
            questionDetected = false,
            isLowConfidence = true,
        )
    }

    private fun stripDecorations(line: String): String {
        return line
            .replace(Regex("^\\[[^\\]]+\\]\\s*"), "")
            .replace(Regex("^\\d{1,2}:\\d{2}\\s*"), "")
            .replace(Regex("^(오전|오후)\\s*\\d{1,2}:\\d{2}\\s*"), "")
            .replace(Regex("^[>•·\\-]+\\s*"), "")
            .trim()
    }

    private fun isUiNoise(line: String): Boolean {
        val normalized = line.lowercase()
        if (looksLikeTimestamp(line)) return true
        if (normalized.length <= 1) return true

        val exactNoise = setOf(
            "프로필",
            "프로필 보기",
            "프로필 사진",
            "전송",
            "보내기",
            "메시지 입력",
            "메시지를 입력하세요",
            "답장",
            "입력",
            "사진",
            "카메라",
            "갤러리",
            "앨범",
            "검색",
            "메뉴",
            "복사",
            "공유",
            "replybubble",
            "읽음",
            "스티커",
            "더보기",
            "스티커 더보기",
            "이모티콘",
            "이모지",
            "gif",
            "사진 보기",
        )
        if (normalized in exactNoise) return true

        val noisyPatterns = listOf(
            Regex("^\\d+개$"),
            Regex("^사진\\s*\\d+장$"),
            Regex("^\\d{1,2}:\\d{2}$"),
            Regex("^(오전|오후)\\s*\\d{1,2}:\\d{2}$"),
            Regex("^new$"),
            Regex("^typing\\.\\.\\.$"),
            Regex(".*프로필.*"),
            Regex(".*스티커.*"),
            Regex(".*더보기.*"),
            Regex(".*메시지 입력.*"),
        )
        return noisyPatterns.any { it.matches(normalized) }
    }

    private fun looksLikeTimestamp(text: String): Boolean {
        return Regex("^(오전|오후)?\\s*\\d{1,2}:\\d{2}$").matches(text.trim())
    }

    private fun looksLikeSentenceEnd(text: String): Boolean {
        return text.endsWith("?") || text.endsWith("!") || text.endsWith(".")
    }

    private fun looksLikeQuestion(line: String): Boolean {
        val normalized = line.trim()
        val keywords = listOf(
            "뭐해",
            "뭐 하고",
            "왜",
            "어디",
            "언제",
            "누구",
            "어때",
            "갈래",
            "먹을래",
            "괜찮아",
            "왔어",
            "했어",
            "볼래",
            "시간 돼",
            "가능해",
        )
        return normalized.endsWith("?") || keywords.any { normalized.contains(it) }
    }

    private data class MessageLine(
        val speaker: Speaker,
        val content: String,
    )

    private enum class Speaker {
        OTHER,
        ME,
        UNKNOWN,
    }
}
