package com.example.replybubble.recommendation

import com.example.replybubble.domain.model.ConversationContext
import com.example.replybubble.domain.model.ConversationVibe
import com.example.replybubble.domain.model.EmotionalTone
import com.example.replybubble.domain.model.NormalizedOcrResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationAnalyzer @Inject constructor() {
    fun analyze(result: NormalizedOcrResult): ConversationContext {
        val recentContents = result.recentMessages.map(::stripSpeakerPrefix)
        val target = result.lastQuestion ?: result.lastMessage.ifBlank {
            recentContents.lastOrNull().orEmpty()
        }
        val joined = recentContents.joinToString(" ")
        val shortBias = recentContents.isNotEmpty() &&
            recentContents.map { it.length }.average() < 11.0

        val emotion = when {
            containsAny(target, "피곤", "졸려", "지침", "힘들", "바빴", "야근", "출근") -> EmotionalTone.TIRED
            containsAny(target, "좋아", "행복", "웃기", "신나", "재밌", "설레") -> EmotionalTone.POSITIVE
            result.questionDetected || containsAny(target, "왜", "뭐", "어디", "언제", "어때", "가능해") -> EmotionalTone.CURIOUS
            containsAny(target, "몰라", "별로", "그냥", "싫", "어색") -> EmotionalTone.COLD
            else -> EmotionalTone.NEUTRAL
        }

        val vibe = when {
            containsAny(joined, "보고 싶", "생각나", "좋아", "설레", "데이트", "자기") -> ConversationVibe.AFFECTIONATE
            containsAny(joined, "ㅋㅋ", "ㅎㅎ", "장난", "웃기", "밈", "놀리") -> ConversationVibe.PLAYFUL
            shortBias || containsAny(joined, "응", "ㅇㅇ", "그래", "넵", "흠") -> ConversationVibe.AWKWARD
            emotion == EmotionalTone.POSITIVE || result.questionDetected -> ConversationVibe.LIGHT
            else -> ConversationVibe.NEUTRAL
        }

        return ConversationContext(
            questionDetected = result.questionDetected,
            emotionalTone = emotion,
            vibe = vibe,
            lastMessage = result.lastMessage,
            lastQuestion = result.lastQuestion,
            topicKeyword = detectTopic(target),
            shortMessageBias = shortBias,
        )
    }

    private fun detectTopic(text: String): String? {
        val candidates = linkedMapOf(
            "food" to listOf("먹", "메뉴", "밥", "배달", "저녁", "점심", "카페", "커피", "디저트"),
            "schedule" to listOf("언제", "시간", "이번 주", "주말", "내일", "오늘", "약속", "가능"),
            "where" to listOf("어디", "갈래", "가자", "장소", "만날", "근처"),
            "content" to listOf("영화", "드라마", "넷플릭스", "유튜브", "노래", "콘서트", "웹툰"),
            "work" to listOf("회사", "학교", "과제", "시험", "회의", "출근", "야근", "수업"),
            "condition" to listOf("피곤", "졸려", "힘들", "아파", "컨디션", "바쁨"),
            "weather" to listOf("날씨", "비", "눈", "춥", "덥", "바람"),
            "checkin" to listOf("뭐해", "뭐 하고", "지금", "깼어", "왔어", "했어"),
        )
        return candidates.entries.firstOrNull { (_, keywords) ->
            keywords.any { text.contains(it, ignoreCase = true) }
        }?.key
    }

    private fun stripSpeakerPrefix(line: String): String {
        return line
            .removePrefix("상대:")
            .removePrefix("나:")
            .trim()
    }

    private fun containsAny(source: String, vararg keywords: String): Boolean {
        return keywords.any { source.contains(it, ignoreCase = true) }
    }
}
