package com.example.replybubble.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.replybubble.R
import com.example.replybubble.ui.common.GradientScreenContainer
import com.example.replybubble.ui.common.SectionCard
import com.example.replybubble.ui.theme.ReplyBubbleTheme

class AccessibilityPermissionActivity : ComponentActivity() {
    private var autoStart = false
    private var launchedAppInfo = false
    private var launchedAccessibilitySettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        autoStart = intent.getBooleanExtra(EXTRA_AUTO_START, false)
        launchedAppInfo = savedInstanceState?.getBoolean(KEY_LAUNCHED_APP_INFO) ?: false
        launchedAccessibilitySettings =
            savedInstanceState?.getBoolean(KEY_LAUNCHED_ACCESSIBILITY_SETTINGS) ?: false

        setContent {
            ReplyBubbleTheme {
                Surface {
                    AccessibilityPermissionScreen(
                        onOpenAppInfo = ::openAppInfo,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AccessibilityCaptureHelper.isServiceEnabled(this)) {
            Toast.makeText(this, getString(R.string.accessibility_permission_done), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!autoStart) return

        when {
            !launchedAppInfo -> openAppInfo()
            !launchedAccessibilitySettings -> openAccessibilitySettings()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LAUNCHED_APP_INFO, launchedAppInfo)
        outState.putBoolean(KEY_LAUNCHED_ACCESSIBILITY_SETTINGS, launchedAccessibilitySettings)
    }

    private fun openAppInfo() {
        launchedAppInfo = true
        AccessibilityCaptureHelper.openAppInfoSettings(this)
    }

    private fun openAccessibilitySettings() {
        launchedAccessibilitySettings = true
        AccessibilityCaptureHelper.openAccessibilitySettings(this)
    }

    companion object {
        private const val EXTRA_AUTO_START = "extra_auto_start"
        private const val KEY_LAUNCHED_APP_INFO = "launched_app_info"
        private const val KEY_LAUNCHED_ACCESSIBILITY_SETTINGS = "launched_accessibility_settings"

        fun createIntent(context: Context, autoStart: Boolean): Intent {
            return Intent(context, AccessibilityPermissionActivity::class.java)
                .putExtra(EXTRA_AUTO_START, autoStart)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilityPermissionScreen(
    onOpenAppInfo: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.accessibility_permission_title)) },
            )
        },
    ) { innerPadding ->
        GradientScreenContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(
                    title = stringResource(R.string.accessibility_permission_card_title),
                    subtitle = stringResource(R.string.accessibility_permission_card_body),
                ) {
                    Text(
                        text = stringResource(R.string.accessibility_permission_step_1_body),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.accessibility_permission_step_1_button))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.accessibility_permission_step_2_body),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.accessibility_permission_step_2_button))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.overlay_action_close))
                    }
                }
            }
        }
    }
}
