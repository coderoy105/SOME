package com.example.replybubble.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.overlay.BatteryOptimizationHelper
import com.example.replybubble.overlay.OverlayBubbleService
import com.example.replybubble.overlay.OverlayPermissionHelper
import com.example.replybubble.ui.common.EmptyStateCard
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.ProfileCard
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.common.SessionCard
import com.example.replybubble.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddProfile: () -> Unit,
    onProfileClick: (Long) -> Unit,
    onAnalysisClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableUpdate = uiState.updateInfo
    val batteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    var pendingOverlayStart by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context, pendingOverlayStart, uiState.overlayState.isRunning) {
        val observer = LifecycleEventObserver { _, event ->
            val readyToStart = OverlayPermissionHelper.canDrawOverlays(context) &&
                BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            if (
                event == Lifecycle.Event.ON_RESUME &&
                pendingOverlayStart &&
                readyToStart &&
                !uiState.overlayState.isRunning
            ) {
                OverlayBubbleService.start(context)
                pendingOverlayStart = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProfile,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.home_add_profile)) },
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
                contentPadding = PaddingValues(bottom = 112.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SectionCard(
                        title = stringResource(R.string.home_quick_actions),
                        subtitle = uiState.overlayState.lastStatus,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                onClick = onAnalysisClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = stringResource(R.string.home_start_analysis))
                            }
                            FilledTonalButton(
                                onClick = {
                                    val canDrawOverlays = OverlayPermissionHelper.canDrawOverlays(context)
                                    val batteryIgnoredNow = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

                                    if (uiState.overlayState.isRunning) {
                                        pendingOverlayStart = false
                                        OverlayBubbleService.stop(context)
                                    } else if (!canDrawOverlays) {
                                        pendingOverlayStart = true
                                        OverlayPermissionHelper.openOverlayPermissionSettings(context)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.overlay_permission_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else if (!batteryIgnoredNow) {
                                        pendingOverlayStart = true
                                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.battery_optimization_toast),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } else {
                                        pendingOverlayStart = false
                                        OverlayBubbleService.start(context)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = if (uiState.overlayState.isRunning) {
                                        stringResource(R.string.home_stop_overlay)
                                    } else {
                                        stringResource(R.string.home_start_overlay)
                                    },
                                )
                            }
                        }
                        Text(
                            text = if (uiState.autoSaveHistory) {
                                stringResource(R.string.home_auto_save_on)
                            } else {
                                stringResource(R.string.home_auto_save_off)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!batteryIgnored || uiState.overlayState.lastStatus.contains("배터리")) {
                            TextButton(
                                onClick = {
                                    pendingOverlayStart = true
                                    if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                        BatteryOptimizationHelper.openAppInfoSettings(context)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.battery_app_info_toast),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } else {
                                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.battery_optimization_toast),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(R.string.open_battery_optimization_button))
                            }
                        }
                    }
                }

                if (uiState.updateCheckEnabled) {
                    item {
                        SectionCard(
                            title = if (availableUpdate != null) {
                                stringResource(R.string.update_available_title, availableUpdate.versionName)
                            } else {
                                stringResource(R.string.update_check_title)
                            },
                            subtitle = availableUpdate?.message ?: stringResource(R.string.update_check_body),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = viewModel::refreshUpdate,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = if (uiState.checkingForUpdate) {
                                            stringResource(R.string.update_checking)
                                        } else {
                                            stringResource(R.string.update_check_button)
                                        },
                                    )
                                }
                                if (availableUpdate != null) {
                                    FilledTonalButton(
                                        onClick = {
                                            val targetUrl = availableUpdate.pageUrl ?: availableUpdate.apkUrl
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(text = stringResource(R.string.update_download_button))
                                    }
                                }
                            }
                            if (availableUpdate != null) {
                                Text(
                                    text = stringResource(R.string.update_manual_install_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.home_profiles_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                if (uiState.profiles.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(R.string.empty_profiles_title),
                            description = stringResource(R.string.empty_profiles_body),
                        )
                    }
                } else {
                    items(uiState.profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            relationshipText = context.getString(profile.relationshipType.labelRes()),
                            toneText = context.getString(profile.toneStyle.labelRes()),
                            onClick = { onProfileClick(profile.id) },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.home_recent_sessions_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        TextButton(onClick = onAnalysisClick) {
                            Text(text = stringResource(R.string.home_recent_sessions_more))
                        }
                    }
                }

                if (uiState.recentSessions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(R.string.empty_sessions_title),
                            description = stringResource(R.string.empty_sessions_body),
                        )
                    }
                } else {
                    items(uiState.recentSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            vibeText = context.getString(session.vibe.labelRes()),
                            sourceText = context.getString(session.source.labelRes()),
                            onClick = { onSessionClick(session.id) },
                            onDelete = { viewModel.deleteSession(session.id) },
                        )
                    }
                }
            }
        }
    }
}
