package com.example.doshka.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.sync.SyncManager
import com.example.doshka.data.sync.SyncState
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.network.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для керування синхронізацією
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    /**
     * Поточний стан синхронізації
     */
    val syncState: StateFlow<SyncState> = syncManager.syncState

    /**
     * Кількість операцій в черзі
     */
    val pendingOperationsCount: StateFlow<Int> = syncManager.pendingOperationsCount

    /**
     * Чи є підключення до мережі
     */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    /**
     * Комбінований стан для UI
     */
    val uiState: StateFlow<SyncUiState> = combine(
        syncState,
        pendingOperationsCount,
        isOnline
    ) { state, pending, online ->
        SyncUiState(
            syncState = state,
            pendingCount = pending,
            isOnline = online
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncUiState()
    )

    /**
     * Час останньої синхронізації
     */
    val lastSyncTime: Flow<Long> = syncManager.getLastSyncTime()

    init {
        // Слідкуємо за змінами мережі для автоматичного показу статусу
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                when (status) {
                    is NetworkStatus.Available -> {
                        // Мережа з'явилась - можна показати повідомлення
                    }
                    is NetworkStatus.Lost, NetworkStatus.Unavailable -> {
                        // Мережа пропала
                    }
                }
            }
        }
    }

    /**
     * Запускає ручну синхронізацію
     */
    fun triggerSync() {
        syncManager.triggerSync()
    }

    /**
     * Скасовує всі операції в черзі
     */
    fun cancelPendingOperations() {
        viewModelScope.launch {
            syncManager.cancelAllPendingOperations()
        }
    }
}

/**
 * UI стан синхронізації
 */
data class SyncUiState(
    val syncState: SyncState = SyncState.Idle,
    val pendingCount: Int = 0,
    val isOnline: Boolean = true
) {
    val showSyncBanner: Boolean
        get() = syncState is SyncState.Syncing ||
                syncState is SyncState.Offline ||
                syncState is SyncState.Error ||
                pendingCount > 0

    val statusMessage: String
        get() = when {
            syncState is SyncState.Syncing -> "Синхронізація..."
            syncState is SyncState.Offline && pendingCount > 0 -> "Офлайн · $pendingCount змін"
            syncState is SyncState.Offline -> "Офлайн режим"
            syncState is SyncState.Error -> (syncState as SyncState.Error).message
            pendingCount > 0 -> "$pendingCount змін очікують"
            else -> "Синхронізовано"
        }
}
