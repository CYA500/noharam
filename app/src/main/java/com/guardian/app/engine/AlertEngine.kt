package com.guardian.app.engine

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.guardian.app.lock.LockOverlayActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AlertEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val islamicMessages: List<String> = listOf(
        "أما علمت أن الله يراك؟",
        "اصبر، موعدك الجنة",
        "الحور العين تنتظرك فلا تيأس",
        "وَلَا يُلَقَّاهَا إِلَّا الصَّابِرُونَ",
        "غُضَّ بَصَرَك يَرحَمكَ الله",
        "كُن مع الله يَكُن معك",
        "إن الله مع الصابرين"
    )

    val hourlyLockMessages: List<String> = islamicMessages + listOf(
        "﴿وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ﴾",
        "﴿إِنَّ اللهَ لَا يُضِيعُ أَجْرَ المُحسِنِينَ﴾",
        "﴿وَمَن يَتَّقِ اللهَ يَجعَل لَهُ مَخرَجاً﴾"
    )

    fun randomMessage(): String = islamicMessages.random()

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    fun vibrateLevel1() {
        val v = vibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(500)
        }
    }

    fun vibrateLevel3() {
        val v = vibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 500), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 300, 100, 300, 100, 500), -1)
        }
    }

    fun playAlertSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        ringtone.play()
    }

    fun showOverlay(
        threatLevel: Int,
        lockDurationMinutes: Long = 0L,
        message: String = randomMessage(),
        blockedBanner: Boolean = false,
        lockEndMs: Long = 0L
    ) {
        val intent = Intent(context, LockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockOverlayActivity.EXTRA_THREAT_LEVEL, threatLevel)
            putExtra(LockOverlayActivity.EXTRA_LOCK_MINUTES, lockDurationMinutes)
            putExtra(LockOverlayActivity.EXTRA_MESSAGE, message)
            putExtra(LockOverlayActivity.EXTRA_BLOCKED_BANNER, blockedBanner)
            putExtra(LockOverlayActivity.EXTRA_LOCK_END_MS, lockEndMs)
            putExtra(
                LockOverlayActivity.EXTRA_IS_LOCK_SCREEN,
                lockDurationMinutes > 0L || lockEndMs > System.currentTimeMillis()
            )
        }
        context.startActivity(intent)
    }

    fun triggerLevel1() {
        vibrateLevel1()
        showOverlay(threatLevel = 1, lockDurationMinutes = 0L)
    }

    fun triggerLevel2(lockMinutes: Long = Random.nextLong(30, 121)) {
        vibrateLevel1()
        showOverlay(
            threatLevel = 2,
            lockDurationMinutes = lockMinutes,
            blockedBanner = true
        )
    }

    fun triggerLevel3(lockMinutes: Long = Random.nextLong(5 * 60, 24 * 60 + 1)) {
        vibrateLevel3()
        playAlertSound()
        showOverlay(threatLevel = 3, lockDurationMinutes = lockMinutes)
    }
}
