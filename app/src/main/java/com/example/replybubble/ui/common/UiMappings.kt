package com.example.replybubble.ui.common

import androidx.annotation.StringRes
import com.example.replybubble.R
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.model.ConversationVibe
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.model.ToneStyle

@StringRes
fun RelationshipType.labelRes(): Int = when (this) {
    RelationshipType.FRIEND -> R.string.relationship_friend
    RelationshipType.CRUSH -> R.string.relationship_crush
    RelationshipType.PARTNER -> R.string.relationship_partner
    RelationshipType.INTEREST -> R.string.relationship_interest
    RelationshipType.SENIOR_JUNIOR -> R.string.relationship_senior_junior
    RelationshipType.STRANGER -> R.string.relationship_stranger
}

@StringRes
fun ToneStyle.labelRes(): Int = when (this) {
    ToneStyle.CUTE -> R.string.tone_cute
    ToneStyle.WITTY -> R.string.tone_witty
    ToneStyle.WARM -> R.string.tone_warm
    ToneStyle.FLIRTY -> R.string.tone_flirty
    ToneStyle.CASUAL -> R.string.tone_casual
    ToneStyle.SERIOUS -> R.string.tone_serious
}

@StringRes
fun ReplyConstraint.labelRes(): Int = when (this) {
    ReplyConstraint.NO_HEAVY_FLIRTING -> R.string.constraint_no_heavy_flirting
    ReplyConstraint.NO_LONG_REPLY -> R.string.constraint_no_long_reply
    ReplyConstraint.NO_EMOJI -> R.string.constraint_no_emoji
    ReplyConstraint.FORCE_CASUAL -> R.string.constraint_force_casual
    ReplyConstraint.FORCE_FORMAL -> R.string.constraint_force_formal
    ReplyConstraint.MINIMIZE_CRINGE -> R.string.constraint_minimize_cringe
}

@StringRes
fun ReplyCategory.labelRes(): Int = when (this) {
    ReplyCategory.SAFE -> R.string.category_safe
    ReplyCategory.WITTY -> R.string.category_witty
    ReplyCategory.SWEET -> R.string.category_sweet
    ReplyCategory.SHORT -> R.string.category_short
    ReplyCategory.FOLLOW_UP -> R.string.category_follow_up
}

@StringRes
fun StyleAdjustment.labelRes(): Int = when (this) {
    StyleAdjustment.MORE_CUTE -> R.string.adjustment_more_cute
    StyleAdjustment.SHORTER -> R.string.adjustment_shorter
    StyleAdjustment.MORE_NATURAL -> R.string.adjustment_more_natural
    StyleAdjustment.MORE_ROMANTIC -> R.string.adjustment_more_romantic
    StyleAdjustment.MORE_CASUAL -> R.string.adjustment_more_casual
}

@StringRes
fun ConversationVibe.labelRes(): Int = when (this) {
    ConversationVibe.LIGHT -> R.string.vibe_light
    ConversationVibe.PLAYFUL -> R.string.vibe_playful
    ConversationVibe.AFFECTIONATE -> R.string.vibe_affectionate
    ConversationVibe.NEUTRAL -> R.string.vibe_neutral
    ConversationVibe.AWKWARD -> R.string.vibe_awkward
}

@StringRes
fun AnalysisSource.labelRes(): Int = when (this) {
    AnalysisSource.APP -> R.string.source_app
    AnalysisSource.OVERLAY -> R.string.source_overlay
    AnalysisSource.DEMO -> R.string.source_demo
}
