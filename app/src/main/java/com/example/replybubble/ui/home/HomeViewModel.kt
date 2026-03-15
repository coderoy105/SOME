package com.example.replybubble.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.SessionPreview
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.domain.repository.SettingsRepository
import com.example.replybubble.overlay.OverlayRuntimeState
import com.example.replybubble.overlay.OverlayUiState
import com.example.replybubble.update.AppUpdateChecker
import com.example.replybubble.update.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val profiles: List<ContactProfile> = emptyList(),
    val recentSessions: List<SessionPreview> = emptyList(),
    val overlayState: OverlayUiState = OverlayUiState(),
    val autoSaveHistory: Boolean = true,
    val updateInfo: AppUpdateInfo? = null,
    val updateCheckEnabled: Boolean = false,
    val checkingForUpdate: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    private val appUpdateChecker: AppUpdateChecker,
) : ViewModel() {
    private val updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    private val checkingForUpdate = MutableStateFlow(false)

    private val baseUiState = combine(
        profileRepository.observeProfiles(),
        sessionRepository.observeRecentSessions(),
        settingsRepository.observeSettings(),
        OverlayRuntimeState.state,
    ) { profiles, sessions, settings, overlayState ->
        HomeUiState(
            profiles = profiles,
            recentSessions = sessions.take(10),
            overlayState = overlayState,
            autoSaveHistory = settings.autoSaveHistory,
            updateCheckEnabled = appUpdateChecker.isConfigured(),
        )
    }

    val uiState = combine(
        baseUiState,
        updateInfo,
        checkingForUpdate,
    ) { baseState, availableUpdate, isChecking ->
        baseState.copy(
            updateInfo = availableUpdate,
            checkingForUpdate = isChecking,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private val sessionRepositoryRef = sessionRepository
    private val profileRepositoryRef = profileRepository

    init {
        refreshUpdate()
    }

    fun refreshUpdate() {
        if (!appUpdateChecker.isConfigured()) return
        viewModelScope.launch {
            checkingForUpdate.value = true
            updateInfo.value = appUpdateChecker.checkForUpdate()
            checkingForUpdate.value = false
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepositoryRef.deleteSession(sessionId)
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepositoryRef.deleteProfile(profileId)
        }
    }
}
