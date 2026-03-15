package com.example.replybubble.data.repository

import com.example.replybubble.data.local.dao.ContactProfileDao
import com.example.replybubble.data.local.entity.ContactProfileEntity
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val contactProfileDao: ContactProfileDao,
) : ProfileRepository {
    override fun observeProfiles(): Flow<List<ContactProfile>> {
        return contactProfileDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun observeProfile(profileId: Long): Flow<ContactProfile?> {
        return contactProfileDao.observeById(profileId).map { it?.toDomain() }
    }

    override suspend fun getProfile(profileId: Long): ContactProfile? {
        return contactProfileDao.getById(profileId)?.toDomain()
    }

    override suspend fun saveProfile(
        profileId: Long?,
        name: String,
        relationshipType: RelationshipType,
        toneStyle: ToneStyle,
        constraints: Set<ReplyConstraint>,
    ): Long {
        val existing = profileId?.let { contactProfileDao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = ContactProfileEntity(
            id = existing?.id ?: 0,
            name = name.trim(),
            relationshipType = relationshipType.name,
            toneStyle = toneStyle.name,
            constraints = constraints.map { it.name }.sorted(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return contactProfileDao.upsert(entity)
    }

    override suspend fun deleteProfile(profileId: Long) {
        contactProfileDao.deleteById(profileId)
    }
}
