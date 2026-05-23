package com.guardian.app.lock

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.guardian.app.engine.AlertEngine
import com.guardian.app.engine.GuardianAI
import com.guardian.app.ui.theme.GuardianTheme
import com.guardian.app.ui.theme.Gold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockOverlayActivity : ComponentActivity() {

    @Inject lateinit var lockEngine: LockEngine
    @Inject lateinit var guardianAI: GuardianAI
    @Inject lateinit var alertEngine: AlertEngine

    companion object {
        const val EXTRA_THREAT_LEVEL = "threat_level"
        const val EXTRA_LOCK_MINUTES = "lock_minutes"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_IS_LOCK_SCREEN = "is_lock_screen"
        const val EXTRA_BLOCKED_BANNER = "blocked_banner"
        const val EXTRA_LOCK_END_MS = "lock_end_ms"
        private const val MESSAGE_DISPLAY_SECONDS = 30
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val threatLevel = intent.getIntExtra(EXTRA_THREAT_LEVEL, 1)
        val lockMinutes = intent.getLongExtra(EXTRA_LOCK_MINUTES, 0L)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: alertEngine.randomMessage()
        val blockedBanner = intent.getBooleanExtra(EXTRA_BLOCKED_BANNER, false)
        val explicitEndMs = intent.getLongExtra(EXTRA_LOCK_END_MS, 0L)
        val isLockScreen = intent.getBooleanExtra(EXTRA_IS_LOCK_SCREEN, lockMinutes > 0)

        val lockEndMs = when {
            explicitEndMs > 0 -> explicitEndMs
            lockMinutes > 0 -> System.currentTimeMillis() + lockMinutes * 60_000L
            else -> 0L
        }

        setContent {
            GuardianTheme(darkTheme = true) {
                BackHandler { }
                LockOverlayScreen(
                    threatLevel = threatLevel,
                    lockEndMs = lockEndMs,
                    initialMessage = message,
                    blockedBanner = blockedBanner,
                    isLockScreen = isLockScreen,
                    onMessageOnlyExpired = { if (!isLockScreen) finish() },
                    onLockExpired = {
                        lifecycleScope.launch {
                            lockEngine.releaseLock()
                            finish()
                        }
                    },
                    onAnalyzeUnlock = { text, stamps ->
                        guardianAI.analyzeUnlockRequestDetailed(text, stamps, maxOf(0L, lockEndMs - System.currentTimeMillis()))
                    },
                    hourlyMessages = alertEngine.hourlyLockMessages
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) return true
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun LockOverlayScreen(
    threatLevel: Int,
    lockEndMs: Long,
    initialMessage: String,
    blockedBanner: Boolean,
    isLockScreen: Boolean,
    onMessageOnlyExpired: () -> Unit,
    onLockExpired: () -> Unit,
    onAnalyzeUnlock: (String, List<Long>) -> GuardianAI.AnalysisReport,
    hourlyMessages: List<String>
) {
    val darkBg = Color(0xFF0D1117)
    val goldColor = Gold
    val softGold = Color(0xFFE8C97A)
    val white = Color(0xFFF5F5F5)

    var currentMessage by remember { mutableStateOf(initialMessage) }
    var remainingMs by remember { mutableLongStateOf(maxOf(0L, lockEndMs - System.currentTimeMillis())) }
    var msgCountdown by remember { mutableIntStateOf(30) }
    var showUnlock by remember { mutableStateOf(false) }
    var unlockText by remember { mutableStateOf("") }
    var unlockResponse by remember { mutableStateOf("") }
    val wordTimestamps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(lockEndMs, isLockScreen) {
        while (isActive) {
            delay(1_000)
            remainingMs = maxOf(0L, lockEndMs - System.currentTimeMillis())
            if (!isLockScreen && msgCountdown > 0) {
                msgCountdown--
                if (msgCountdown == 0) {
                    onMessageOnlyExpired()
                    break
                }
            }
            if (isLockScreen && lockEndMs > 0 && remainingMs <= 0) {
                onLockExpired()
                break
            }
        }
    }

    LaunchedEffect(isLockScreen) {
        if (!isLockScreen) return@LaunchedEffect
        var index = 0
        while (isActive) {
            delay(3_600_000L)
            index = (index + 1) % hourlyMessages.size
            currentMessage = hourlyMessages[index]
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(darkBg, Color(0xFF111820), Color(0xFF0A0D12))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (blockedBanner) {
                Text(
                    text = "هذا المحتوى محجوب",
                    color = Color(0xFFE57373),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            Text(text = "🛡", fontSize = 56.sp, modifier = Modifier.alpha(pulseAlpha))

            Text(
                text = currentMessage,
                color = goldColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = 44.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(pulseAlpha)
            )

            if (!isLockScreen && msgCountdown > 0) {
                Text(
                    text = "$msgCountdown ثانية",
                    color = white.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }

            if (isLockScreen && lockEndMs > 0) {
                Divider(color = goldColor.copy(alpha = 0.3f))
                Text(
                    text = formatDuration(remainingMs),
                    color = softGold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الوقت المتبقي حتى انتهاء القفل",
                    color = white.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            if (isLockScreen && threatLevel >= 2) {
                if (!showUnlock) {
                    TextButton(onClick = { showUnlock = true }) {
                        Text(
                            "طلب فتح القفل",
                            color = white.copy(alpha = 0.35f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    UnlockRequestSection(
                        text = unlockText,
                        onTextChange = { newText ->
                            if (newText.length > unlockText.length) {
                                wordTimestamps.add(System.currentTimeMillis())
                            }
                            unlockText = newText
                        },
                        response = unlockResponse,
                        onSubmit = {
                            val report = onAnalyzeUnlock(unlockText, wordTimestamps.toList())
                            unlockResponse = report.responseMessage
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockRequestSection(
    text: String,
    onTextChange: (String) -> Unit,
    response: String,
    onSubmit: () -> Unit
) {
    val goldColor = Gold
    val white = Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "اكتب سبب طلب الفتح:",
            color = white.copy(alpha = 0.7f),
            fontSize = 13.sp,
            style = TextStyle(textDirection = TextDirection.Rtl)
        )
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(color = white, fontSize = 14.sp, textDirection = TextDirection.Rtl),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .heightIn(min = 80.dp)
        )
        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = goldColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إرسال", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
        }
        if (response.isNotEmpty()) {
            Text(
                text = response,
                color = goldColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}
