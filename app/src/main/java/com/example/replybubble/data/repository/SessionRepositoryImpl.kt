package com.example.replybubble.data.repository

import com.example.replybubble.data.local.dao.AnalysisSessionDao
import com.example.replybubble.data.local.dao.ContactProfileDao
import com.example.replybubble.data.local.dao.ReplySuggestionDao
import com.example.replybubble.data.local.entity.AnalysisSessionEntity
import com.example.replybubble.data.local.entity.ReplySuggestionEntity
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.model.ConversationContext
import com.example.replybubble.domain.model.ConversationVibe
import com.example.replybubble.domain.model.EmotionalTone
import com.example.replybubble.domain.model.NormalizedOcrResult
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.SessionDetail
import com.example.replybubble.domain.model.SessionPreview
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.domain.repository.SettingsRepository
import com.example.replybubble.domain.repository.StyleTrainingRepository
import com.example.replybubble.ocr.OcrTextNormalizer
import com.example.replybubble.recommendation.ConversationAnalyzer
import com.example.replybubble.recommendation.GeneratedReply
import com.example.replybubble.recommendation.RecommendationEngine
import com.example.replybubble.recommendation.RecommendationRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val analysisSessionDao: AnalysisSessionDao,
    private val replySuggestionDao: ReplySuggestionDao,
    private val contactProfileDao: ContactProfileDao,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val styleTrainingRepository: StyleTrainingRepository,
    private val ocrTextNormalizer: OcrTextNormalizer,
    private val conversationAnalyzer: ConversationAnalyzer,
    private val recommendationEngine: RecommendationEngine,
) : SessionRepository {
    override fun observeRecentSessions(): Flow<List<SessionPreview>> {
        return combine(
            analysisSessionDao.observeRecent(),
            contactProfileDao.observeAll(),
        ) { sessions, profiles ->
            val profileMap = profiles.associateBy { it.id }
            sessions.map { session ->
                SessionPreview(
                    id = session.id,
                    contactName = profileMap[session.contactId]?.name ?: "프로필 미선택",
                    lastMessage = session.lastMessage.ifBlank { "대화 텍스트가 부족해서 기본 추천을 만들었어요." },
                    questionDetected = session.questionDetected,
                    vibe = enumOrDefault(session.vibe, ConversationVibe.NEUTRAL),
                    createdAt = session.createdAt,
                    source = enumOrDefault(session.source, AnalysisSource.APP),
                )
            }
        }
    }

    override fun observeLatestSession(): Flow<SessionPreview?> {
        return combine(
            analysisSessionDao.observeLatest(),
            contactProfileDao.observeAll(),
        ) { session, profiles ->
            session?.let {
                val profileMap = profiles.associateBy { entity -> entity.id }
                SessionPreview(
                    id = it.id,
                    contactName = profileMap[it.contactId]?.name ?: "프로필 미선택",
                    lastMessage = it.lastMessage.ifBlank { "기본 추천 세션" },
                    questionDetected = it.questionDetected,
                    vibe = enumOrDefault(it.vibe, ConversationVibe.NEUTRAL),
                    createdAt = it.createdAt,
                    source = enumOrDefault(it.source, AnalysisSource.APP),
                )
            }
        }
    }

    override fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> {
        return combine(
            analysisSessionDao.observeById(sessionId),
            replySuggestionDao.observeBySessionId(sessionId),
            contactProfileDao.observeAll(),
        ) { session, replies, profiles ->
            session?.let {
                val profile = profiles.firstOrNull { entity -> entity.id == it.contactId }?.toDomain()
                SessionDetail(
                    session = it.toDomain(),
                    contact = profile,
                    suggestions = replies.map { reply -> reply.toDomain() },
                )
            }
        }
    }

    override suspend fun getSessionDetail(sessionId: Long): SessionDetail? {
        val session = analysisSessionDao.getById(sessionId) ?: return null
        val replies = replySuggestionDao.getBySessionId(sessionId)
        val profile = session.contactId?.let { contactProfileDao.getById(it)?.toDomain() }
        return SessionDetail(
            session = session.toDomain(),
            contact = profile,
            suggestions = replies.map { it.toDomain() },
        )
    }

    override suspend fun processCapturedText(
        contactId: Long?,
        rawText: String,
        source: AnalysisSource,
        adjustments: Set<StyleAdjustment>,
    ): Long {
        val settings = settingsRepository.getSettings()
        val profile = contactId?.let { profileRepository.getProfile(it) }
        val styleSamples = styleTrainingRepository.getSamples()
        val normalized = ocrTextNormalizer.normalize(rawText)
        val context = conversationAnalyzer.analyze(normalized)
        val replies = recommendationEngine.generate(
            RecommendationRequest(
                profile = profile,
                settings = settings,
                normalized = normalized,
                context = context,
                adjustments = adjustments,
                styleSamples = styleSamples,
                generationNonce = System.currentTimeMillis(),
            ),
        )
        if (!settings.autoSaveHistory) {
            clearHistory()
        }
        return persistSession(
            contactId = contactId,
            source = source,
            normalized = normalized,
            context = context,
            replies = replies,
            wasFallback = normalized.isLowConfidence,
        )
    }

    override suspend fun createDemoSession(contactId: Long?): Long {
        val demoTranscript = listOf(
            "상대: 오늘 뭐 하고 있어?",
            "나: 방금 카페 나왔어. 너는?",
            "상대: 난 지금 퇴근 중인데 오늘 좀 피곤하다",
            "상대: 너라면 뭐라고 답할 것 같아?",
        ).joinToString("\n")
        return processCapturedText(
            contactId = contactId,
            rawText = demoTranscript,
            source = AnalysisSource.DEMO,
        )
    }

    override suspend fun regenerateSuggestions(sessionId: Long, adjustments: Set<StyleAdjustment>) {
        val session = analysisSessionDao.getById(sessionId) ?: return
        val profile = session.contactId?.let { profileRepository.getProfile(it) }
        val settings = settingsRepository.getSettings()
        val styleSamples = styleTrainingRepository.getSamples()
        val existingSuggestions = replySuggestionDao.getBySessionId(sessionId)
        val normalized = buildNormalizedResult(session)
        val context = buildConversationContext(session, normalized)
        val regenerated = recommendationEngine.generate(
            RecommendationRequest(
                profile = profile,
                settings = settings,
                normalized = normalized,
                context = context,
                adjustments = adjustments,
                styleSamples = styleSamples,
                excludedContentByCategory = existingSuggestions.associate { entity ->
                    enumOrDefault(entity.category, ReplyCategory.SAFE) to entity.content
                },
                generationNonce = System.currentTimeMillis(),
            ),
        )
        replySuggestionDao.deleteBySessionId(sessionId)
        replySuggestionDao.insertAll(
            regenerated.map { reply ->
                ReplySuggestionEntity(
                    sessionId = sessionId,
                    category = reply.category.name,
                    content = reply.content,
                    copiedCount = 0,
                    createdAt = System.currentTimeMillis(),
                )
            },
        )
    }

    override suspend fun regenerateSuggestion(replyId: Long, adjustments: Set<StyleAdjustment>) {
        val currentReply = replySuggestionDao.getById(replyId) ?: return
        val session = analysisSessionDao.getById(currentReply.sessionId) ?: return
        val profile = session.contactId?.let { profileRepository.getProfile(it) }
        val settings = settingsRepository.getSettings()
        val styleSamples = styleTrainingRepository.getSamples()
        val normalized = buildNormalizedResult(session)
        val context = buildConversationContext(session, normalized)
        val targetCategory = enumOrDefault(currentReply.category, ReplyCategory.SAFE)

        val regeneratedReplies = recommendationEngine.generate(
            RecommendationRequest(
                profile = profile,
                settings = settings,
                normalized = normalized,
                context = context,
                adjustments = adjustments,
                styleSamples = styleSamples,
                targetCategory = targetCategory,
                excludedContentByCategory = mapOf(targetCategory to currentReply.content),
                generationNonce = System.currentTimeMillis(),
            ),
        )

        val replacement = regeneratedReplies.firstOrNull { it.category == targetCategory }
            ?: return

        replySuggestionDao.updateReply(
            replyId = replyId,
            content = replacement.content,
            copiedCount = 0,
            createdAt = System.currentTimeMillis(),
        )
    }

    override suspend fun incrementCopyCount(replyId: Long) {
        replySuggestionDao.incrementCopyCount(replyId)
    }

    override suspend fun deleteSession(sessionId: Long) {
        analysisSessionDao.deleteById(sessionId)
    }

    override suspend fun clearHistory() {
        replySuggestionDao.clearAll()
        analysisSessionDao.clearAll()
    }

    private fun buildNormalizedResult(session: AnalysisSessionEntity): NormalizedOcrResult {
        return NormalizedOcrResult(
            rawText = session.rawOcrText,
            cleanedText = session.cleanedOcrText,
            recentMessages = session.recentMessages,
            lastMessage = session.lastMessage,
            lastQuestion = session.lastQuestion,
            questionDetected = session.questionDetected,
            isLowConfidence = session.wasFallback,
        )
    }

    private fun buildConversationContext(
        session: AnalysisSessionEntity,
        normalized: NormalizedOcrResult,
    ): ConversationContext {
        val analyzed = conversationAnalyzer.analyze(normalized)
        return ConversationContext(
            questionDetected = session.questionDetected,
            emotionalTone = enumOrDefault(session.emotionalTone, EmotionalTone.NEUTRAL),
            vibe = enumOrDefault(session.vibe, ConversationVibe.NEUTRAL),
            lastMessage = session.lastMessage,
            lastQuestion = session.lastQuestion,
            topicKeyword = analyzed.topicKeyword,
            shortMessageBias = analyzed.shortMessageBias,
        )
    }

    private suspend fun persistSession(
        contactId: Long?,
        source: AnalysisSource,
        normalized: NormalizedOcrResult,
        context: ConversationContext,
        replies: List<GeneratedReply>,
        wasFallback: Boolean,
    ): Long {
        val now = System.currentTimeMillis()
        val sessionId = analysisSessionDao.insert(
            AnalysisSessionEntity(
                contactId = contactId,
                source = source.name,
                rawOcrText = normalized.rawText,
                cleanedOcrText = normalized.cleanedText,
                recentMessages = normalized.recentMessages,
                lastMessage = normalized.lastMessage,
                lastQuestion = normalized.lastQuestion,
                questionDetected = normalized.questionDetected,
                emotionalTone = context.emotionalTone.name,
                vibe = context.vibe.name,
                wasFallback = wasFallback,
                createdAt = now,
            ),
        )
        val safeReplies = if (replies.isEmpty()) {
            listOf(
                GeneratedReply(ReplyCategory.SAFE, "방금 확인했어. 어떻게 답하면 좋을지 같이 맞춰볼까?"),
                GeneratedReply(ReplyCategory.WITTY, "이 대화 흐름이면 살짝 센스 있게 받아도 괜찮아 보여."),
                GeneratedReply(ReplyCategory.SWEET, "먼저 연락해줘서 괜히 반갑다."),
                GeneratedReply(ReplyCategory.SHORT, "응, 방금 봤어. 더 말해줘."),
                GeneratedReply(ReplyCategory.FOLLOW_UP, "지금 상황이 어떻게 된 건지 조금만 더 알려줘."),
            )
        } else {
            replies
        }
        replySuggestionDao.insertAll(
            safeReplies.map { reply ->
                ReplySuggestionEntity(
                    sessionId = sessionId,
                    category = reply.category.name,
                    content = reply.content,
                    copiedCount = 0,
                    createdAt = now,
                )
            },
        )
        return sessionId
    }
}
