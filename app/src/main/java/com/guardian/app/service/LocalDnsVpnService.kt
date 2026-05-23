package com.guardian.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.guardian.app.R
import com.guardian.app.engine.KeywordEngine
import com.guardian.app.lock.LockEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class LocalDnsVpnService : VpnService() {

    @Inject lateinit var keywordEngine: KeywordEngine

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var packetJob: Job? = null
    private var fullBlockMode: Boolean = false

    companion object {
        const val MODE_DNS_ONLY = "dns_only"
        const val MODE_FULL_BLOCK = "full_block"
        private const val EXTRA_MODE = "vpn_mode"
        private const val CHANNEL_ID = "guardian_vpn_channel"
        private const val NOTIF_ID = 1002
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val VPN_ADDRESS = "10.0.0.2"

        fun start(context: Context, mode: String = MODE_DNS_ONLY) {
            val intent = Intent(context, LocalDnsVpnService::class.java).apply {
                putExtra(EXTRA_MODE, mode)
            }
            context.startForegroundService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fullBlockMode = intent?.getStringExtra(EXTRA_MODE) == MODE_FULL_BLOCK
        setupVpnInterface()
        startPacketProcessing()
        return START_STICKY
    }

    private fun setupVpnInterface() {
        val builder = Builder()
            .addAddress(VPN_ADDRESS, 32)
            .addDnsServer(UPSTREAM_DNS)
            .setSession("الحارس VPN")
            .setBlocking(false)

        if (fullBlockMode) {
            builder.addRoute("0.0.0.0", 0)
            for (pkg in LockEngine.ALWAYS_ALLOWED_PACKAGES) {
                try {
                    builder.addAllowedApplication(pkg)
                } catch (_: Exception) { }
            }
            try {
                builder.addAllowedApplication(packageName)
            } catch (_: Exception) { }
        } else {
            builder.addRoute(UPSTREAM_DNS, 32)
        }

        vpnInterface = builder.establish()
    }

    private fun startPacketProcessing() {
        val pfd = vpnInterface ?: return
        val inStream = FileInputStream(pfd.fileDescriptor)
        val outStream = FileOutputStream(pfd.fileDescriptor)

        packetJob = scope.launch {
            val packet = ByteBuffer.allocate(32767)
            while (isActive) {
                packet.clear()
                val length = inStream.channel.read(packet)
                if (length <= 0) continue
                packet.flip()
                val raw = ByteArray(packet.limit())
                packet.get(raw)
                val processed = processPacket(raw)
                if (processed != null) {
                    withContext(Dispatchers.IO) { outStream.write(processed) }
                }
            }
        }
    }

    private suspend fun processPacket(raw: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (raw.size < 20) return@withContext if (fullBlockMode) null else null
            val protocol = raw[9].toInt() and 0xFF
            if (protocol != 17) return@withContext if (fullBlockMode) null else forwardRaw(raw)

            val ipHeaderLen = (raw[0].toInt() and 0xF) * 4
            if (raw.size < ipHeaderLen + 8) return@withContext null
            val destPort = ((raw[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                (raw[ipHeaderLen + 3].toInt() and 0xFF)
            if (destPort != DNS_PORT) return@withContext if (fullBlockMode) null else forwardRaw(raw)

            val dnsOffset = ipHeaderLen + 8
            if (raw.size <= dnsOffset) return@withContext null
            val dnsPayload = raw.copyOfRange(dnsOffset, raw.size)
            val domain = parseDnsQueryName(dnsPayload) ?: return@withContext forwardRaw(raw)

            if (keywordEngine.isBlockedUrl(domain)) {
                return@withContext buildNxdomainResponse(raw, dnsPayload, ipHeaderLen)
            }
            return@withContext forwardDns(raw, dnsPayload, ipHeaderLen)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDnsQueryName(dns: ByteArray): String? = try {
        val sb = StringBuilder()
        var i = 12
        while (i < dns.size) {
            val len = dns[i].toInt() and 0xFF
            if (len == 0) break
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(dns, i + 1, len, Charsets.US_ASCII))
            i += len + 1
        }
        sb.toString().lowercase()
    } catch (_: Exception) {
        null
    }

    private fun buildNxdomainResponse(original: ByteArray, dnsQuery: ByteArray, ipHeaderLen: Int): ByteArray {
        val response = dnsQuery.copyOf()
        if (response.size >= 4) {
            response[2] = 0x84.toByte()
            response[3] = 0x03.toByte()
        }
        return rebuildPacket(original, response, ipHeaderLen, swapSrcDst = true)
    }

    private suspend fun forwardDns(original: ByteArray, dnsQuery: ByteArray, ipHeaderLen: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                protect(socket)
                socket.send(
                    DatagramPacket(
                        dnsQuery, dnsQuery.size,
                        InetAddress.getByName(UPSTREAM_DNS), DNS_PORT
                    )
                )
                val buf = ByteArray(512)
                val reply = DatagramPacket(buf, buf.size)
                socket.soTimeout = 3000
                socket.receive(reply)
                socket.close()
                rebuildPacket(original, buf.copyOf(reply.length), ipHeaderLen, swapSrcDst = true)
            } catch (_: Exception) {
                null
            }
        }

    private fun forwardRaw(raw: ByteArray): ByteArray = raw

    private fun rebuildPacket(
        original: ByteArray,
        dnsPayload: ByteArray,
        ipHeaderLen: Int,
        swapSrcDst: Boolean
    ): ByteArray {
        val udpLen = 8 + dnsPayload.size
        val totalLen = ipHeaderLen + udpLen
        val packet = ByteArray(totalLen)
        System.arraycopy(original, 0, packet, 0, ipHeaderLen)
        packet[2] = (totalLen shr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        if (swapSrcDst) {
            System.arraycopy(original, 12, packet, 16, 4)
            System.arraycopy(original, 16, packet, 12, 4)
        }
        packet[ipHeaderLen] = original[ipHeaderLen + 2]
        packet[ipHeaderLen + 1] = original[ipHeaderLen + 3]
        packet[ipHeaderLen + 2] = original[ipHeaderLen]
        packet[ipHeaderLen + 3] = original[ipHeaderLen + 1]
        packet[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        packet[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        System.arraycopy(dnsPayload, 0, packet, ipHeaderLen + 8, dnsPayload.size)
        return packet
    }

    override fun onDestroy() {
        packetJob?.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }

    override fun onRevoke() {
        vpnInterface?.close()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_guardian_shield)
            .setContentTitle(getString(R.string.vpn_active_title))
            .setContentText(getString(R.string.vpn_active_body))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}
