package com.example.replybubble.ui.styletraining

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard

private data class TrainingPromptDefinition(
    val id: Int,
    val titleRes: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleTrainingScreen(
    onBack: () -> Unit,
    viewModel: StyleTrainingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedToast = stringResource(R.string.style_training_saved)
    val clearedToast = stringResource(R.string.style_training_cleared)
    val learnedToast = stringResource(R.string.style_training_learned)
    val prompts = listOf(
        TrainingPromptDefinition(1, R.string.style_training_prompt_1),
        TrainingPromptDefinition(2, R.string.style_training_prompt_2),
        TrainingPromptDefinition(3, R.string.style_training_prompt_3),
        TrainingPromptDefinition(4, R.string.style_training_prompt_4),
        TrainingPromptDefinition(5, R.string.style_training_prompt_5),
        TrainingPromptDefinition(6, R.string.style_training_prompt_6),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.style_training_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        GradientScreenContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            SectionCard(
                title = stringResource(R.string.style_training_intro_title),
                subtitle = stringResource(R.string.style_training_intro_body),
            ) {
                Text(text = stringResource(R.string.style_training_intro_tip))
                Button(
                    onClick = {
                        viewModel.saveAll(
                            prompts.map { prompt ->
                                prompt.id to context.getString(prompt.titleRes)
                            },
                        )
                        Toast.makeText(context, learnedToast, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.style_training_learn_button))
                }
            }

            SectionCard(
                title = stringResource(R.string.style_training_saved_title),
                subtitle = stringResource(R.string.style_training_saved_body, uiState.samples.size),
            ) {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        Toast.makeText(context, clearedToast, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.style_training_clear_all))
                }
            }

            prompts.forEach { prompt ->
                val sample = uiState.samples.firstOrNull { it.promptId == prompt.id }
                val promptText = stringResource(prompt.titleRes)
                val answer = uiState.answers[prompt.id].orEmpty()

                SectionCard(
                    title = stringResource(R.string.style_training_question_title, prompt.id),
                    subtitle = promptText,
                ) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { viewModel.updateAnswer(prompt.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text(text = stringResource(R.string.style_training_answer_label)) },
                        placeholder = { Text(text = stringResource(R.string.style_training_answer_placeholder)) },
                    )
                    Button(
                        onClick = {
                            viewModel.saveSample(
                                promptId = prompt.id,
                                prompt = promptText,
                                answer = answer,
                            )
                            Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.style_training_save))
                    }
                    if (sample != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteSample(sample.id)
                                viewModel.updateAnswer(prompt.id, "")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.style_training_delete))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
