package com.terralink.iotfarm.data.network

import android.content.Context
import android.net.wifi.WifiManager
import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.debug.LogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugLogManager: DebugLogManager
) {

    companion object {
        private const val DISCOVERY_PORT = 8888
        private const val DISCOVERY_REQUEST = "DISCOVER_TERRALINK_FARM"
        private const val DISCOVERY_RESPONSE_PREFIX = "TERRALINK_ACK"
        private const val TIMEOUT_MS = 3000
    }

    suspend fun discoverFarmIp(): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        var multicastLock: WifiManager.MulticastLock? = null
        try {
            // Acquire MulticastLock to ensure UDP broadcast/multicast packets are allowed by Android OS
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("TerraLinkUdpLock")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = TIMEOUT_MS
            }

            val requestData = DISCOVERY_REQUEST.toByteArray()
            debugLogManager.log(LogEntry.LogType.UDP, "Starting UDP broadcast discovery on port $DISCOVERY_PORT")

            // 1. Send to 255.255.255.255 (Global Broadcast)
            try {
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(requestData, requestData.size, broadcastAddress, DISCOVERY_PORT)
                socket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Send to 10.0.2.2 (Android Emulator Host Gateway)
            try {
                val emulatorHostAddress = InetAddress.getByName("10.0.2.2")
                val emulatorPacket = DatagramPacket(requestData, requestData.size, emulatorHostAddress, DISCOVERY_PORT)
                socket.send(emulatorPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            val response = String(receivePacket.data, 0, receivePacket.length).trim()
            val respondedIp = receivePacket.address.hostAddress

            debugLogManager.log(LogEntry.LogType.UDP, "Received UDP response from $respondedIp: $response")

            if (response.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                if (respondedIp == "127.0.0.1" || respondedIp == "0.0.0.0" || respondedIp == "localhost") {
                    "10.0.2.2"
                } else {
                    respondedIp ?: "10.0.2.2"
                }
            } else {
                null
            }
        } catch (e: SocketTimeoutException) {
            debugLogManager.log(LogEntry.LogType.UDP, "UDP discovery timed out.")
            null
        } catch (e: Exception) {
            debugLogManager.log(LogEntry.LogType.UDP, "UDP discovery error: ${e.message}")
            null
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            socket?.close()
        }
    }
}
