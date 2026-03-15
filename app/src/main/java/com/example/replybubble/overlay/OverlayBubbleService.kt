package com.example.replybubble.overlay

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.replybubble.MainActivity
import com.example.replybubble.R
import com.example.replybubble.correction.TextCorrectionEngine
import com.example.replybubble.domain.model.ContactProfile
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.ReplySuggestion
import com.example.replybubble.domain.repository.ProfileRepository
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.domain.repository.SettingsRepository
import com.example.replybubble.service.ScreenAnalysisForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class OverlayBubbleService : Service() {
    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var textCorrectionEngine: TextCorrectionEngine

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var receiverRegistered = false
    private var analysisInProgress = false
    private var correctionInProgress = false
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private var statusTextView: TextView? = null
    private var profileValueTextView: TextView? = null
    private var suggestionsTitleView: TextView? = null
    private var suggestionsToggleView: TextView? = null
    private var suggestionsScrollView: ScrollView? = null
    private var suggestionsContainer: LinearLayout? = null
    private var suggestionsCollapsed = false
    private var latestSessionId: Long? = null
    private var availableProfiles: List<ContactProfile> = emptyList()
    private var selectedProfileId: Long? = null
    private var overlayScale: Float = 1f

    private val analysisResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            analysisInProgress = false
            latestSessionId = intent
                ?.getLongExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_SESSION_ID, -1L)
                ?.takeIf { it > 0L }

            val status = intent?.getStringExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_STATUS)
                ?: getString(R.string.overlay_status_ready)
            statusTextView?.text = status
            OverlayRuntimeState.updateStatus(status, latestSessionId)

            val sessionId = latestSessionId
            if (sessionId != null) {
                loadSuggestions(sessionId)
                showPanel()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            OverlayRuntimeState.updateStatus(getString(R.string.overlay_status_permission_missing))
            stopSelf()
            return
        }

        selectedProfileId = readSelectedProfileId()
        overlayScale = runBlocking {
            settingsRepository.getSettings().overlayScale
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        runCatching {
            createBubble()
            createPanel()
            ensureNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_overlay_title))
                .setContentText(getString(R.string.notification_overlay_body))
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        100,
                        MainActivity.createIntent(this, null),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
                .setOngoing(true)
                .build()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
            registerAnalysisReceiver()
            observeProfiles()
            observeOverlaySettings()
            OverlayRuntimeState.updateRunning(true)
            OverlayRuntimeState.updateStatus(getString(R.string.overlay_status_ready))
        }.onFailure { throwable ->
            Log.e("OverlayBubbleService", "Failed to start overlay", throwable)
            OverlayRuntimeState.updateRunning(false)
            OverlayRuntimeState.updateStatus(getString(R.string.overlay_status_start_failed))
            Toast.makeText(this, getString(R.string.overlay_status_start_failed), Toast.LENGTH_LONG).show()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SHOW_PANEL -> togglePanel()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        runCatching { bubbleView?.let { windowManager.removeView(it) } }
        runCatching { panelView?.let { windowManager.removeView(it) } }
        if (receiverRegistered) {
            runCatching { unregisterReceiver(analysisResultReceiver) }
        }
        OverlayRuntimeState.updateRunning(false)
        if (OverlayRuntimeState.state.value.lastStatus != getString(R.string.overlay_status_start_failed)) {
            OverlayRuntimeState.updateStatus(getString(R.string.overlay_status_stopped), null)
        }
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubble() {
        if (bubbleView != null) return

        bubbleParams = WindowManager.LayoutParams(
            dp(48),
            dp(48),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(220)
        }

        val container = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.overlayBubbleColor))
                setStroke(dp(1), ContextCompat.getColor(context, android.R.color.white))
            }
            elevation = dp(10).toFloat()
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_replybubble_badge)
            setPadding(dp(11), dp(11), dp(11), dp(11))
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white))
        }
        container.addView(icon)

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    bubbleParams.x = initialX + (event.rawX - touchX).toInt()
                    bubbleParams.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, bubbleParams)
                    updatePanelPosition()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) > 12 || abs(event.rawY - touchY) > 12
                    if (!moved) togglePanel()
                    true
                }

                else -> false
            }
        }

        bubbleView = container
        windowManager.addView(container, bubbleParams)
    }

    private fun createPanel() {
        if (panelView != null) return

        panelParams = WindowManager.LayoutParams(
            dp(258),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = ContextCompat.getDrawable(context, R.drawable.bg_overlay_panel)
            elevation = dp(12).toFloat()
        }

        val header = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val title = TextView(this).apply {
            text = getString(R.string.overlay_title)
            textSize = 19f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        val collapseButton = TextView(this).apply {
            text = getString(R.string.overlay_action_fold)
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = 0.78f
            setOnClickListener { hidePanel() }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL,
            )
        }
        header.addView(title)
        header.addView(collapseButton)
        val topDivider = makeDivider()
        val status = TextView(this).apply {
            text = getString(R.string.overlay_status_ready)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setLineSpacing(0f, 1.18f)
        }
        val middleDivider = makeDivider()
        val profileLabel = TextView(this).apply {
            text = getString(R.string.analysis_profile_selector_title)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, dp(2), 0, dp(4))
        }
        val profileValue = TextView(this).apply {
            text = getString(R.string.analysis_profile_selector_empty)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, 0, 0, dp(8))
        }
        val profileButton = makeActionButton(getString(R.string.overlay_profile_button), accent = true) {}
        profileButton.setOnClickListener { showProfileMenu(profileButton) }
        val suggestionsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, dp(10), 0, dp(6))
        }
        val suggestionsTitle = TextView(this).apply {
            text = getString(R.string.overlay_suggestions_title)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        val suggestionsToggle = TextView(this).apply {
            text = getString(R.string.overlay_suggestions_hide)
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = 0.78f
            setOnClickListener {
                suggestionsCollapsed = !suggestionsCollapsed
                updateSuggestionsVisibility()
            }
        }
        suggestionsHeader.addView(
            suggestionsTitle,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        suggestionsHeader.addView(suggestionsToggle)
        val suggestionList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val suggestionScroll = ScrollView(this).apply {
            addView(
                suggestionList,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(184),
            )
        }

        statusTextView = status
        profileValueTextView = profileValue
        suggestionsTitleView = suggestionsTitle
        suggestionsToggleView = suggestionsToggle
        suggestionsScrollView = suggestionScroll
        suggestionsContainer = suggestionList

        panel.addView(header)
        panel.addView(topDivider)
        panel.addView(status)
        panel.addView(middleDivider)
        panel.addView(profileLabel)
        panel.addView(profileValue)
        panel.addView(profileButton)
        panel.addView(makeActionButton(getString(R.string.overlay_action_analyze)) { startAnalysis() })
        panel.addView(makeActionButton(getString(R.string.overlay_action_correct_input)) { correctComposerInput() })
        panel.addView(makeActionButton(getString(R.string.overlay_action_home)) {
            startActivity(
                MainActivity.createIntent(this, latestSessionId).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
            )
            hidePanel()
        })
        panel.addView(suggestionsHeader)
        panel.addView(suggestionScroll)

        renderSuggestions(emptyList())
        panel.visibility = View.GONE
        windowManager.addView(panel, panelParams)
        panelView = panel
        updatePanelPosition()
    }

    private fun startAnalysis() {
        if (!AccessibilityCaptureHelper.isServiceEnabled(this) || !OverlayAccessibilityService.isConnected()) {
            analysisInProgress = false
            val statusText = getString(R.string.overlay_status_accessibility_missing)
            statusTextView?.text = statusText
            OverlayRuntimeState.updateStatus(statusText, null)
            hidePanel()
            startActivity(AccessibilityPermissionActivity.createIntent(this, autoStart = true))
            Toast.makeText(this, getString(R.string.overlay_accessibility_toast), Toast.LENGTH_LONG).show()
            return
        }

        if (selectedProfileId == null) {
            val statusText = if (availableProfiles.isEmpty()) {
                getString(R.string.overlay_profile_missing)
            } else {
                getString(R.string.overlay_profile_required)
            }
            statusTextView?.text = statusText
            OverlayRuntimeState.updateStatus(statusText, null)
            Toast.makeText(this, statusText, Toast.LENGTH_SHORT).show()
            showPanel()
            return
        }

        if (analysisInProgress) {
            Toast.makeText(this, getString(R.string.overlay_analysis_in_progress), Toast.LENGTH_SHORT).show()
            return
        }

        analysisInProgress = true
        latestSessionId = null
        renderSuggestions(emptyList())
        val runningStatus = getString(R.string.overlay_status_capture_running)
        statusTextView?.text = runningStatus
        OverlayRuntimeState.updateStatus(runningStatus, null)
        hidePanel()

        if (!OverlayAccessibilityService.requestCapture(contactId = selectedProfileId)) {
            analysisInProgress = false
            val unavailableStatus = getString(R.string.overlay_status_accessibility_unavailable)
            statusTextView?.text = unavailableStatus
            OverlayRuntimeState.updateStatus(unavailableStatus, null)
            Toast.makeText(this, unavailableStatus, Toast.LENGTH_SHORT).show()
        }
    }

    private fun correctComposerInput() {
        if (!AccessibilityCaptureHelper.isServiceEnabled(this) || !OverlayAccessibilityService.isConnected()) {
            val statusText = getString(R.string.overlay_status_accessibility_missing)
            statusTextView?.text = statusText
            OverlayRuntimeState.updateStatus(statusText, latestSessionId)
            hidePanel()
            startActivity(AccessibilityPermissionActivity.createIntent(this, autoStart = true))
            Toast.makeText(this, getString(R.string.overlay_accessibility_toast), Toast.LENGTH_LONG).show()
            return
        }

        if (correctionInProgress) {
            Toast.makeText(this, getString(R.string.overlay_correction_in_progress), Toast.LENGTH_SHORT).show()
            return
        }

        correctionInProgress = true
        hidePanel()
        val runningStatus = getString(R.string.overlay_status_correction_running)
        statusTextView?.text = runningStatus
        OverlayRuntimeState.updateStatus(runningStatus, latestSessionId)

        serviceScope.launch {
            val snapshot = OverlayAccessibilityService.readCurrentComposerText()
            if (snapshot == null || snapshot.text.isBlank()) {
                correctionInProgress = false
                val missingStatus = getString(R.string.overlay_status_input_missing)
                statusTextView?.text = missingStatus
                OverlayRuntimeState.updateStatus(missingStatus, latestSessionId)
                Toast.makeText(this@OverlayBubbleService, missingStatus, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val correctedText = runCatching {
                textCorrectionEngine.correct(snapshot.text)
            }.getOrElse {
                ""
            }.trim()

            if (correctedText.isBlank()) {
                correctionInProgress = false
                val failedStatus = getString(R.string.overlay_status_correction_failed)
                statusTextView?.text = failedStatus
                OverlayRuntimeState.updateStatus(failedStatus, latestSessionId)
                Toast.makeText(this@OverlayBubbleService, failedStatus, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val applied = OverlayAccessibilityService.applyComposerText(correctedText)
            correctionInProgress = false

            when {
                applied && correctedText == snapshot.text -> {
                    val status = getString(R.string.overlay_status_correction_unchanged)
                    statusTextView?.text = status
                    OverlayRuntimeState.updateStatus(status, latestSessionId)
                    Toast.makeText(this@OverlayBubbleService, status, Toast.LENGTH_SHORT).show()
                }

                applied -> {
                    val status = getString(R.string.overlay_status_correction_done)
                    statusTextView?.text = status
                    OverlayRuntimeState.updateStatus(status, latestSessionId)
                    Toast.makeText(this@OverlayBubbleService, status, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("corrected_message", correctedText))
                    val status = getString(R.string.overlay_status_correction_copy_fallback)
                    statusTextView?.text = status
                    OverlayRuntimeState.updateStatus(status, latestSessionId)
                    Toast.makeText(this@OverlayBubbleService, status, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeProfiles() {
        serviceScope.launch {
            profileRepository.observeProfiles().collectLatest { profiles ->
                availableProfiles = profiles.sortedBy { it.name.lowercase() }
                if (availableProfiles.none { it.id == selectedProfileId }) {
                    selectedProfileId = null
                    saveSelectedProfileId(null)
                }
                updateSelectedProfileText()
            }
        }
    }

    private fun observeOverlaySettings() {
        serviceScope.launch {
            settingsRepository.observeSettings().collectLatest { settings ->
                val newScale = settings.overlayScale.coerceIn(0.8f, 1.2f)
                if (abs(newScale - overlayScale) < 0.01f) return@collectLatest
                overlayScale = newScale
                refreshOverlayViews()
            }
        }
    }

    private fun refreshOverlayViews() {
        val currentBubbleX = if (::bubbleParams.isInitialized) bubbleParams.x else scaledDp(16)
        val currentBubbleY = if (::bubbleParams.isInitialized) bubbleParams.y else scaledDp(220)
        val wasPanelVisible = panelView?.visibility == View.VISIBLE
        val currentStatus = statusTextView?.text?.toString() ?: OverlayRuntimeState.state.value.lastStatus

        runCatching { bubbleView?.let { windowManager.removeView(it) } }
        runCatching { panelView?.let { windowManager.removeView(it) } }

        bubbleView = null
        panelView = null
        statusTextView = null
        profileValueTextView = null
        suggestionsTitleView = null
        suggestionsToggleView = null
        suggestionsScrollView = null
        suggestionsContainer = null

        createBubble()
        bubbleParams.x = currentBubbleX
        bubbleParams.y = currentBubbleY
        bubbleView?.let { windowManager.updateViewLayout(it, bubbleParams) }

        createPanel()
        statusTextView?.text = currentStatus
        updateSelectedProfileText()
        latestSessionId?.let { loadSuggestions(it) }

        if (wasPanelVisible) {
            showPanel()
        } else {
            hidePanel()
        }
    }

    private fun loadSuggestions(sessionId: Long) {
        serviceScope.launch {
            val detail = sessionRepository.getSessionDetail(sessionId)
            renderSuggestions(detail?.suggestions.orEmpty())
        }
    }

    private fun renderSuggestions(suggestions: List<ReplySuggestion>) {
        val container = suggestionsContainer ?: return
        container.removeAllViews()
        val hasSuggestions = suggestions.isNotEmpty()
        suggestionsTitleView?.parent?.let { parent ->
            (parent as? View)?.visibility = if (hasSuggestions) View.VISIBLE else View.GONE
        }

        buildOverlaySuggestions(suggestions).forEach { suggestion ->
            container.addView(buildSuggestionView(suggestion))
        }
        updateSuggestionsVisibility()
    }

    private fun buildOverlaySuggestions(suggestions: List<ReplySuggestion>): List<ReplySuggestion> {
        val preferredOrder = listOf(
            ReplyCategory.SAFE,
            ReplyCategory.WITTY,
            ReplyCategory.SWEET,
            ReplyCategory.FOLLOW_UP,
        )
        val byCategory = suggestions.associateBy { it.category }
        val ordered = mutableListOf<ReplySuggestion>()

        preferredOrder.forEach { category ->
            byCategory[category]?.let(ordered::add)
        }

        suggestions.forEach { suggestion ->
            if (ordered.none { it.id == suggestion.id } && ordered.size < 4) {
                ordered.add(suggestion)
            }
        }
        return ordered.take(4)
    }

    private fun updateSuggestionsVisibility() {
        val hasSuggestions = suggestionsContainer?.childCount ?: 0 > 0
        suggestionsTitleView?.parent?.let { parent ->
            (parent as? View)?.visibility = if (hasSuggestions) View.VISIBLE else View.GONE
        }
        suggestionsToggleView?.text = getString(
            if (suggestionsCollapsed) {
                R.string.overlay_suggestions_show
            } else {
                R.string.overlay_suggestions_hide
            },
        )
        suggestionsScrollView?.visibility = if (hasSuggestions && !suggestionsCollapsed) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun buildSuggestionView(suggestion: ReplySuggestion): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xFFFFFFFF.toInt())
                setStroke(dp(1), 0x26F04F9C.toInt())
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
        }

        val category = TextView(this).apply {
            text = overlayCategoryLabel(suggestion)
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.overlayBubbleColor))
        }
        val content = TextView(this).apply {
            text = suggestion.content
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(0, dp(4), 0, dp(5))
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val refreshButton = makeActionButton(getString(R.string.common_refresh)) {
            serviceScope.launch {
                statusTextView?.text = getString(R.string.recommendation_refreshing_single)
                runCatching {
                    sessionRepository.regenerateSuggestion(suggestion.id)
                }.onSuccess {
                    latestSessionId?.let { loadSuggestions(it) }
                    statusTextView?.text = getString(R.string.overlay_status_analysis_complete)
                }.onFailure {
                    statusTextView?.text = getString(R.string.overlay_status_analysis_failed)
                    Toast.makeText(this@OverlayBubbleService, getString(R.string.overlay_status_analysis_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val copyButton = makeActionButton(getString(R.string.overlay_copy_button)) {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("reply_suggestion", suggestion.content))
            Toast.makeText(this, getString(R.string.recommendation_copy_done), Toast.LENGTH_SHORT).show()
            serviceScope.launch {
                sessionRepository.incrementCopyCount(suggestion.id)
            }
            hidePanel()
        }
        actionRow.addView(
            refreshButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        actionRow.addView(
            copyButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        card.addView(category)
        card.addView(content)
        card.addView(actionRow)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(10)
        }
        card.layoutParams = params
        return card
    }

    private fun showProfileMenu(anchor: View) {
        if (availableProfiles.isEmpty()) {
            val message = getString(R.string.overlay_profile_missing)
            statusTextView?.text = message
            OverlayRuntimeState.updateStatus(message, null)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            return
        }

        PopupMenu(this, anchor).apply {
            availableProfiles.forEachIndexed { index, profile ->
                menu.add(0, index + 1, index, profile.name)
            }
            setOnMenuItemClickListener { item ->
                val selectedProfile = availableProfiles.getOrNull(item.order) ?: return@setOnMenuItemClickListener false
                selectedProfileId = selectedProfile.id
                saveSelectedProfileId(selectedProfile.id)
                updateSelectedProfileText()
                val message = getString(R.string.overlay_profile_selected, selectedProfile.name)
                statusTextView?.text = message
                OverlayRuntimeState.updateStatus(message, latestSessionId)
                true
            }
            show()
        }
    }

    private fun updateSelectedProfileText() {
        val label = availableProfiles.firstOrNull { it.id == selectedProfileId }?.name
            ?: getString(R.string.analysis_profile_selector_empty)
        profileValueTextView?.text = label
    }

    private fun readSelectedProfileId(): Long? {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_SELECTED_PROFILE_ID, -1L)
            .takeIf { it > 0L }
    }

    private fun saveSelectedProfileId(profileId: Long?) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SELECTED_PROFILE_ID, profileId ?: -1L)
            .apply()
    }

    private fun overlayCategoryLabel(suggestion: ReplySuggestion): String {
        return when (suggestion.category) {
            ReplyCategory.SAFE -> getString(R.string.category_safe)
            ReplyCategory.WITTY -> getString(R.string.category_witty)
            ReplyCategory.SWEET -> getString(R.string.category_sweet)
            ReplyCategory.SHORT -> getString(R.string.category_short)
            ReplyCategory.FOLLOW_UP -> getString(R.string.category_follow_up)
        }
    }

    private fun makeActionButton(
        label: String,
        accent: Boolean = false,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            gravity = Gravity.CENTER
            minHeight = dp(40)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = if (accent) {
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xFFF8D7F7.toInt(), 0xFFFFFFFF.toInt()),
                ).apply {
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), 0x33E3B6E7.toInt())
                }
            } else {
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0xFFFFFFFF.toInt(), 0xFFFDF8FD.toInt()),
                ).apply {
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), 0x22DCC6D8.toInt())
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(7)
            }
            stateListAnimator = null
            elevation = 0f
            setOnClickListener { onClick() }
        }
    }

    private fun makeDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1),
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x00FFFFFF, 0x22A38AA4, 0x00FFFFFF),
            )
        }
    }

    private fun togglePanel() {
        panelView?.let { panel ->
            panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            updatePanelPosition()
        }
    }

    private fun showPanel() {
        panelView?.visibility = View.VISIBLE
        updatePanelPosition()
    }

    private fun hidePanel() {
        panelView?.visibility = View.GONE
    }

    private fun updatePanelPosition() {
        if (!::panelParams.isInitialized || !::bubbleParams.isInitialized) return
        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalPadding = dp(8)
        val targetX = bubbleParams.x - dp(118)
        val maxX = max(horizontalPadding, screenWidth - panelParams.width - horizontalPadding)

        panelParams.x = targetX.coerceIn(horizontalPadding, maxX)
        panelParams.y = max(dp(24), bubbleParams.y + dp(58))
        panelView?.let { panel ->
            runCatching { windowManager.updateViewLayout(panel, panelParams) }
        }
    }

    private fun registerAnalysisReceiver() {
        val filter = IntentFilter(ScreenAnalysisForegroundService.ACTION_ANALYSIS_FINISHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(analysisResultReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(analysisResultReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun scaledDp(value: Int): Int {
        return max(1, (value * resources.displayMetrics.density * overlayScale).toInt())
    }

    private fun scaledSp(value: Float): Float {
        return value * overlayScale
    }

    private fun dp(value: Int): Int {
        return scaledDp(value)
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_overlay),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 3001
        private const val ACTION_STOP = "com.example.replybubble.action.STOP_OVERLAY"
        private const val ACTION_SHOW_PANEL = "com.example.replybubble.action.SHOW_OVERLAY_PANEL"
        private const val PREFS_NAME = "overlay_preferences"
        private const val KEY_SELECTED_PROFILE_ID = "selected_profile_id"

        fun start(context: Context) {
            context.startService(Intent(context, OverlayBubbleService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayBubbleService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
