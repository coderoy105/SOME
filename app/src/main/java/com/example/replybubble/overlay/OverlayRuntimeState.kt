package com.example.replybubble.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlayUiState(
    val isRunning: Boolean = false,
    val lastStatus: String = "오버레이가 준비되지 않았어요.",
    val lastSessionId: Long? = null,
)

object OverlayRuntimeState {
    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state.asStateFlow()

    fun updateRunning(isRunning: Boolean) {
        _state.value = _state.value.copy(isRunning = isRunning)
    }

    fun updateStatus(status: String, sessionId: Long? = _state.value.lastSessionId) {
        _state.value = _state.value.copy(lastStatus = status, lastSessionId = sessionId)
    }
}
