package com.example.replybubble.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.SessionDetail
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.overlay.OverlayRuntimeState
import com.example.replybubble.overlay.OverlayUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val profiles: List<ContactProfile> = emptyList(),
    val selectedProfileId: Long? = null,
    val latestSession: SessionDetail? = null,
    val overlayState: OverlayUiState = OverlayUiState(),
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val selectedProfileIdFlow = MutableSharedFlow<Long?>(replay = 1)
    private val demoSessionEvents = MutableSharedFlow<Long>()

    init {
        selectedProfileIdFlow.tryEmit(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        profileRepository.observeProfiles(),
        sessionRepository.observeLatestSession().flatMapLatest { latest ->
            latest?.let { sessionRepository.observeSessionDetail(it.id) } ?: flowOf(null)
        },
        selectedProfileIdFlow,
        OverlayRuntimeState.state,
    ) { profiles, latestSession, selectedProfileId, overlayState ->
        AnalysisUiState(
            profiles = profiles,
            selectedProfileId = selectedProfileId,
            latestSession = latestSession,
            overlayState = overlayState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(),
    )

    val sessionCreatedEvents = demoSessionEvents

    fun selectProfile(profileId: Long?) {
        selectedProfileIdFlow.tryEmit(profileId)
    }

    fun createDemoSession() {
        viewModelScope.launch {
            val sessionId = sessionRepository.createDemoSession(uiState.value.selectedProfileId)
            demoSessionEvents.emit(sessionId)
        }
    }
}
