package com.example.replybubble.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.BuildConfig
import com.example.replybubble.R
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ToneStyle
import com.example.replybubble.ui.common.ChoiceChipGroup
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStyleTrainingClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                title = stringResource(R.string.settings_default_relationship),
            ) {
                ChoiceChipGroup(
                    values = RelationshipType.entries.toList(),
                    selectedValue = settings.defaultRelationshipType,
                    label = { type -> stringResource(type.labelRes()) },
                    onSelected = viewModel::updateRelationship,
                )
            }
            SectionCard(
                title = stringResource(R.string.settings_default_tone),
            ) {
                ChoiceChipGroup(
                    values = ToneStyle.entries.toList(),
                    selectedValue = settings.defaultToneStyle,
                    label = { style -> stringResource(style.labelRes()) },
                    onSelected = viewModel::updateTone,
                )
            }
            SectionCard(
                title = stringResource(R.string.settings_history_title),
                subtitle = stringResource(R.string.settings_history_body),
                trailing = {
                    Switch(
                        checked = settings.autoSaveHistory,
                        onCheckedChange = viewModel::updateAutoSave,
                    )
                },
            ) {}
            SectionCard(
                title = stringResource(R.string.settings_style_limits_title),
                subtitle = stringResource(R.string.settings_style_limits_body),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_limit_punctuation),
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Switch(
                        checked = settings.limitPunctuationOveruse,
                        onCheckedChange = viewModel::updateLimitPunctuation,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_limit_laugh),
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Switch(
                        checked = settings.limitLaughOveruse,
                        onCheckedChange = viewModel::updateLimitLaugh,
                    )
                }
            }
            SectionCard(
                title = stringResource(R.string.settings_overlay_scale_title),
                subtitle = stringResource(R.string.settings_overlay_scale_body),
            ) {
                OverlayScalePreview(scale = settings.overlayScale)
                Text(
                    text = stringResource(
                        R.string.settings_overlay_scale_value,
                        (settings.overlayScale * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = settings.overlayScale,
                    onValueChange = viewModel::updateOverlayScale,
                    valueRange = 0.8f..1.2f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            SectionCard(
                title = stringResource(R.string.style_training_settings_title),
                subtitle = stringResource(R.string.style_training_settings_body),
            ) {
                Button(
                    onClick = onStyleTrainingClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.style_training_settings_button))
                }
            }
            SectionCard(
                title = stringResource(R.string.settings_guide_overlay_title),
                subtitle = stringResource(R.string.settings_guide_overlay_body),
            ) {}
            SectionCard(
                title = stringResource(R.string.settings_guide_capture_title),
                subtitle = stringResource(R.string.settings_guide_capture_body),
            ) {}
            SectionCard(
                title = stringResource(R.string.settings_data_actions_title),
                subtitle = stringResource(R.string.settings_data_actions_body),
            ) {
                Button(
                    onClick = viewModel::clearHistory,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.settings_clear_history))
                }
                Button(
                    onClick = viewModel::clearAllData,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.settings_clear_all))
                }
            }
            SectionCard(
                title = stringResource(R.string.settings_app_info_title),
            ) {
                Text(
                    text = stringResource(R.string.settings_app_info_body, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OverlayScalePreview(scale: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                ),
            )
            .padding(16.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ((1.2f - scale) * 42f).dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.overlay_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(
                    text = stringResource(R.string.overlay_status_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text(
                    text = stringResource(R.string.analysis_profile_selector_title),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.relationship_friend),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PreviewButton(
                    text = stringResource(R.string.overlay_profile_button),
                    accent = true,
                )
                PreviewButton(text = stringResource(R.string.overlay_action_analyze))
                PreviewButton(text = stringResource(R.string.overlay_action_correct_input))
                PreviewButton(text = stringResource(R.string.overlay_action_home))
            }
        }
    }
}

@Composable
private fun PreviewButton(
    text: String,
    accent: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (accent) 2.dp else 0.dp,
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (accent) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        )
                    },
                )
                .padding(vertical = 11.dp, horizontal = 12.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
