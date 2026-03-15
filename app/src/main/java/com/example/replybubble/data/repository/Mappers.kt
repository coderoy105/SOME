package com.example.replybubble.data.repository

import com.example.replybubble.data.local.entity.AnalysisSessionEntity
import com.example.replybubble.data.local.entity.ContactProfileEntity
import com.example.replybubble.data.local.entity.ReplySuggestionEntity
import com.example.replybubble.data.local.entity.StyleTrainingSampleEntity
import com.example.replybubble.domain.model.AnalysisSession
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.ConversationVibe
import com.example.replybubble.domain.model.EmotionalTone
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.ReplySuggestion
import com.example.replybubble.domain.model.StyleTrainingSample
import com.example.replybubble.domain.model.ToneStyle

internal fun ContactProfileEntity.toDomain(): ContactProfile {
    return ContactProfile(
        id = id,
        name = name,
        relationshipType = enumOrDefault(relationshipType, RelationshipType.FRIEND),
        toneStyle = enumOrDefault(toneStyle, ToneStyle.CASUAL),
        constraints = constraints.mapNotNull {
            runCatching { enumValueOf<ReplyConstraint>(it) }.getOrNull()
        }.toSet(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun AnalysisSessionEntity.toDomain(): AnalysisSession {
    return AnalysisSession(
        id = id,
        contactId = contactId,
        source = enumOrDefault(source, AnalysisSource.APP),
        rawOcrText = rawOcrText,
        cleanedOcrText = cleanedOcrText,
        recentMessages = recentMessages,
        lastMessage = lastMessage,
        lastQuestion = lastQuestion,
        questionDetected = questionDetected,
        emotionalTone = enumOrDefault(emotionalTone, EmotionalTone.NEUTRAL),
        vibe = enumOrDefault(vibe, ConversationVibe.NEUTRAL),
        wasFallback = wasFallback,
        createdAt = createdAt,
    )
}

internal fun ReplySuggestionEntity.toDomain(): ReplySuggestion {
    return ReplySuggestion(
        id = id,
        sessionId = sessionId,
        category = enumOrDefault(category, ReplyCategory.SAFE),
        content = content,
        copiedCount = copiedCount,
        createdAt = createdAt,
    )
}

internal fun StyleTrainingSampleEntity.toDomain(): StyleTrainingSample {
    return StyleTrainingSample(
        id = id,
        promptId = promptId,
        prompt = prompt,
        answer = answer,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T {
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}
