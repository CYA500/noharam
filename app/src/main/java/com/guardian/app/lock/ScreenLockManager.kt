package com.guardian.app.lock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.guardian.app.receiver.GuardianDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)

    fun bringLockToFront(level: Int, remainingMs: Long, message: String, lockEndMs: Long) {
        val intent = Intent(context, LockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LockOverlayActivity.EXTRA_THREAT_LEVEL, level)
            putExtra(LockOverlayActivity.EXTRA_MESSAGE, message)
            putExtra(LockOverlayActivity.EXTRA_LOCK_END_MS, lockEndMs)
            putExtra(LockOverlayActivity.EXTRA_IS_LOCK_SCREEN, true)
        }
        context.startActivity(intent)
    }

    fun isDeviceAdminActive(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent)

    fun lockScreenNow() {
        if (isDeviceAdminActive()) {
            devicePolicyManager.lockNow()
        }
    }

    fun buildFullScreenLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
    }
}
