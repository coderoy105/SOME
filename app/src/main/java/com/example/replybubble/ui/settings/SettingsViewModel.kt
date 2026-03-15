package com.example.replybubble.ui.settings

import com.example.replybubble.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.domain.repository.MaintenanceRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.domain.repository.SettingsRepository
import com.example.replybubble.update.AppUpdateChecker
import com.example.replybubble.update.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val appUpdateChecker: AppUpdateChecker,
) : ViewModel() {
    val settings = settingsRepository.observeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )
    val updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val checkingForUpdate = MutableStateFlow(false)
    val updateCheckEnabled: Boolean = appUpdateChecker.isConfigured()
    val updateSiteUrl: String = BuildConfig.UPDATE_SITE_URL.trim()

    init {
        refreshUpdate()
    }

    fun updateRelationship(type: RelationshipType) {
        viewModelScope.launch {
            settingsRepository.updateDefaultRelationshipType(type)
        }
    }

    fun updateTone(style: ToneStyle) {
        viewModelScope.launch {
            settingsRepository.updateDefaultToneStyle(style)
        }
    }

    fun updateAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSaveHistory(enabled)
        }
    }

    fun updateLimitPunctuation(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLimitPunctuationOveruse(enabled)
        }
    }

    fun updateLimitLaugh(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLimitLaughOveruse(enabled)
        }
    }

    fun updateOverlayScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.updateOverlayScale(scale)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            sessionRepository.clearHistory()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            maintenanceRepository.clearAllData()
        }
    }

    fun refreshUpdate() {
        if (!appUpdateChecker.isConfigured()) return
        viewModelScope.launch {
            checkingForUpdate.value = true
            updateInfo.value = appUpdateChecker.checkForUpdate()
            checkingForUpdate.value = false
        }
    }
}
