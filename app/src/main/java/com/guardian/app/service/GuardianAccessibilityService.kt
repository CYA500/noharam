package com.guardian.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guardian.app.data.ThreatEventRepository
import com.guardian.app.engine.AlertEngine
import com.guardian.app.engine.ImageClassifier
import com.guardian.app.engine.KeywordEngine
import com.guardian.app.engine.MediaDeleter
import com.guardian.app.engine.ThreatLevel
import com.guardian.app.lock.LockEngine
import com.guardian.app.lock.ScreenLockManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

private val Context.settingsStore by preferencesDataStore("guardian_settings")
private val KEY_QURAN_PKG = stringPreferencesKey("quran_pkg")

@AndroidEntryPoint
class GuardianAccessibilityService : AccessibilityService() {

    @Inject lateinit var keywordEngine: KeywordEngine
    @Inject lateinit var alertEngine: AlertEngine
    @Inject lateinit var lockEngine: LockEngine
    @Inject lateinit var screenLockManager: ScreenLockManager
    @Inject lateinit var imageClassifier: ImageClassifier
    @Inject lateinit var mediaDeleter: MediaDeleter
    @Inject lateinit var threatEventRepository: ThreatEventRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastAnalysedText = ""
    private var lastAlertTime = 0L
    private var lastImageCheckTime = 0L

    companion object {
        private const val ALERT_DEBOUNCE_MS = 3_000L
        private const val IMAGE_DEBOUNCE_MS = 5_000L
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        scope.launch { handleEvent(event, pkg) }
    }

    private suspend fun handleEvent(event: AccessibilityEvent, pkg: String) {
        val lockState = lockEngine.currentState()
        val quranPkg = applicationContext.settingsStore.data
            .map { it[KEY_QURAN_PKG] ?: LockEngine.DEFAULT_QURAN_PACKAGE }
            .first()

        if (lockState.isLocked && !lockState.isExpired) {
            if (!lockEngine.isPackageAllowed(pkg, lockState.level, quranPkg)) {
                screenLockManager.bringLockToFront(
                    lockState.level,
                    lockState.remainingMs,
                    lockState.message,
                    lockState.lockEndMs
                )
                return
            }
        } else if (lockState.isExpired) {
            lockEngine.releaseLock()
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text.joinToString(" ").trim()
                analyseText(text)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                analyseText(extractAllText(root))
                checkClipboardAndImages(root)
            }
        }
    }

    private suspend fun analyseText(text: String) {
        if (text == lastAnalysedText || text.length < 3) return
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < ALERT_DEBOUNCE_MS) return
        lastAnalysedText = text

        when (val threatLevel = keywordEngine.analyse(text).level) {
            ThreatLevel.LINK -> {
                lastAlertTime = now
                val minutes = Random.nextLong(30, 121)
                val message = alertEngine.randomMessage()
                threatEventRepository.record(2)
                performGlobalAction(GLOBAL_ACTION_HOME)
                alertEngine.triggerLevel2(minutes)
                lockEngine.activateLock(2, minutes, message)
            }
            ThreatLevel.KEYWORD -> {
                lastAlertTime = now
                threatEventRepository.record(1)
                performGlobalAction(GLOBAL_ACTION_BACK)
                alertEngine.triggerLevel1()
            }
            ThreatLevel.NONE -> { }
            else -> { }
        }
    }

    private suspend fun checkClipboardAndImages(root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastImageCheckTime < IMAGE_DEBOUNCE_MS) return

        val imageUri = findImageUri(root) ?: readClipboardImageUri()
        if (imageUri != null) {
            lastImageCheckTime = now
            val classification = imageClassifier.classifyUri(imageUri)
            if (classification.isExplicit) {
                triggerLevel3(imageUri)
            }
        }
    }

    private suspend fun triggerLevel3(uri: Uri) {
        val now = System.currentTimeMillis()
        lastAlertTime = now
        mediaDeleter.deleteImageUri(uri)
        threatEventRepository.record(3)
        val minutes = Random.nextLong(5 * 60, 24 * 60 + 1)
        val message = alertEngine.randomMessage()
        alertEngine.triggerLevel3(minutes)
        lockEngine.activateLock(3, minutes, message)
        screenLockManager.lockScreenNow()
    }

    private fun findImageUri(node: AccessibilityNodeInfo?): Uri? {
        if (node == null) return null
        node.contentDescription?.toString()?.let { desc ->
            if (desc.startsWith("content://")) return Uri.parse(desc)
        }
        for (i in 0 until node.childCount) {
            findImageUri(node.getChild(i))?.let { return it }
        }
        return null
    }

    private fun readClipboardImageUri(): Uri? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        return item.uri ?: item.coerceToText(this)?.toString()?.let {
            if (it.startsWith("content://")) Uri.parse(it) else null
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        collectText(node, sb, 0)
        return sb.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > 10) return
        node.text?.let { sb.append(it).append(' ') }
        node.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i) ?: continue, sb, depth + 1)
        }
    }

    override fun onInterrupt() { }
}
