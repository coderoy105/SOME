package com.example.replybubble.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.replybubble.R
import com.example.replybubble.di.ServiceEntryPoint
import com.example.replybubble.domain.model.AnalysisSource
import com.example.replybubble.domain.repository.SessionRepository
import com.example.replybubble.service.ScreenAnalysisForegroundService
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionRepository: SessionRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java,
        ).sessionRepository()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun captureCurrentScreen(contactId: Long?) {
        serviceScope.launch {
            delay(450L)
            val root = rootInActiveWindow
            if (root == null) {
                sendStatus(getString(R.string.overlay_status_accessibility_unavailable))
                return@launch
            }

            val activePackage = root.packageName?.toString().orEmpty()
            if (activePackage == packageName) {
                sendStatus(getString(R.string.overlay_status_wrong_screen))
                return@launch
            }

            val rawText = collectVisibleText(root)
            if (rawText.length < 6) {
                sendStatus(getString(R.string.overlay_status_accessibility_unavailable))
                return@launch
            }

            val sessionId = runCatching {
                sessionRepository.processCapturedText(
                    contactId = contactId,
                    rawText = rawText,
                    source = AnalysisSource.OVERLAY,
                )
            }.getOrElse {
                sendStatus(getString(R.string.overlay_status_analysis_failed))
                return@launch
            }

            sendResult(
                sessionId = sessionId,
                status = getString(R.string.overlay_status_analysis_complete),
            )
        }
    }

    private fun sendResult(
        sessionId: Long,
        status: String,
    ) {
        OverlayRuntimeState.updateStatus(status, sessionId)
        sendBroadcast(
            Intent(ScreenAnalysisForegroundService.ACTION_ANALYSIS_FINISHED)
                .setPackage(packageName)
                .putExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_SESSION_ID, sessionId)
                .putExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_STATUS, status),
        )
    }

    private fun sendStatus(status: String) {
        OverlayRuntimeState.updateStatus(status, null)
        sendBroadcast(
            Intent(ScreenAnalysisForegroundService.ACTION_ANALYSIS_FINISHED)
                .setPackage(packageName)
                .putExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_SESSION_ID, -1L)
                .putExtra(ScreenAnalysisForegroundService.EXTRA_FINISHED_STATUS, status),
        )
    }

    private fun captureEditableText(): EditableTextSnapshot? {
        val root = rootInActiveWindow ?: return null
        val activePackage = root.packageName?.toString().orEmpty()
        if (activePackage == packageName) return null

        val editableNode = findBestEditableNode(root) ?: return null
        val text = editableNode.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return null
        return EditableTextSnapshot(text = text)
    }

    private fun applyCorrectedText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val activePackage = root.packageName?.toString().orEmpty()
        if (activePackage == packageName) return false

        val editableNode = findBestEditableNode(root) ?: return false
        editableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun collectVisibleText(root: AccessibilityNodeInfo): String {
        val entries = mutableListOf<VisibleText>()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        walkNode(root, entries, screenWidth, screenHeight)

        val ordered = entries
            .sortedWith(compareBy<VisibleText> { it.top }.thenBy { it.left })
            .fold(mutableListOf<VisibleText>()) { acc, current ->
                val previous = acc.lastOrNull()
                val isDuplicate = previous != null &&
                    previous.text == current.text &&
                    previous.speaker == current.speaker &&
                    abs(previous.top - current.top) <= (12 * resources.displayMetrics.density).toInt()
                if (!isDuplicate) {
                    acc += current
                }
                acc
            }
        val taggedLines = buildTaggedConversation(ordered)
        if (taggedLines.isNotEmpty()) {
            return taggedLines.joinToString("\n")
        }

        return ""
    }

    private fun findBestEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<EditableCandidate>()
        val screenHeight = resources.displayMetrics.heightPixels
        walkEditableNode(root, candidates, screenHeight)
        return candidates.maxByOrNull { it.score }?.node
    }

    private fun walkEditableNode(
        node: AccessibilityNodeInfo,
        candidates: MutableList<EditableCandidate>,
        screenHeight: Int,
    ) {
        if (!node.isVisibleToUser) return

        val supportsSetText = node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        val looksEditable = node.isEditable ||
            supportsSetText ||
            node.className?.toString()?.contains("EditText", ignoreCase = true) == true

        if (looksEditable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                var score = rect.bottom
                if (node.isFocused) score += screenHeight
                if (rect.top > (screenHeight * 0.5f).toInt()) score += screenHeight / 2
                if (rect.height() in dp(32)..dp(120)) score += 240
                candidates += EditableCandidate(node = node, score = score)
            }
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            walkEditableNode(child, candidates, screenHeight)
        }
    }

    private fun walkNode(
        node: AccessibilityNodeInfo,
        entries: MutableList<VisibleText>,
        screenWidth: Int,
        screenHeight: Int,
    ) {
        if (!node.isVisibleToUser) {
            return
        }

        if (!node.isEditable) {
            appendNodeText(node, node.text, entries, screenWidth, screenHeight)
            appendNodeText(node, node.contentDescription, entries, screenWidth, screenHeight)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            walkNode(child, entries, screenWidth, screenHeight)
        }
    }

    private fun appendNodeText(
        node: AccessibilityNodeInfo,
        value: CharSequence?,
        entries: MutableList<VisibleText>,
        screenWidth: Int,
        screenHeight: Int,
    ) {
        val normalized = value
            ?.toString()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.length < 2) return
        if (isLikelyNonMessageText(normalized)) return

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return
        if (rect.bottom < (screenHeight * 0.12f).toInt()) return
        if (rect.top > (screenHeight * 0.87f).toInt()) return

        entries += VisibleText(
            text = normalized,
            top = rect.top,
            bottom = rect.bottom,
            left = rect.left,
            right = rect.right,
            speaker = detectSpeaker(rect, screenWidth),
        )
    }

    private fun buildTaggedConversation(entries: List<VisibleText>): List<String> {
        val grouped = mutableListOf<GroupedMessage>()
        val mergeThresholdPx = (20 * resources.displayMetrics.density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels

        entries.forEach { entry ->
            val speaker = entry.speaker ?: return@forEach
            if (entry.text.length <= 1) return@forEach

            val last = grouped.lastOrNull()
            val sameBubble = last != null &&
                last.speaker == speaker &&
                abs(entry.top - last.bottom) <= mergeThresholdPx &&
                abs(entry.left - last.left) <= (screenWidth * 0.14f).toInt()

            if (sameBubble) {
                if (last.parts.lastOrNull() != entry.text) {
                    last.parts += entry.text
                    last.bottom = maxOf(last.bottom, entry.bottom)
                }
            } else {
                grouped += GroupedMessage(
                    speaker = speaker,
                    bottom = entry.bottom,
                    left = entry.left,
                    parts = mutableListOf(entry.text),
                )
            }
        }

        return grouped
            .map { group ->
                val mergedText = group.parts
                    .fold(mutableListOf<String>()) { acc, part ->
                        if (acc.lastOrNull() != part) {
                            acc += part
                        }
                        acc
                    }
                    .joinToString(" ")
                    .trim()

                if (mergedText.isBlank()) {
                    ""
                } else {
                    "${group.speaker.label}: $mergedText"
                }
            }
            .filter { it.isNotBlank() }
            .takeLast(14)
    }

    private fun isLikelyNonMessageText(text: String): Boolean {
        val normalized = text.lowercase()
        val exactNoise = setOf(
            "프로필",
            "프로필 보기",
            "프로필 사진",
            "스티커",
            "더보기",
            "스티커 더보기",
            "이모티콘",
            "이모지",
            "gif",
            "사진",
            "앨범",
            "갤러리",
            "카메라",
            "메시지 입력",
            "메시지를 입력하세요",
            "전송",
            "보내기",
            "답장",
            "입력",
            "복사",
            "공유",
            "메뉴",
            "읽음",
            "검색",
            "replybubble",
            "사진",
            "사진 보기",
        )
        if (normalized in exactNoise) return true

        val containsNoise = listOf(
            "프로필",
            "스티커",
            "더보기",
            "이모티콘",
            "메시지 입력",
            "입력하세요",
            "사진 보내기",
            "사진 보기",
            "카메라",
            "갤러리",
            "앨범",
            "공유",
        )
        if (containsNoise.any { normalized.contains(it) }) return true

        return false
    }

    private fun detectSpeaker(
        rect: Rect,
        screenWidth: Int,
    ): Speaker? {
        val bubbleWidthRatio = rect.width() / screenWidth.toFloat()
        if (bubbleWidthRatio >= 0.78f) return null

        val centerX = (rect.left + rect.right) / 2f
        val leftZone = screenWidth * 0.44f
        val rightZone = screenWidth * 0.56f
        if (centerX <= leftZone) return Speaker.OTHER
        if (centerX >= rightZone) return Speaker.ME

        val distanceToLeft = rect.left
        val distanceToRight = screenWidth - rect.right
        val horizontalBiasThreshold = (screenWidth * 0.08f).toInt()

        return when {
            distanceToRight + horizontalBiasThreshold < distanceToLeft -> Speaker.ME
            distanceToLeft + horizontalBiasThreshold < distanceToRight -> Speaker.OTHER
            else -> null
        }
    }

    private data class VisibleText(
        val text: String,
        val top: Int,
        val bottom: Int,
        val left: Int,
        val right: Int,
        val speaker: Speaker?,
    )

    private data class GroupedMessage(
        val speaker: Speaker,
        var bottom: Int,
        val left: Int,
        val parts: MutableList<String>,
    )

    private data class EditableCandidate(
        val node: AccessibilityNodeInfo,
        val score: Int,
    )

    data class EditableTextSnapshot(
        val text: String,
    )

    private enum class Speaker(val label: String) {
        OTHER("상대"),
        ME("나"),
    }

    companion object {
        @Volatile
        private var instance: OverlayAccessibilityService? = null

        fun isConnected(): Boolean = instance != null

        fun requestCapture(contactId: Long?): Boolean {
            val service = instance ?: return false
            service.captureCurrentScreen(contactId)
            return true
        }

        fun readCurrentComposerText(): EditableTextSnapshot? {
            return instance?.captureEditableText()
        }

        fun applyComposerText(text: String): Boolean {
            return instance?.applyCorrectedText(text) == true
        }
    }
}
