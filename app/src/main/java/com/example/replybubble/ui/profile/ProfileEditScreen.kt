package com.example.replybubble.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ReplyConstraint
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.ui.common.ChoiceChipGroup
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.common.ToggleChipGroup
import com.example.replybubble.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEditEvent.Saved -> onBack()
                ProfileEditEvent.Deleted -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) {
                            stringResource(R.string.profile_edit_title)
                        } else {
                            stringResource(R.string.profile_add_title)
                        },
                    )
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
                title = stringResource(R.string.profile_name_title),
                subtitle = stringResource(R.string.profile_name_body),
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.profile_name_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = uiState.validationMessage != null,
                    supportingText = {
                        if (uiState.validationMessage != null) {
                            Text(text = uiState.validationMessage ?: "")
                        }
                    },
                )
            }
            SectionCard(
                title = stringResource(R.string.profile_relationship_title),
            ) {
                ChoiceChipGroup(
                    values = RelationshipType.entries.toList(),
                    selectedValue = uiState.relationshipType,
                    label = { type -> context.getString(type.labelRes()) },
                    onSelected = viewModel::setRelationshipType,
                )
            }
            SectionCard(
                title = stringResource(R.string.profile_tone_title),
            ) {
                ChoiceChipGroup(
                    values = ToneStyle.entries.toList(),
                    selectedValue = uiState.toneStyle,
                    label = { style -> context.getString(style.labelRes()) },
                    onSelected = viewModel::setToneStyle,
                )
            }
            SectionCard(
                title = stringResource(R.string.profile_constraints_title),
                subtitle = stringResource(R.string.profile_constraints_body),
            ) {
                ToggleChipGroup(
                    values = ReplyConstraint.entries.toList(),
                    selectedValues = uiState.constraints,
                    label = { constraint -> context.getString(constraint.labelRes()) },
                    onToggle = viewModel::toggleConstraint,
                )
            }
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.profile_save_button))
            }
            if (uiState.isEditing) {
                TextButton(
                    onClick = {
                        viewModel.delete()
                        Toast.makeText(context, context.getString(R.string.profile_delete_done), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_delete_button),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
