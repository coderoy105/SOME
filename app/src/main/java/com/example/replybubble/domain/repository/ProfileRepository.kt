package com.example.replybubble.domain.repository

import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.ToneStyle
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<ContactProfile>>
    fun observeProfile(profileId: Long): Flow<ContactProfile?>
    suspend fun getProfile(profileId: Long): ContactProfile?
    suspend fun saveProfile(
        profileId: Long?,
        name: String,
        relationshipType: RelationshipType,
        toneStyle: ToneStyle,
        constraints: Set<ReplyConstraint>,
    ): Long

    suspend fun deleteProfile(profileId: Long)
}
