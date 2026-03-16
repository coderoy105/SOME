package com.example.replybubble.recommendation

import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.ConversationVibe
import com.example.replybubble.domain.model.EmotionalTone
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.model.ToneStyle

object OpenRouterPromptBuilder {
    fun buildSystemPrompt(): String {
        return """
            You are SOME, a Korean DM reply assistant.
            Messages may be tagged as:
            - "상대:" = the other person's message
            - "나:" = the user's own message
            Your job is to recommend replies to the latest relevant "상대:" message, not to the user's own message.
            Read the full conversation for context, but answer based on the latest visible message from the other person.
            Output only JSON with this exact shape:
            {"suggestions":[
              {"category":"SAFE","content":"..."},
              {"category":"WITTY","content":"..."},
              {"category":"SWEET","content":"..."},
              {"category":"SHORT","content":"..."},
              {"category":"FOLLOW_UP","content":"..."}
            ]}
            Rules:
            - Write natural Korean DM replies.
            - Every suggestion must be directly relevant to the latest "상대:" message.
            - Use the full conversation only as supporting context.
            - Ignore non-message UI labels such as profile labels, photo labels, sticker menus, "더보기", input bars, send buttons, gallery buttons, and system labels.
            - Strongly imitate the user's saved writing style examples when they are provided.
            - Match the user's sentence ending, punctuation habit, laugh-token usage, and overall brevity as closely as possible.
            - Respect relationship, tone, constraints, style adjustments, and punctuation limits.
            - Do not overuse repeated punctuation or laughter symbols unless the saved examples clearly do that.
            - SAFE should be safest and most natural.
            - WITTY should be playful but still on-topic.
            - SWEET should be warm or lightly romantic only if allowed.
            - SHORT should be concise.
            - FOLLOW_UP should briefly react to the latest message first, then ask one easy-to-answer follow-up question.
            - FOLLOW_UP must not be a bare standalone question with no reaction.
            - No markdown, no explanations, no numbering, no backticks.
            - Keep each reply to one or two short sentences.
        """.trimIndent()
    }

    fun buildUserPrompt(request: RecommendationRequest): String {
        val profile = request.profile
        val targetCategory = request.targetCategory
        val styleProfile = StyleLearningHelper.buildProfile(request.styleSamples)
        val styleGuide = StyleLearningHelper.buildPromptHint(styleProfile)
        val recentMessages = request.normalized.recentMessages
            .takeLast(12)
            .joinToString(separator = "\n") { "- $it" }
            .ifBlank { "- 대화 추출이 충분하지 않음" }
        val constraints = formatConstraints(profile)
        val adjustments = request.adjustments
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { adjustmentLabel(it) }
            ?: "없음"
        val styleExamples = request.styleSamples
            .take(6)
            .joinToString(separator = "\n") { sample ->
                "- 질문: ${sample.prompt}\n  내 답변: ${sample.answer}"
            }
            .ifBlank { "- 저장된 말투 학습 예시 없음" }

        return buildString {
            appendLine("다음 정보를 바탕으로 한국어 DM 답장을 추천해 줘.")
            appendLine()
            appendLine("[상대 프로필]")
            appendLine("이름: ${profile?.name ?: "미선택"}")
            appendLine("관계: ${relationshipLabel(profile?.relationshipType)}")
            appendLine("말투: ${toneLabel(profile?.toneStyle)}")
            appendLine("제한 옵션: $constraints")
            appendLine("스타일 조정: $adjustments")
            appendLine()
            appendLine("[문장 습관 제한]")
            appendLine(
                "쉼표/느낌표 과다 제한: ${if (request.settings.limitPunctuationOveruse) "켜짐" else "꺼짐"}, " +
                    "ㅎ/ㅋ 과다 제한: ${if (request.settings.limitLaughOveruse) "켜짐" else "꺼짐"}",
            )
            appendLine()
            appendLine("[대화 분석]")
            appendLine("상대 질문 여부: ${if (request.context.questionDetected) "질문 있음" else "명확한 질문 없음"}")
            appendLine("상대 감정 톤: ${emotionalToneLabel(request.context.emotionalTone)}")
            appendLine("대화 분위기: ${vibeLabel(request.context.vibe)}")
            appendLine("추정 주제: ${request.context.topicKeyword ?: "불명확"}")
            appendLine()
            appendLine("[전체 대화]")
            appendLine(request.normalized.cleanedText.take(1800).ifBlank { request.normalized.rawText.take(1800) })
            appendLine()
            appendLine("[최근 대화 목록]")
            appendLine(recentMessages)
            appendLine()
            appendLine("[말투 요약]")
            appendLine(styleGuide)
            appendLine()
            appendLine("[사용자 말투 학습 예시]")
            appendLine(styleExamples)
            appendLine()
            appendLine("[답장해야 하는 마지막 상대 메시지]")
            appendLine(request.normalized.lastMessage.ifBlank { "없음" })
            appendLine()
            appendLine("[마지막 상대 질문]")
            appendLine(request.normalized.lastQuestion ?: "없음")
            if (targetCategory != null) {
                appendLine()
                appendLine("[재생성 요청]")
                appendLine("특히 ${replyCategoryLabel(targetCategory)} 카테고리를 새로 만들어 줘.")
                appendLine(
                    "이전 ${replyCategoryLabel(targetCategory)} 답장: " +
                        (request.excludedContentByCategory[targetCategory] ?: "없음"),
                )
                appendLine("이전 답장과 문장 구조나 표현이 겹치지 않게 바꿔 줘.")
            }
            appendLine()
            appendLine("반드시 마지막 상대 메시지에 답하는 형태로 추천해 줘.")
            appendLine("사용자 자신의 마지막 발화인 '나:' 메시지에 반응하면 안 된다.")
            appendLine("학습 예시가 있으면 그 문체와 말끝을 최대한 비슷하게 맞춰 줘.")
            appendLine("저장된 예시가 짧고 담백하면 답장도 짧고 담백하게 맞춰 줘.")
            appendLine("저장된 예시가 ㅎㅎ, ㅋ, !, , 를 거의 안 쓰면 추천 답장에서도 남발하지 마.")
        }
    }

    private fun formatConstraints(profile: ContactProfile?): String {
        val constraints = profile?.constraints.orEmpty()
        return if (constraints.isEmpty()) {
            "없음"
        } else {
            constraints.joinToString(", ") { constraintLabel(it) }
        }
    }

    private fun relationshipLabel(type: RelationshipType?): String {
        return when (type) {
            RelationshipType.FRIEND -> "친구"
            RelationshipType.CRUSH -> "썸"
            RelationshipType.PARTNER -> "연인"
            RelationshipType.INTEREST -> "관심 있는 사람"
            RelationshipType.SENIOR_JUNIOR -> "선배/후배"
            RelationshipType.STRANGER -> "낯선 사람"
            null -> "미선택"
        }
    }

    private fun toneLabel(style: ToneStyle?): String {
        return when (style) {
            ToneStyle.CUTE -> "귀엽게"
            ToneStyle.WITTY -> "재치 있게"
            ToneStyle.WARM -> "다정하게"
            ToneStyle.FLIRTY -> "설레게"
            ToneStyle.CASUAL -> "편하게"
            ToneStyle.SERIOUS -> "진지하게"
            null -> "미선택"
        }
    }

    private fun constraintLabel(constraint: ReplyConstraint): String {
        return when (constraint) {
            ReplyConstraint.NO_HEAVY_FLIRTING -> "과한 플러팅 금지"
            ReplyConstraint.NO_LONG_REPLY -> "너무 긴 답장 금지"
            ReplyConstraint.NO_EMOJI -> "이모지 사용 안 함"
            ReplyConstraint.FORCE_CASUAL -> "반말 고정"
            ReplyConstraint.FORCE_FORMAL -> "존댓말 고정"
            ReplyConstraint.MINIMIZE_CRINGE -> "오글거림 최소화"
        }
    }

    private fun adjustmentLabel(adjustment: StyleAdjustment): String {
        return when (adjustment) {
            StyleAdjustment.MORE_CUTE -> "더 귀엽게"
            StyleAdjustment.SHORTER -> "더 짧게"
            StyleAdjustment.MORE_NATURAL -> "더 자연스럽게"
            StyleAdjustment.MORE_ROMANTIC -> "더 설레게"
            StyleAdjustment.MORE_CASUAL -> "더 편하게"
        }
    }

    private fun replyCategoryLabel(category: ReplyCategory): String {
        return when (category) {
            ReplyCategory.SAFE -> "안전형"
            ReplyCategory.WITTY -> "재치형"
            ReplyCategory.SWEET -> "설렘형"
            ReplyCategory.SHORT -> "짧은형"
            ReplyCategory.FOLLOW_UP -> "이어가기 질문형"
        }
    }

    private fun emotionalToneLabel(tone: EmotionalTone): String {
        return when (tone) {
            EmotionalTone.POSITIVE -> "긍정적"
            EmotionalTone.CURIOUS -> "궁금함"
            EmotionalTone.TIRED -> "지침"
            EmotionalTone.COLD -> "차분함"
            EmotionalTone.NEUTRAL -> "중립"
        }
    }

    private fun vibeLabel(vibe: ConversationVibe): String {
        return when (vibe) {
            ConversationVibe.LIGHT -> "가벼움"
            ConversationVibe.PLAYFUL -> "장난스러움"
            ConversationVibe.AFFECTIONATE -> "호감형"
            ConversationVibe.NEUTRAL -> "무난함"
            ConversationVibe.AWKWARD -> "어색함"
        }
    }
}
