package com.example.replybubble.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.replybubble.R
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.service.ScreenAnalysisForegroundService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreenCapturePermissionActivity : ComponentActivity() {
    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: AnalysisSource.APP.name
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L).takeIf { it > 0L }
        val delayMs = intent.getLongExtra(EXTRA_DELAY_MS, DEFAULT_DELAY_MS)
        val openAfter = intent.getBooleanExtra(EXTRA_OPEN_AFTER_RESULT, true)

        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ContextCompat.startForegroundService(
                this,
                ScreenAnalysisForegroundService.createIntent(
                    context = this,
                    resultCode = result.resultCode,
                    resultData = result.data!!,
                    contactId = contactId,
                    source = source,
                    delayMs = delayMs,
                    openAfterResult = openAfter,
                ),
            )
            if (source == AnalysisSource.OVERLAY.name && !openAfter) {
                moveTaskToBack(true)
            }
        } else {
            sendBroadcast(
                Intent(ScreenAnalysisForegroundService.ACTION_ANALYSIS_FINISHED)
                    .setPackage(packageName)
                    .putExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_SESSION_ID, -1L)
                    .putExtra(
                        ScreenAnalysisForegroundService.EXTRA_FINISHED_STATUS,
                        getString(R.string.overlay_status_capture_cancelled),
                    ),
            )
            Toast.makeText(this, getString(R.string.overlay_status_capture_cancelled), Toast.LENGTH_SHORT).show()
        }
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        if (savedInstanceState == null) {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    companion object {
        private const val EXTRA_CONTACT_ID = "extra_contact_id"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_DELAY_MS = "extra_delay_ms"
        private const val EXTRA_OPEN_AFTER_RESULT = "extra_open_after_result"
        private const val DEFAULT_DELAY_MS = 1500L

        fun createIntent(
            context: Context,
            contactId: Long?,
            source: AnalysisSource,
            delayMs: Long,
            openAfterResult: Boolean,
        ): Intent {
            return Intent(context, ScreenCapturePermissionActivity::class.java)
                .putExtra(EXTRA_CONTACT_ID, contactId ?: -1L)
                .putExtra(EXTRA_SOURCE, source.name)
                .putExtra(EXTRA_DELAY_MS, delayMs)
                .putExtra(EXTRA_OPEN_AFTER_RESULT, openAfterResult)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
    }
}
