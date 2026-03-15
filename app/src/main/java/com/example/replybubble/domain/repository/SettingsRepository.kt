package com.example.replybubble.domain.repository

import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ToneStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateDefaultRelationshipType(type: RelationshipType)
    suspend fun updateDefaultToneStyle(style: ToneStyle)
    suspend fun updateAutoSaveHistory(enabled: Boolean)
    suspend fun updateLimitPunctuationOveruse(enabled: Boolean)
    suspend fun updateLimitLaughOveruse(enabled: Boolean)
    suspend fun updateOverlayScale(scale: Float)
    suspend fun resetSettings()
}
