package com.example.replybubble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppEntryUiState(
    val loading: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = settingsRepository.observeSettings()
        .map { settings ->
            AppEntryUiState(
                loading = false,
                onboardingCompleted = settings.onboardingCompleted,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppEntryUiState(),
        )
}
