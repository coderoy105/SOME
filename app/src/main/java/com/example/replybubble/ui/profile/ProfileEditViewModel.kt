package com.example.replybubble.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val profileId: Long? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val relationshipType: RelationshipType = RelationshipType.FRIEND,
    val toneStyle: ToneStyle = ToneStyle.CASUAL,
    val constraints: Set<ReplyConstraint> = emptySet(),
    val validationMessage: String? = null,
)

sealed interface ProfileEditEvent {
    data object Saved : ProfileEditEvent
    data object Deleted : ProfileEditEvent
}

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEditEvent>()
    val events = _events.asSharedFlow()

    private val profileId = savedStateHandle.get<Long>("profileId")?.takeIf { it > 0L }

    init {
        viewModelScope.launch {
            if (profileId != null) {
                val profile = profileRepository.getProfile(profileId)
                _uiState.value = ProfileEditUiState(
                    profileId = profile?.id,
                    isLoading = false,
                    isEditing = true,
                    name = profile?.name.orEmpty(),
                    relationshipType = profile?.relationshipType ?: RelationshipType.FRIEND,
                    toneStyle = profile?.toneStyle ?: ToneStyle.CASUAL,
                    constraints = profile?.constraints ?: emptySet(),
                )
            } else {
                val settings = settingsRepository.observeSettings().first()
                _uiState.value = ProfileEditUiState(
                    isLoading = false,
                    relationshipType = settings.defaultRelationshipType,
                    toneStyle = settings.defaultToneStyle,
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, validationMessage = null)
    }

    fun setRelationshipType(type: RelationshipType) {
        _uiState.value = _uiState.value.copy(relationshipType = type)
    }

    fun setToneStyle(style: ToneStyle) {
        _uiState.value = _uiState.value.copy(toneStyle = style)
    }

    fun toggleConstraint(constraint: ReplyConstraint) {
        val current = _uiState.value.constraints.toMutableSet()
        if (!current.add(constraint)) {
            current.remove(constraint)
        }
        if (constraint == ReplyConstraint.FORCE_CASUAL) {
            current.remove(ReplyConstraint.FORCE_FORMAL)
        }
        if (constraint == ReplyConstraint.FORCE_FORMAL) {
            current.remove(ReplyConstraint.FORCE_CASUAL)
        }
        _uiState.value = _uiState.value.copy(constraints = current)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(validationMessage = "이름을 입력해 주세요.")
            return
        }
        viewModelScope.launch {
            profileRepository.saveProfile(
                profileId = state.profileId,
                name = state.name,
                relationshipType = state.relationshipType,
                toneStyle = state.toneStyle,
                constraints = state.constraints,
            )
            _events.emit(ProfileEditEvent.Saved)
        }
    }

    fun delete() {
        val targetId = _uiState.value.profileId ?: return
        viewModelScope.launch {
            profileRepository.deleteProfile(targetId)
            _events.emit(ProfileEditEvent.Deleted)
        }
    }
}
