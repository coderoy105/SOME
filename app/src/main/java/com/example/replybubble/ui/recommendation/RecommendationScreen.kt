package com.example.replybubble.ui.recommendation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.domain.model.StyleAdjustment
import com.example.replybubble.ui.common.EmptyStateCard
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.ReplySuggestionCard
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.common.ToggleChipGroup
import com.example.replybubble.ui.common.labelRes
import com.example.replybubble.util.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    onBack: () -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.recommendation_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.regenerate(emptySet()) }) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        GradientScreenContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val detail = uiState.sessionDetail
            if (detail == null) {
                EmptyStateCard(
                    title = stringResource(R.string.recommendation_empty_title),
                    description = stringResource(R.string.recommendation_empty_body),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        SectionCard(
                            title = detail.contact?.name ?: stringResource(R.string.recommendation_default_profile),
                            subtitle = DateTimeFormatter.formatSessionTime(detail.session.createdAt),
                        ) {
                            Text(
                                text = detail.session.lastMessage.ifBlank {
                                    stringResource(R.string.analysis_preview_fallback)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = detail.session.cleanedOcrText.ifBlank {
                                    stringResource(R.string.analysis_preview_no_ocr)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        SectionCard(
                            title = stringResource(R.string.recommendation_adjustment_title),
                            subtitle = stringResource(R.string.recommendation_adjustment_body),
                            trailing = {
                                if (uiState.isRegenerating) {
                                    Text(
                                        text = stringResource(R.string.recommendation_regenerating),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        ) {
                            ToggleChipGroup(
                                values = StyleAdjustment.entries.toList(),
                                selectedValues = emptySet(),
                                label = { adjustment -> context.getString(adjustment.labelRes()) },
                                onToggle = { adjustment -> viewModel.regenerate(setOf(adjustment)) },
                            )
                            FilledTonalButton(
                                onClick = { viewModel.regenerate(emptySet()) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(R.string.recommendation_regenerate_button))
                            }
                        }
                    }
                    items(detail.suggestions, key = { it.id }) { suggestion ->
                        ReplySuggestionCard(
                            category = context.getString(suggestion.category.labelRes()),
                            suggestion = suggestion,
                            isRefreshing = suggestion.id in uiState.refreshingReplyIds,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(suggestion.content))
                                viewModel.markCopied(suggestion.id)
                                Toast.makeText(context, context.getString(R.string.recommendation_copy_done), Toast.LENGTH_SHORT).show()
                            },
                            onRefresh = { viewModel.regenerateSuggestion(suggestion.id) },
                        )
                    }
                }
            }
        }
    }
}
