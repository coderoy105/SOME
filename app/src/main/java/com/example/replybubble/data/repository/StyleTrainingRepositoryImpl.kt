package com.example.replybubble.data.repository

import com.example.replybubble.data.local.dao.StyleTrainingSampleDao
import com.example.replybubble.data.local.entity.StyleTrainingSampleEntity
import com.example.replybubble.domain.model.StyleTrainingSample
import com.example.replybubble.domain.repository.StyleTrainingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StyleTrainingRepositoryImpl @Inject constructor(
    private val styleTrainingSampleDao: StyleTrainingSampleDao,
) : StyleTrainingRepository {
    override fun observeSamples(): Flow<List<StyleTrainingSample>> {
        return styleTrainingSampleDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSamples(): List<StyleTrainingSample> {
        return styleTrainingSampleDao.getAll().map { it.toDomain() }
    }

    override suspend fun saveSample(
        promptId: Int,
        prompt: String,
        answer: String,
    ): Long {
        val existing = styleTrainingSampleDao.getByPromptId(promptId)
        val now = System.currentTimeMillis()
        return styleTrainingSampleDao.upsert(
            StyleTrainingSampleEntity(
                id = existing?.id ?: 0,
                promptId = promptId,
                prompt = prompt.trim(),
                answer = answer.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteSample(sampleId: Long) {
        styleTrainingSampleDao.deleteById(sampleId)
    }

    override suspend fun clearSamples() {
        styleTrainingSampleDao.clearAll()
    }
}
