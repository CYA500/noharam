package com.guardian.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guardian.app.di.BootReceiverEntryPoint
import com.guardian.app.lock.LockForegroundService
import com.guardian.app.service.LocalDnsVpnService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pending = goAsync()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BootReceiverEntryPoint::class.java
                )
                val lockEngine = entryPoint.lockEngine()
                val state = lockEngine.lockStateFlow.first()
                if (state.isLocked && !state.isExpired && state.lockEndMs > System.currentTimeMillis()) {
                    val serviceIntent = Intent(context, LockForegroundService::class.java).apply {
                        putExtra(LockForegroundService.EXTRA_LEVEL, state.level)
                        putExtra(LockForegroundService.EXTRA_END_MS, state.lockEndMs)
                        putExtra(LockForegroundService.EXTRA_MESSAGE, state.message)
                    }
                    context.startForegroundService(serviceIntent)
                    val vpnMode = if (state.level >= 3) {
                        LocalDnsVpnService.MODE_FULL_BLOCK
                    } else {
                        LocalDnsVpnService.MODE_DNS_ONLY
                    }
                    LocalDnsVpnService.start(context, vpnMode)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
