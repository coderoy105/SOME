package com.example.replybubble.recommendation

import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.model.ToneStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private enum class SpeechMode {
    CASUAL,
    FORMAL,
}

@Singleton
class BuiltInRecommendationEngine @Inject constructor() : RecommendationEngine {
    override suspend fun generate(request: RecommendationRequest): List<GeneratedReply> {
        val profile = request.profile
        val relationship = profile?.relationshipType ?: request.settings.defaultRelationshipType
        val tone = profile?.toneStyle ?: request.settings.defaultToneStyle
        val constraints = profile?.constraints ?: emptySet()
        val speechMode = determineSpeechMode(relationship, tone, constraints, request.adjustments)
        val seed = abs(
            request.normalized.cleanedText.ifBlank { "fallback" }.hashCode() +
                request.generationNonce.hashCode() +
                (request.targetCategory?.ordinal ?: 0) * 31,
        )

        val replies = listOf(
            GeneratedReply(ReplyCategory.SAFE, buildSafeReply(request, speechMode, relationship, tone, seed)),
            GeneratedReply(ReplyCategory.WITTY, buildWittyReply(request, speechMode, relationship, tone, seed + 7)),
            GeneratedReply(ReplyCategory.SWEET, buildSweetReply(request, speechMode, relationship, tone, constraints, seed + 13)),
            GeneratedReply(ReplyCategory.SHORT, buildShortReply(request, speechMode, seed + 19)),
            GeneratedReply(ReplyCategory.FOLLOW_UP, buildFollowUpReply(request, speechMode, relationship, tone, seed + 29)),
        )

        return replies.map { reply ->
            reply.copy(
                content = postProcess(
                    raw = reply.content,
                    speechMode = speechMode,
                    tone = tone,
                    constraints = constraints,
                    adjustments = request.adjustments,
                    seed = seed + reply.category.ordinal,
                ),
            )
        }
    }

    private fun buildSafeReply(
        request: RecommendationRequest,
        speechMode: SpeechMode,
        relationship: RelationshipType,
        tone: ToneStyle,
        seed: Int,
    ): String {
        if (request.normalized.isLowConfidence) {
            return when (speechMode) {
                SpeechMode.CASUAL -> "방금 봤어. 조금만 더 말해주면 자연스럽게 답해볼 수 있을 것 같아."
                SpeechMode.FORMAL -> "방금 확인했어요. 조금만 더 말씀해 주시면 더 자연스럽게 답을 맞춰볼 수 있을 것 같아요."
            }
        }

        return if (request.context.questionDetected) {
            when (request.context.topicKeyword) {
                "food" -> line(speechMode, "나도 메뉴 고민 중이야. 너는 지금 뭐가 제일 땡겨?", "저도 메뉴 고민 중이에요. 지금 가장 끌리는 메뉴가 뭐예요?")
                "schedule" -> line(speechMode, "나는 저녁쯤이 편해. 너는 언제 괜찮아?", "저는 저녁쯤이 편해요. 언제가 괜찮으세요?")
                "where" -> line(speechMode, "나는 조용한 데가 좋을 것 같아. 너는 어디 생각 중이야?", "저는 조용한 곳이 좋을 것 같아요. 어디 생각하고 계세요?")
                "content" -> line(speechMode, "그거 괜찮지. 나는 그런 분위기 좋아해.", "그거 괜찮아요. 저는 그런 분위기 좋아해요.")
                "work" -> {
                    if (containsAny(request.context.lastQuestion.orEmpty(), "퇴근", "끝났")) {
                        line(speechMode, "응, 이제 막 끝났어. 너는?", "네, 이제 막 끝났어요. 그쪽은요?")
                    } else {
                        line(speechMode, "오늘은 좀 바빴어. 너는 어땠어?", "오늘은 조금 바빴어요. 그쪽은 어떠셨어요?")
                    }
                }
                "condition" -> line(speechMode, "좀 피곤하긴 한데 괜찮아. 너는 컨디션 어때?", "조금 피곤하긴 한데 괜찮아요. 컨디션은 어떠세요?")
                "weather" -> line(speechMode, "오늘 날씨 진짜 애매하더라. 너는 밖이야?", "오늘 날씨가 애매하더라고요. 지금 밖에 계세요?")
                "checkin" -> line(speechMode, "나도 이제 좀 쉬는 중이야. 너는 뭐 하고 있어?", "저도 이제 좀 쉬고 있어요. 지금 뭐 하고 계세요?")
                else -> {
                    if (relationship == RelationshipType.STRANGER) {
                        line(speechMode, "저도 그 얘기 궁금했어요. 조금만 더 들려주실래요?", "저도 그 이야기 궁금했어요. 조금만 더 들려주실래요?")
                    } else {
                        line(speechMode, "나도 그거 궁금했어. 너 생각은 어때?", "저도 그거 궁금했어요. 어떻게 생각하세요?")
                    }
                }
            }
        } else {
            when (request.context.topicKeyword) {
                "food" -> line(speechMode, "그 얘기 들으니까 갑자기 배고파진다.", "그 얘기 들으니까 갑자기 배고파지네요.")
                "schedule" -> line(speechMode, "그 일정이면 슬슬 맞춰봐도 되겠다.", "그 일정이면 슬슬 맞춰봐도 되겠네요.")
                "where" -> line(speechMode, "그쪽 분위기 괜찮을 것 같네.", "그쪽 분위기 괜찮을 것 같네요.")
                "content" -> line(speechMode, "그 주제면 나도 계속 듣게 되더라.", "그 주제는 저도 계속 듣게 되더라고요.")
                "work" -> line(speechMode, "오늘 진짜 고생했겠다.", "오늘 정말 고생 많으셨겠어요.")
                "condition" -> line(speechMode, "오늘은 진짜 너무 무리하지 마.", "오늘은 너무 무리하지 마세요.")
                "weather" -> line(speechMode, "오늘 날씨가 괜히 사람 지치게 하더라.", "오늘 날씨가 괜히 사람을 지치게 하더라고요.")
                "checkin" -> line(speechMode, "지금 딱 연락하기 좋은 타이밍이네.", "지금 연락하기 딱 좋은 타이밍이네요.")
                else -> {
                    when {
                        request.context.vibe.name == "AFFECTIONATE" ->
                            line(speechMode, "그렇게 말해주니까 괜히 기분 좋아진다.", "그렇게 말씀해 주시니까 괜히 기분이 좋아지네요.")
                        else ->
                            line(speechMode, "그 말 들으니까 상황이 바로 그려진다.", "그 말씀 들으니까 상황이 바로 그려져요.")
                    }
                }
            }
        }
    }

    private fun buildWittyReply(
        request: RecommendationRequest,
        speechMode: SpeechMode,
        relationship: RelationshipType,
        tone: ToneStyle,
        seed: Int,
    ): String {
        val opener = pick(
            seed,
            listOf(
                line(speechMode, "이거 은근 답장 각이 좋네.", "이거 은근 답장 각이 좋네요."),
                line(speechMode, "이 흐름이면 센스 있게 받아칠 수 있겠다.", "이 흐름이면 센스 있게 받아칠 수 있겠네요."),
            ),
        )

        val body = when (request.context.topicKeyword) {
            "food" -> line(speechMode, "메뉴 얘기 나오면 갑자기 진지해지는 타입이거든.", "메뉴 얘기 나오면 갑자기 진지해지는 편이거든요.")
            "schedule" -> line(speechMode, "일정만 맞으면 거의 절반은 성공 아닌가.", "일정만 맞으면 거의 절반은 성공 아닌가요.")
            "where" -> line(speechMode, "장소 고르는 센스까지 보면 꽤 기대되는데.", "장소 고르는 센스까지 보면 꽤 기대되는데요.")
            "content" -> line(speechMode, "그 얘기 시작하면 나 생각보다 길게 말할 수도 있어.", "그 이야기 시작하면 저 생각보다 길게 말할 수도 있어요.")
            "work" -> line(speechMode, "오늘은 생존 모드였다는 말이 제일 정확할 듯.", "오늘은 생존 모드였다는 말이 제일 정확할 것 같아요.")
            "condition" -> line(speechMode, "오늘은 배터리 10퍼 감성으로 버티는 중이야.", "오늘은 배터리 10퍼 감성으로 버티는 중이에요.")
            "weather" -> line(speechMode, "이 날씨면 사람도 말투가 축축해질 수밖에 없지.", "이 날씨면 사람 말투도 축축해질 수밖에 없죠.")
            "checkin" -> line(speechMode, "타이밍 좋게 딱 걸렸네. 지금은 답장 가능한 상태야.", "타이밍 좋게 딱 걸렸네요. 지금은 답장 가능한 상태예요.")
            else -> {
                if (relationship == RelationshipType.STRANGER || tone == ToneStyle.SERIOUS) {
                    line(speechMode, "이 정도면 대화 이어갈 포인트가 꽤 많다.", "이 정도면 대화 이어갈 포인트가 꽤 많네요.")
                } else {
                    line(speechMode, "이 말투면 내가 그냥 넘기기 어렵지.", "이 말투면 그냥 넘기기 어렵죠.")
                }
            }
        }

        return compose(opener, body)
    }

    private fun buildSweetReply(
        request: RecommendationRequest,
        speechMode: SpeechMode,
        relationship: RelationshipType,
        tone: ToneStyle,
        constraints: Set<ReplyConstraint>,
        seed: Int,
    ): String {
        val affectionateAllowed = ReplyConstraint.NO_HEAVY_FLIRTING !in constraints

        if (!affectionateAllowed && request.context.topicKeyword == "condition") {
            return line(speechMode, "오늘은 진짜 푹 쉬었으면 좋겠다.", "오늘은 정말 푹 쉬셨으면 좋겠어요.")
        }

        return when {
            request.context.vibe.name == "AFFECTIONATE" && affectionateAllowed ->
                line(speechMode, "그렇게 말해주니까 괜히 계속 생각나네.", "그렇게 말씀해 주시니까 괜히 계속 생각나네요.")
            relationship == RelationshipType.PARTNER && affectionateAllowed ->
                line(speechMode, "이런 얘기 나한테 해주는 게 괜히 더 좋다.", "이런 이야기 저한테 해주시는 게 괜히 더 좋네요.")
            relationship == RelationshipType.CRUSH && affectionateAllowed ->
                line(speechMode, "이 대화 흐름이 은근 설렌다.", "이 대화 흐름이 은근 설레네요.")
            tone == ToneStyle.WARM || tone == ToneStyle.CUTE ->
                line(speechMode, "그렇게 말해주니까 마음이 좀 말랑해진다.", "그렇게 말씀해 주시니까 마음이 좀 말랑해지네요.")
            else ->
                pick(
                    seed,
                    listOf(
                        line(speechMode, "이렇게 얘기 이어가는 거 좋다.", "이렇게 이야기 이어가는 거 좋네요."),
                        line(speechMode, "괜히 더 답장하고 싶게 만든다.", "괜히 더 답장하고 싶게 만드네요."),
                    ),
                )
        }
    }

    private fun buildShortReply(
        request: RecommendationRequest,
        speechMode: SpeechMode,
        seed: Int,
    ): String {
        return when (request.context.topicKeyword) {
            "food" -> line(speechMode, "나도 배고파. 뭐 먹을래?", "저도 배고파요. 뭐 먹을래요?")
            "schedule" -> line(speechMode, "좋아. 시간만 맞춰보자.", "좋아요. 시간만 맞춰봐요.")
            "where" -> line(speechMode, "나는 그쪽도 괜찮아.", "저는 그쪽도 괜찮아요.")
            "content" -> line(speechMode, "그거 나도 좋아해.", "그거 저도 좋아해요.")
            "work" -> line(speechMode, "응, 이제 좀 살 것 같아.", "네, 이제 좀 살 것 같아요.")
            "condition" -> line(speechMode, "조금 피곤한데 괜찮아.", "조금 피곤한데 괜찮아요.")
            "weather" -> line(speechMode, "오늘 날씨 진짜 애매해.", "오늘 날씨가 진짜 애매하네요.")
            "checkin" -> line(speechMode, "나도 지금 쉬는 중.", "저도 지금 쉬는 중이에요.")
            else -> {
                pick(
                    seed,
                    listOf(
                        line(speechMode, "그 말 좋다.", "그 말 좋네요."),
                        line(speechMode, "나도 그렇게 생각해.", "저도 그렇게 생각해요."),
                    ),
                )
            }
        }
    }

    private fun buildFollowUpReply(
        request: RecommendationRequest,
        speechMode: SpeechMode,
        relationship: RelationshipType,
        tone: ToneStyle,
        seed: Int,
    ): String {
        return when (request.context.topicKeyword) {
            "food" -> line(speechMode, "지금 제일 먹고 싶은 게 뭐야?", "지금 제일 먹고 싶은 게 뭐예요?")
            "schedule" -> line(speechMode, "너는 언제가 제일 편해?", "언제가 제일 편하세요?")
            "where" -> line(speechMode, "너는 어느 쪽 분위기 생각 중이야?", "어느 쪽 분위기 생각 중이세요?")
            "content" -> line(speechMode, "그중에서 제일 좋았던 포인트가 뭐였어?", "그중에서 제일 좋았던 포인트가 뭐였어요?")
            "work" -> line(speechMode, "오늘 제일 힘들었던 건 뭐였어?", "오늘 제일 힘들었던 건 뭐였어요?")
            "condition" -> line(speechMode, "지금은 좀 나아졌어?", "지금은 좀 나아지셨어요?")
            "weather" -> line(speechMode, "거긴 지금 날씨 어때?", "거긴 지금 날씨 어때요?")
            "checkin" -> line(speechMode, "너는 지금 뭐 하다가 연락한 거야?", "지금 뭐 하다가 연락하신 거예요?")
            else -> {
                if (relationship == RelationshipType.STRANGER || tone == ToneStyle.SERIOUS) {
                    pick(
                        seed,
                        listOf(
                            line(speechMode, "조금만 더 자세히 들려주실래요?", "조금만 더 자세히 들려주실래요?"),
                            line(speechMode, "그 다음에는 어떻게 됐어요?", "그 다음에는 어떻게 됐어요?"),
                        ),
                    )
                } else {
                    pick(
                        seed,
                        listOf(
                            line(speechMode, "그래서 그다음엔 어떻게 됐어?", "그래서 그다음에는 어떻게 됐어요?"),
                            line(speechMode, "그 얘기 조금만 더 해봐.", "그 이야기 조금만 더 해봐요."),
                        ),
                    )
                }
            }
        }
    }

    private fun determineSpeechMode(
        relationship: RelationshipType,
        tone: ToneStyle,
        constraints: Set<ReplyConstraint>,
        adjustments: Set<StyleAdjustment>,
    ): SpeechMode {
        return when {
            ReplyConstraint.FORCE_FORMAL in constraints -> SpeechMode.FORMAL
            ReplyConstraint.FORCE_CASUAL in constraints -> SpeechMode.CASUAL
            StyleAdjustment.MORE_CASUAL in adjustments -> SpeechMode.CASUAL
            tone == ToneStyle.SERIOUS -> SpeechMode.FORMAL
            relationship == RelationshipType.STRANGER || relationship == RelationshipType.SENIOR_JUNIOR -> SpeechMode.FORMAL
            else -> SpeechMode.CASUAL
        }
    }

    private fun postProcess(
        raw: String,
        speechMode: SpeechMode,
        tone: ToneStyle,
        constraints: Set<ReplyConstraint>,
        adjustments: Set<StyleAdjustment>,
        seed: Int,
    ): String {
        var result = raw.trim().replace(Regex("\\s+"), " ")

        if (StyleAdjustment.MORE_CUTE in adjustments && speechMode == SpeechMode.CASUAL) {
            result = when {
                result.endsWith(".") -> result.removeSuffix(".")
                else -> result
            } + pick(seed, listOf(" ㅎㅎ", " 왠지 좋네"))
        }

        if (StyleAdjustment.MORE_ROMANTIC in adjustments && ReplyConstraint.NO_HEAVY_FLIRTING !in constraints) {
            result = compose(
                result,
                if (speechMode == SpeechMode.CASUAL) "너랑 얘기하면 괜히 기분이 좋아져." else "이렇게 이야기하면 괜히 기분이 좋아져요.",
            )
        }

        if (tone == ToneStyle.WARM && speechMode == SpeechMode.CASUAL) {
            result = result.replace("좋다.", "좋다.").replace("괜찮아.", "괜찮아.")
        }

        if (ReplyConstraint.MINIMIZE_CRINGE in constraints || StyleAdjustment.MORE_NATURAL in adjustments) {
            result = result
                .replace("괜히 계속 생각나네.", "은근 기억에 남네.")
                .replace("이 대화 흐름이 은근 설렌다.", "이 대화 흐름이 은근 좋다.")
                .replace("마음이 좀 말랑해진다.", "기분이 좀 부드러워진다.")
        }

        if (ReplyConstraint.NO_HEAVY_FLIRTING in constraints) {
            result = result
                .replace("설렌다", "기분 좋다")
                .replace("계속 생각나네", "기억에 남네")
                .replace("좋아져.", "괜찮네.")
        }

        if (ReplyConstraint.NO_LONG_REPLY in constraints || StyleAdjustment.SHORTER in adjustments) {
            result = shorten(result)
        }

        if (ReplyConstraint.NO_EMOJI in constraints) {
            result = result.replace("ㅎㅎ", "").trim()
        }

        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun shorten(text: String): String {
        val firstSentence = text.split(Regex("(?<=[.?!])\\s+")).firstOrNull().orEmpty()
        return if (firstSentence.length <= 34) firstSentence else firstSentence.take(34).trimEnd() + "..."
    }

    private fun containsAny(source: String, vararg keywords: String): Boolean {
        return keywords.any { source.contains(it, ignoreCase = true) }
    }

    private fun line(mode: SpeechMode, casual: String, formal: String): String {
        return if (mode == SpeechMode.FORMAL) formal else casual
    }

    private fun compose(vararg parts: String): String {
        return parts.map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun <T> pick(seed: Int, values: List<T>): T {
        return values[abs(seed) % values.size]
    }
}
