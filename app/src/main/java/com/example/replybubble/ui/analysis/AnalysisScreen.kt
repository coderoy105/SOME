package com.example.replybubble.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.overlay.ScreenCapturePermissionActivity
import com.example.replybubble.ui.common.EmptyStateCard
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    onSessionCreated: (Long) -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sessionCreatedEvents.collect { sessionId ->
            onSessionCreated(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.analysis_title)) },
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
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SectionCard(
                        title = stringResource(R.string.analysis_start_title),
                        subtitle = stringResource(R.string.analysis_start_body),
                    ) {
                        Button(
                            onClick = {
                                if (uiState.selectedProfileId == null) {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            if (uiState.profiles.isEmpty()) {
                                                R.string.overlay_profile_missing
                                            } else {
                                                R.string.overlay_profile_required
                                            },
                                        ),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@Button
                                }
                                context.startActivity(
                                    ScreenCapturePermissionActivity.createIntent(
                                        context = context,
                                        contactId = uiState.selectedProfileId,
                                        source = AnalysisSource.APP,
                                        delayMs = 3500L,
                                        openAfterResult = true,
                                    ),
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.analysis_capture_toast),
                                    Toast.LENGTH_LONG,
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.analysis_start_button))
                        }
                        TextButton(
                            onClick = viewModel::createDemoSession,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.analysis_demo_button))
                        }
                        Text(
                            text = uiState.overlayState.lastStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    SectionCard(
                        title = stringResource(R.string.analysis_profile_selector_title),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.profiles.firstOrNull { it.id == uiState.selectedProfileId }?.name
                                    ?: stringResource(R.string.analysis_profile_selector_empty),
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = true },
                                readOnly = true,
                                label = { Text(text = stringResource(R.string.analysis_profile_selector_label)) },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                uiState.profiles.forEach { profile ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(text = profile.name) },
                                        onClick = {
                                            viewModel.selectProfile(profile.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val latestSession = uiState.latestSession
                    if (latestSession == null) {
                        EmptyStateCard(
                            title = stringResource(R.string.analysis_empty_title),
                            description = stringResource(R.string.analysis_empty_body),
                        )
                    } else {
                        SectionCard(
                            title = stringResource(R.string.analysis_latest_preview_title),
                            subtitle = latestSession.contact?.name ?: stringResource(R.string.analysis_profile_selector_empty),
                        ) {
                            Text(
                                text = latestSession.session.lastMessage.ifBlank {
                                    stringResource(R.string.analysis_preview_fallback)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = latestSession.session.cleanedOcrText.ifBlank {
                                    stringResource(R.string.analysis_preview_no_ocr)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${stringResource(R.string.analysis_latest_vibe)} ${context.getString(latestSession.session.vibe.labelRes())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
