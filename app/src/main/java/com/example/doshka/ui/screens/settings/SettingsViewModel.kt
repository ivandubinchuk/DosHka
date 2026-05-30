package com.example.doshka.ui.screens.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.util.DeviceUtils
import com.example.doshka.util.ServerDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val isServerConfigured: Boolean = true,
    val isDiscovering: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
    val darkTheme: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isGeneratingQR: Boolean = false,
    val showQRDialog: Boolean = false,
    val qrCodeBase64: String? = null,
    val qrExpiresAt: String? = null,
    val showAddExecutorDialog: Boolean = false,
    val newExecutorEmail: String = ""
)

enum class ConnectionStatus {
    Unknown,
    Testing,
    Success,
    Failed
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val api: DoshkaApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        autoDiscoverServer()
    }

    /**
     * Автоматичне виявлення сервера при запуску
     */
    private fun autoDiscoverServer() {
        viewModelScope.launch {
            val isConfigured = dataStoreManager.isServerUrlConfigured.first()

            if (isConfigured) {
                // Сервер вже налаштований — перевіряємо з'єднання
                testConnection()
                return@launch
            }

            // Для емулятора — фіксована адреса
            if (DeviceUtils.isEmulator()) {
                Log.d("SettingsVM", "Emulator detected, using 10.0.2.2")
                dataStoreManager.setServerUrl(DataStoreManager.EMULATOR_SERVER_URL)
                _uiState.update { it.copy(isServerConfigured = true) }
                return@launch
            }

            // Для реального пристрою — автовиявлення
            Log.d("SettingsVM", "Starting auto-discovery...")
            _uiState.update { it.copy(isDiscovering = true) }

            val serverInfo = ServerDiscovery.discoverWithRetry(3, context)

            if (serverInfo != null) {
                Log.d("SettingsVM", "Server found: ${serverInfo.url}")
                dataStoreManager.setServerUrl(serverInfo.url)
                _uiState.update {
                    it.copy(
                        serverUrl = serverInfo.url,
                        isServerConfigured = true,
                        isDiscovering = false,
                        connectionStatus = ConnectionStatus.Success
                    )
                }
            } else {
                Log.d("SettingsVM", "Server not found")
                _uiState.update {
                    it.copy(
                        isServerConfigured = false,
                        isDiscovering = false
                    )
                }
            }
        }
    }

    /**
     * Повторний пошук сервера вручну
     */
    fun rediscoverServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, connectionStatus = ConnectionStatus.Unknown) }

            val serverInfo = ServerDiscovery.discoverWithRetry(3, context)

            if (serverInfo != null) {
                dataStoreManager.setServerUrl(serverInfo.url)
                _uiState.update {
                    it.copy(
                        serverUrl = serverInfo.url,
                        isServerConfigured = true,
                        isDiscovering = false,
                        connectionStatus = ConnectionStatus.Success
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDiscovering = false,
                        connectionStatus = ConnectionStatus.Failed
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                dataStoreManager.serverUrl,
                dataStoreManager.isServerUrlConfigured
            ) { url, configured ->
                Pair(url, configured)
            }.collect { (url, configured) ->
                _uiState.update {
                    it.copy(
                        serverUrl = url,
                        isServerConfigured = configured || DeviceUtils.isEmulator()
                    )
                }
            }
        }
        viewModelScope.launch {
            dataStoreManager.darkTheme.collect { dark ->
                _uiState.update { it.copy(darkTheme = dark) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, connectionStatus = ConnectionStatus.Unknown) }
    }

    fun saveServerUrl() {
        viewModelScope.launch {
            dataStoreManager.setServerUrl(_uiState.value.serverUrl)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionStatus = ConnectionStatus.Testing) }

            try {
                // Нормалізуємо URL - додаємо http:// якщо відсутній
                var baseUrl = _uiState.value.serverUrl.trim()
                if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                    baseUrl = "http://$baseUrl"
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = "$baseUrl/"
                }

                val healthUrl = "${baseUrl}health"

                val responseCode = withContext(Dispatchers.IO) {
                    val connection = java.net.URL(healthUrl).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Accept", "application/json")

                    try {
                        connection.responseCode
                    } finally {
                        connection.disconnect()
                    }
                }

                if (responseCode == 200) {
                    // Зберігаємо нормалізований URL
                    dataStoreManager.setServerUrl(baseUrl)
                    _uiState.update {
                        it.copy(
                            serverUrl = baseUrl,
                            isTestingConnection = false,
                            connectionStatus = ConnectionStatus.Success
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isTestingConnection = false, connectionStatus = ConnectionStatus.Failed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(isTestingConnection = false, connectionStatus = ConnectionStatus.Failed)
                }
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkTheme(enabled)
            _uiState.update { it.copy(darkTheme = enabled) }
        }
    }

    fun showAddExecutorDialog() {
        _uiState.update {
            it.copy(showAddExecutorDialog = true)
        }
    }

    fun hideAddExecutorDialog() {
        _uiState.update {
            it.copy(
                showAddExecutorDialog = false,
                newExecutorEmail = ""
            )
        }
    }

    fun updateEmail(email: String) {
        _uiState.update {
            it.copy(newExecutorEmail = email)
        }
    }
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationsEnabled(enabled)
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }
    }
    fun addExecutor() {
        viewModelScope.launch {
            try {
                val email = _uiState.value.newExecutorEmail.trim()
                if (email.isBlank()) return@launch

                api.addExecutor(email)
                hideAddExecutorDialog()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun generateInviteQR() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingQR = true) }

            try {
                val teamId = dataStoreManager.teamId.first()
                if (teamId.isNullOrBlank()) {
                    _uiState.update { it.copy(isGeneratingQR = false) }
                    return@launch
                }

                val response = api.generateInviteQR(teamId)
                if (response.isSuccessful && response.body() != null) {
                    val qrResponse = response.body()!!
                    _uiState.update {
                        it.copy(
                            isGeneratingQR = false,
                            showQRDialog = true,
                            qrCodeBase64 = qrResponse.qrCode,
                            qrExpiresAt = qrResponse.expiresAt
                        )
                    }
                } else {
                    _uiState.update { it.copy(isGeneratingQR = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isGeneratingQR = false) }
            }
        }
    }

    fun dismissQRDialog() {
        _uiState.update {
            it.copy(
                showQRDialog = false,
                qrCodeBase64 = null,
                qrExpiresAt = null
            )
        }
    }
}
