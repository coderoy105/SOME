package com.example.replybubble.domain.repository

import com.example.replybubble.domain.model.StyleTrainingSample
import kotlinx.coroutines.flow.Flow

interface StyleTrainingRepository {
    fun observeSamples(): Flow<List<StyleTrainingSample>>
    suspend fun getSamples(): List<StyleTrainingSample>
    suspend fun saveSample(
        promptId: Int,
        prompt: String,
        answer: String,
    ): Long

    suspend fun deleteSample(sampleId: Long)
    suspend fun clearSamples()
}
