package com.example.replybubble.domain.model

data class ContactProfile(
    val id: Long,
    val name: String,
    val relationshipType: RelationshipType,
    val toneStyle: ToneStyle,
    val constraints: Set<ReplyConstraint>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val defaultRelationshipType: RelationshipType = RelationshipType.FRIEND,
    val defaultToneStyle: ToneStyle = ToneStyle.CASUAL,
    val autoSaveHistory: Boolean = true,
    val limitPunctuationOveruse: Boolean = false,
    val limitLaughOveruse: Boolean = false,
    val overlayScale: Float = 1f,
)
