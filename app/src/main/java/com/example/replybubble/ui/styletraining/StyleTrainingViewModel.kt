package com.example.replybubble.ui.styletraining

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.replybubble.domain.model.StyleTrainingSample
import com.example.replybubble.domain.repository.StyleTrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StyleTrainingUiState(
    val samples: List<StyleTrainingSample> = emptyList(),
    val answers: Map<Int, String> = emptyMap(),
)

@HiltViewModel
class StyleTrainingViewModel @Inject constructor(
    private val styleTrainingRepository: StyleTrainingRepository,
) : ViewModel() {
    private val draftAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())

    val uiState = combine(
        styleTrainingRepository.observeSamples(),
        draftAnswers,
    ) { samples, drafts ->
        val mergedAnswers = buildMap {
            samples.forEach { sample -> put(sample.promptId, sample.answer) }
            drafts.forEach { (promptId, answer) -> put(promptId, answer) }
        }
        StyleTrainingUiState(
            samples = samples,
            answers = mergedAnswers,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StyleTrainingUiState(),
        )

    fun updateAnswer(
        promptId: Int,
        answer: String,
    ) {
        draftAnswers.value = draftAnswers.value.toMutableMap().apply {
            put(promptId, answer)
        }
    }

    fun saveSample(
        promptId: Int,
        prompt: String,
        answer: String,
    ) {
        viewModelScope.launch {
            if (answer.isBlank()) return@launch
            styleTrainingRepository.saveSample(
                promptId = promptId,
                prompt = prompt,
                answer = answer,
            )
        }
    }

    fun saveAll(prompts: List<Pair<Int, String>>) {
        viewModelScope.launch {
            prompts.forEach { (promptId, prompt) ->
                val answer = draftAnswers.value[promptId]
                    ?: uiState.value.answers[promptId]
                    ?: ""
                if (answer.isBlank()) return@forEach
                styleTrainingRepository.saveSample(
                    promptId = promptId,
                    prompt = prompt,
                    answer = answer,
                )
            }
        }
    }

    fun deleteSample(sampleId: Long) {
        viewModelScope.launch {
            styleTrainingRepository.deleteSample(sampleId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            styleTrainingRepository.clearSamples()
            draftAnswers.value = emptyMap()
        }
    }
}
