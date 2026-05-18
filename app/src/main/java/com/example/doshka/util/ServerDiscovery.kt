package com.example.doshka.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * Автоматичне виявлення сервера в локальній мережі
 */
object ServerDiscovery {
    private const val TAG = "ServerDiscovery"
    private const val DISCOVERY_PORT = 9999
    private const val DISCOVERY_MESSAGE = "DOSHKA_DISCOVER"
    private const val SERVER_PORT = 8000
    private const val TIMEOUT_MS = 1500

    data class ServerInfo(
        val ip: String,
        val port: Int,
        val url: String
    )

    /**
     * Комплексний пошук сервера - пробує всі методи
     */
    suspend fun discoverWithRetry(maxAttempts: Int = 3, context: Context? = null): ServerInfo? {
        Log.d(TAG, "Starting server discovery...")

        // 1. Спробувати UDP broadcast
        repeat(2) {
            val result = discoverViaUdp()
            if (result != null) {
                Log.d(TAG, "Found via UDP: ${result.url}")
                return result
            }
        }

        // 2. Сканувати локальну мережу
        val subnetResult = scanLocalNetwork(context)
        if (subnetResult != null) {
            Log.d(TAG, "Found via subnet scan: ${subnetResult.url}")
            return subnetResult
        }

        Log.d(TAG, "Server not found")
        return null
    }

    /**
     * UDP broadcast discovery
     */
    private suspend fun discoverViaUdp(): ServerInfo? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = TIMEOUT_MS
            }

            val message = DISCOVERY_MESSAGE.toByteArray()
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(message, message.size, broadcastAddress, DISCOVERY_PORT)

            socket.send(sendPacket)

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)

            withTimeoutOrNull(TIMEOUT_MS.toLong()) {
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length)
                parseUdpResponse(response)
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP discovery failed: ${e.message}")
            null
        } finally {
            socket?.close()
        }
    }

    /**
     * Сканує локальну мережу на наявність сервера
     */
    private suspend fun scanLocalNetwork(context: Context?): ServerInfo? = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet(context) ?: return@withContext null
        Log.d(TAG, "Scanning subnet: $subnet.*")

        val foundServer = AtomicReference<ServerInfo?>(null)

        // Пріоритетні адреси (часто використовуються для комп'ютерів)
        val priorityHosts = listOf(1, 2, 100, 101, 102, 103, 104, 105, 50, 51, 52)

        // Спочатку швидко перевіряємо пріоритетні
        val priorityJobs = priorityHosts.map { host ->
            async {
                if (foundServer.get() == null) {
                    val ip = "$subnet.$host"
                    if (checkServer(ip)) {
                        foundServer.compareAndSet(null, ServerInfo(ip, SERVER_PORT, "http://$ip:$SERVER_PORT/"))
                    }
                }
            }
        }
        priorityJobs.awaitAll()

        if (foundServer.get() != null) {
            return@withContext foundServer.get()
        }

        // Якщо не знайдено - скануємо решту (паралельно, batch по 20)
        val remainingHosts = (1..254).filter { it !in priorityHosts }

        for (batch in remainingHosts.chunked(30)) {
            if (foundServer.get() != null) break

            val jobs = batch.map { host ->
                async {
                    if (foundServer.get() == null) {
                        val ip = "$subnet.$host"
                        if (checkServer(ip)) {
                            foundServer.compareAndSet(null, ServerInfo(ip, SERVER_PORT, "http://$ip:$SERVER_PORT/"))
                        }
                    }
                }
            }
            jobs.awaitAll()
        }

        foundServer.get()
    }

    /**
     * Перевіряє чи сервер доступний за вказаною IP
     */
    private fun checkServer(ip: String): Boolean {
        return try {
            val url = URL("http://$ip:$SERVER_PORT/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 300
            connection.readTimeout = 300
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 200) {
                Log.d(TAG, "Server found at $ip")
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Отримує subnet локальної мережі (напр. "192.168.1")
     */
    private fun getLocalSubnet(context: Context?): String? {
        return try {
            if (context != null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val ipInt = wifiInfo?.ipAddress ?: 0

                if (ipInt != 0) {
                    val ip = String.format(
                        "%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff
                    )
                    Log.d(TAG, "Device subnet: $ip")
                    return ip
                }
            }

            // Fallback - типові домашні мережі
            "192.168.1"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get subnet: ${e.message}")
            "192.168.1"
        }
    }

    private fun parseUdpResponse(response: String): ServerInfo? {
        return try {
            val json = JSONObject(response)
            if (json.optString("service") == "doshka") {
                ServerInfo(
                    ip = json.getString("ip"),
                    port = json.getInt("port"),
                    url = json.getString("url")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
