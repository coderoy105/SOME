package com.example.replybubble.ui.recommendation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.SessionDetail
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecommendationUiState(
    val isRegenerating: Boolean = false,
    val refreshingReplyIds: Set<Long> = emptySet(),
    val sessionDetail: SessionDetail? = null,
)

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val sessionId = checkNotNull(savedStateHandle.get<Long>("sessionId"))
    private val isRegenerating = MutableStateFlow(false)
    private val refreshingReplyIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState = combine(
        sessionRepository.observeSessionDetail(sessionId),
        isRegenerating,
        refreshingReplyIds,
    ) { detail, regenerating, refreshingIds ->
        RecommendationUiState(
            isRegenerating = regenerating,
            refreshingReplyIds = refreshingIds,
            sessionDetail = detail,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecommendationUiState(),
    )

    fun regenerate(adjustments: Set<StyleAdjustment>) {
        viewModelScope.launch {
            isRegenerating.value = true
            sessionRepository.regenerateSuggestions(sessionId, adjustments)
            isRegenerating.value = false
        }
    }

    fun regenerateSuggestion(replyId: Long) {
        viewModelScope.launch {
            refreshingReplyIds.value = refreshingReplyIds.value + replyId
            sessionRepository.regenerateSuggestion(replyId)
            refreshingReplyIds.value = refreshingReplyIds.value - replyId
        }
    }

    fun markCopied(replyId: Long) {
        viewModelScope.launch {
            sessionRepository.incrementCopyCount(replyId)
        }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
            onDeleted()
        }
    }
}
