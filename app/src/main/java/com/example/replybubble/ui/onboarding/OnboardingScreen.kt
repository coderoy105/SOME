package com.example.replybubble.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.R
import com.example.replybubble.overlay.OverlayPermissionHelper
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val overlayGranted = OverlayPermissionHelper.canDrawOverlays(context)

    GradientScreenContainer(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionCard(
            title = stringResource(R.string.onboarding_card_overlay_title),
            subtitle = stringResource(R.string.onboarding_card_overlay_body),
        ) {}
        SectionCard(
            title = stringResource(R.string.onboarding_card_capture_title),
            subtitle = stringResource(R.string.onboarding_card_capture_body),
        ) {}
        SectionCard(
            title = stringResource(R.string.onboarding_card_engine_title),
            subtitle = stringResource(R.string.onboarding_card_engine_body),
        ) {}
        FilledTonalButton(
            onClick = { OverlayPermissionHelper.openOverlayPermissionSettings(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (overlayGranted) {
                    stringResource(R.string.overlay_permission_granted)
                } else {
                    stringResource(R.string.overlay_permission_button)
                },
            )
        }
        Button(
            onClick = {
                viewModel.completeOnboarding()
                onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_start_button))
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
