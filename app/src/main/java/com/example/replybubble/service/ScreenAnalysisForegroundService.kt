package com.example.replybubble.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.replybubble.MainActivity
import com.example.replybubble.R
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.ocr.OcrProcessor
import com.example.replybubble.overlay.OverlayRuntimeState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@AndroidEntryPoint
class ScreenAnalysisForegroundService : Service() {
    @Inject
    lateinit var ocrProcessor: OcrProcessor

    @Inject
    lateinit var sessionRepository: SessionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()
        startForegroundInternal()
        if (intent?.action == ACTION_ANALYZE_SCREEN) {
            serviceScope.launch {
                runCatching {
                    handleAnalysis(intent)
                }.onFailure { throwable ->
                    val failedStatus = getString(R.string.overlay_status_analysis_failed)
                    OverlayRuntimeState.updateStatus(failedStatus)
                    sendBroadcast(
                        Intent(ACTION_ANALYSIS_FINISHED)
                            .setPackage(packageName)
                            .putExtra(EXTRA_FINISHED_SESSION_ID, -1L)
                            .putExtra(EXTRA_FINISHED_STATUS, failedStatus),
                    )
                    android.util.Log.e("ScreenAnalysisService", "Screen analysis failed", throwable)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleAnalysis(intent: Intent) {
        OverlayRuntimeState.updateStatus(getString(R.string.overlay_status_capture_running))
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val resultData = intent.intentExtraCompat(EXTRA_RESULT_DATA) ?: run {
            stopSelf()
            return
        }
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L).takeIf { it > 0L }
        val source = runCatching {
            enumValueOf<AnalysisSource>(intent.getStringExtra(EXTRA_SOURCE) ?: AnalysisSource.APP.name)
        }.getOrDefault(AnalysisSource.APP)
        val delayMs = intent.getLongExtra(EXTRA_DELAY_MS, 1500L)
        val openAfterResult = intent.getBooleanExtra(EXTRA_OPEN_AFTER_RESULT, true)

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        val mediaProjection = runCatching {
            mediaProjectionManager.getMediaProjection(resultCode, resultData)
        }.getOrNull()

        val bitmap = mediaProjection?.let { projection ->
            captureBitmap(projection, delayMs)
        }
        val rawText = bitmap?.let { ocrProcessor.recognize(it).rawText }.orEmpty()
        val sessionId = sessionRepository.processCapturedText(contactId, rawText, source)

        val completedStatus = getString(R.string.overlay_status_analysis_complete)
        OverlayRuntimeState.updateStatus(completedStatus, sessionId)
        sendBroadcast(
            Intent(ACTION_ANALYSIS_FINISHED)
                .setPackage(packageName)
                .putExtra(EXTRA_FINISHED_SESSION_ID, sessionId)
                .putExtra(EXTRA_FINISHED_STATUS, completedStatus),
        )

        if (openAfterResult) {
            startActivity(
                MainActivity.createIntent(this, sessionId).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun captureBitmap(
        mediaProjection: MediaProjection,
        delayMs: Long,
    ): Bitmap? {
        delay(delayMs)
        val metrics = getScreenMetrics()
        val width = metrics.width
        val height = metrics.height
        val density = metrics.densityDpi
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val handlerThread = HandlerThread("screen-capture-thread").apply { start() }
        val handler = Handler(handlerThread.looper)
        var virtualDisplay: VirtualDisplay? = null

        return try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "replybubble_capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler,
            )
            delay(350L)
            withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine { continuation ->
                    imageReader.setOnImageAvailableListener(
                        { reader: ImageReader ->
                            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                            val bitmap = runCatching { image.toBitmap(width, height) }.getOrNull()
                            image.close()
                            imageReader.setOnImageAvailableListener(null, null)
                            if (continuation.isActive) {
                                continuation.resume(bitmap)
                            }
                        },
                        handler,
                    )
                }
            }
        } finally {
            runCatching { imageReader.close() }
            runCatching { virtualDisplay?.release() }
            runCatching { mediaProjection.stop() }
            handlerThread.quitSafely()
        }
    }

    private fun Image.toBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val plane = planes.first()
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    private fun getScreenMetrics(): ScreenMetrics {
        val density = resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = getSystemService(WindowManager::class.java)
            val bounds = windowManager.currentWindowMetrics.bounds
            ScreenMetrics(bounds.width(), bounds.height(), density)
        } else {
            ScreenMetrics(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                density,
            )
        }
    }

    private fun startForegroundInternal() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_analysis_title))
            .setContentText(getString(R.string.notification_analysis_body))
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ANALYSIS_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(ANALYSIS_NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_analysis),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private data class ScreenMetrics(
        val width: Int,
        val height: Int,
        val densityDpi: Int,
    )

    companion object {
        const val ACTION_ANALYZE_SCREEN = "com.example.replybubble.action.ANALYZE_SCREEN"
        const val ACTION_ANALYSIS_FINISHED = "com.example.replybubble.action.ANALYSIS_FINISHED"

        private const val CHANNEL_ID = "screen_analysis"
        private const val ANALYSIS_NOTIFICATION_ID = 3002
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val EXTRA_CONTACT_ID = "extra_contact_id"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_DELAY_MS = "extra_delay_ms"
        private const val EXTRA_OPEN_AFTER_RESULT = "extra_open_after_result"
        const val EXTRA_FINISHED_SESSION_ID = "extra_finished_session_id"
        const val EXTRA_FINISHED_STATUS = "extra_finished_status"

        fun createIntent(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            contactId: Long?,
            source: String,
            delayMs: Long,
            openAfterResult: Boolean,
        ): Intent {
            return Intent(context, ScreenAnalysisForegroundService::class.java)
                .setAction(ACTION_ANALYZE_SCREEN)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
                .putExtra(EXTRA_CONTACT_ID, contactId ?: -1L)
                .putExtra(EXTRA_SOURCE, source)
                .putExtra(EXTRA_DELAY_MS, delayMs)
                .putExtra(EXTRA_OPEN_AFTER_RESULT, openAfterResult)
        }
    }
}

@Suppress("DEPRECATION")
private fun Intent.intentExtraCompat(key: String): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        getParcelableExtra(key)
    }
}
