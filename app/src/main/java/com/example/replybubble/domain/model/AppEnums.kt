package com.example.replybubble.domain.model

enum class RelationshipType {
    FRIEND,
    CRUSH,
    PARTNER,
    INTEREST,
    SENIOR_JUNIOR,
    STRANGER,
}

enum class ToneStyle {
    CUTE,
    WITTY,
    WARM,
    FLIRTY,
    CASUAL,
    SERIOUS,
}

enum class ReplyConstraint {
    NO_HEAVY_FLIRTING,
    NO_LONG_REPLY,
    NO_EMOJI,
    FORCE_CASUAL,
    FORCE_FORMAL,
    MINIMIZE_CRINGE,
}

enum class ReplyCategory {
    SAFE,
    WITTY,
    SWEET,
    SHORT,
    FOLLOW_UP,
}

enum class ConversationVibe {
    LIGHT,
    PLAYFUL,
    AFFECTIONATE,
    NEUTRAL,
    AWKWARD,
}

enum class EmotionalTone {
    POSITIVE,
    CURIOUS,
    TIRED,
    COLD,
    NEUTRAL,
}

enum class AnalysisSource {
    APP,
    OVERLAY,
    DEMO,
}

enum class StyleAdjustment {
    MORE_CUTE,
    SHORTER,
    MORE_NATURAL,
    MORE_ROMANTIC,
    MORE_CASUAL,
}
