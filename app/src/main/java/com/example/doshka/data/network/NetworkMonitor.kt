package com.example.doshka.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Монітор стану мережі
 * Відстежує підключення та автоматично тригерить синхронізацію
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(getCurrentNetworkType())
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    /**
     * Flow що емітить зміни стану мережі
     */
    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("Мережа доступна")
                _isOnline.value = true
                _networkType.value = getCurrentNetworkType()
                trySend(NetworkStatus.Available(getCurrentNetworkType()))
            }

            override fun onLost(network: Network) {
                Timber.d("Мережа втрачена")
                _isOnline.value = false
                _networkType.value = NetworkType.NONE
                trySend(NetworkStatus.Lost)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val type = getNetworkType(networkCapabilities)
                _networkType.value = type
                trySend(NetworkStatus.Available(type))
            }

            override fun onUnavailable() {
                Timber.d("Мережа недоступна")
                _isOnline.value = false
                _networkType.value = NetworkType.NONE
                trySend(NetworkStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Емітимо поточний стан при підписці
        trySend(
            if (checkCurrentConnectivity()) {
                NetworkStatus.Available(getCurrentNetworkType())
            } else {
                NetworkStatus.Unavailable
            }
        )

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Перевіряє поточний стан підключення
     */
    fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Отримує поточний тип мережі
     */
    private fun getCurrentNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        return getNetworkType(capabilities)
    }

    private fun getNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * Перевіряє чи є швидке з'єднання (WiFi або Ethernet)
     */
    fun isFastConnection(): Boolean {
        return networkType.value in listOf(NetworkType.WIFI, NetworkType.ETHERNET)
    }
}

/**
 * Типи мережевого з'єднання
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

/**
 * Стан мережі
 */
sealed class NetworkStatus {
    data class Available(val type: NetworkType) : NetworkStatus()
    data object Lost : NetworkStatus()
    data object Unavailable : NetworkStatus()
}
