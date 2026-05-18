package com.example.doshka.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doshka_prefs")

/**
 * Менеджер для роботи з DataStore Preferences
 */
@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Ключі для токенів
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val TOKEN_EXPIRY = longPreferencesKey("token_expiry")

        // Ключі для користувача
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val TEAM_ID = stringPreferencesKey("team_id")

        // Ключі для налаштувань
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val DARK_THEME = booleanPreferencesKey("dark_theme")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val CURRENT_BOARD_ID = stringPreferencesKey("current_board_id")

        // Ключі для офлайн режиму
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val IS_OFFLINE_MODE = booleanPreferencesKey("is_offline_mode")

        // Ключ для адреси сервера
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val SERVER_URL_CONFIGURED = booleanPreferencesKey("server_url_configured")

        // Дефолтні URL для різних пристроїв
        const val EMULATOR_SERVER_URL = "http://10.0.2.2:8000/"
        const val DEFAULT_SERVER_URL = "http://127.0.0.1:8000/"
    }

    // Токени
    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[REFRESH_TOKEN] }
    val tokenExpiry: Flow<Long> = dataStore.data.map { it[TOKEN_EXPIRY] ?: 0L }

    // Користувач
    val userId: Flow<String?> = dataStore.data.map { it[USER_ID] }
    val userEmail: Flow<String?> = dataStore.data.map { it[USER_EMAIL] }
    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME] }
    val userRole: Flow<String?> = dataStore.data.map { it[USER_ROLE] }
    val teamId: Flow<String?> = dataStore.data.map { it[TEAM_ID] }

    // Налаштування
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val darkTheme: Flow<Boolean> = dataStore.data.map { it[DARK_THEME] ?: false }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val currentBoardId: Flow<String?> = dataStore.data.map { it[CURRENT_BOARD_ID] }

    // Офлайн
    val lastSyncTime: Flow<Long> = dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }
    val isOfflineMode: Flow<Boolean> = dataStore.data.map { it[IS_OFFLINE_MODE] ?: false }

    // Адреса сервера
    val serverUrl: Flow<String> = dataStore.data.map { it[SERVER_URL] ?: DEFAULT_SERVER_URL }
    val isServerUrlConfigured: Flow<Boolean> = dataStore.data.map { it[SERVER_URL_CONFIGURED] ?: false }

    // Методи збереження токенів
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresIn * 1000)
        }
    }

    suspend fun clearTokens() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(TOKEN_EXPIRY)
        }
    }

    // Методи для користувача
    suspend fun saveUser(id: String, email: String, name: String, role: String, teamId: String?) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USER_EMAIL] = email
            prefs[USER_NAME] = name
            prefs[USER_ROLE] = role
            if (teamId != null) {
                prefs[TEAM_ID] = teamId
            } else {
                prefs.remove(TEAM_ID)
            }
        }
    }

    suspend fun clearUser() {
        dataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_NAME)
            prefs.remove(USER_ROLE)
            prefs.remove(TEAM_ID)
        }
    }

    // Методи для налаштувань
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DARK_THEME] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setCurrentBoardId(boardId: String?) {
        dataStore.edit { prefs ->
            if (boardId != null) {
                prefs[CURRENT_BOARD_ID] = boardId
            } else {
                prefs.remove(CURRENT_BOARD_ID)
            }
        }
    }

    // Методи для офлайн режиму
    suspend fun setLastSyncTime(time: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIME] = time
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_OFFLINE_MODE] = enabled
        }
    }

    // Адреса сервера
    suspend fun setServerUrl(url: String) {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        dataStore.edit { prefs ->
            prefs[SERVER_URL] = normalizedUrl
            prefs[SERVER_URL_CONFIGURED] = true
        }
    }

    // Повне очищення
    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
