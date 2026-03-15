package com.example.replybubble.data.repository

import com.example.replybubble.data.preferences.SettingsDataStore
import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = settingsDataStore.settingsFlow

    override suspend fun getSettings(): AppSettings = settingsDataStore.settingsFlow.first()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        settingsDataStore.update { it.onboardingCompleted = completed }
    }

    override suspend fun updateDefaultRelationshipType(type: RelationshipType) {
        settingsDataStore.update { it.defaultRelationshipType = type }
    }

    override suspend fun updateDefaultToneStyle(style: ToneStyle) {
        settingsDataStore.update { it.defaultToneStyle = style }
    }

    override suspend fun updateAutoSaveHistory(enabled: Boolean) {
        settingsDataStore.update { it.autoSaveHistory = enabled }
    }

    override suspend fun updateLimitPunctuationOveruse(enabled: Boolean) {
        settingsDataStore.update { it.limitPunctuationOveruse = enabled }
    }

    override suspend fun updateLimitLaughOveruse(enabled: Boolean) {
        settingsDataStore.update { it.limitLaughOveruse = enabled }
    }

    override suspend fun updateOverlayScale(scale: Float) {
        settingsDataStore.update { it.overlayScale = scale.coerceIn(0.8f, 1.2f) }
    }

    override suspend fun resetSettings() {
        settingsDataStore.reset()
    }
}
