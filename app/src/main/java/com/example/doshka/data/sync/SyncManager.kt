package com.example.doshka.data.sync

import android.content.Context
import androidx.work.*
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.local.dao.PendingOperationDao
import com.example.doshka.data.local.entity.PendingOperationEntity
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.network.NetworkStatus
import com.example.doshka.data.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер синхронізації - координує всі операції синхронізації
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val pendingOperationDao: PendingOperationDao,
    private val dataStoreManager: DataStoreManager,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingOperationsCount = MutableStateFlow(0)
    val pendingOperationsCount: StateFlow<Int> = _pendingOperationsCount.asStateFlow()

    init {
        // Слідкуємо за змінами мережі
        scope.launch {
            networkMonitor.networkStatus.collect { status ->
                when (status) {
                    is NetworkStatus.Available -> {
                        Timber.d("Мережа доступна, запускаємо синхронізацію")
                        triggerSync()
                    }
                    is NetworkStatus.Lost, NetworkStatus.Unavailable -> {
                        Timber.d("Мережа недоступна, працюємо офлайн")
                        _syncState.value = SyncState.Offline
                    }
                }
            }
        }

        // Слідкуємо за кількістю pending операцій
        scope.launch {
            pendingOperationDao.observePendingCount().collect { count ->
                _pendingOperationsCount.value = count
            }
        }

        // Запускаємо періодичну синхронізацію
        setupPeriodicSync()
    }

    /**
     * Налаштовує періодичну синхронізацію
     */
    private fun setupPeriodicSync() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Тригерить негайну синхронізацію
     */
    fun triggerSync() {
        if (!networkMonitor.isOnline.value) {
            Timber.d("Неможливо синхронізувати - немає мережі")
            return
        }

        _syncState.value = SyncState.Syncing

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )

        // Слідкуємо за результатом
        scope.launch {
            workManager.getWorkInfoByIdFlow(oneTimeWorkRequest.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _syncState.value = SyncState.Synced
                        Timber.d("Синхронізація успішна")
                    }
                    WorkInfo.State.FAILED -> {
                        _syncState.value = SyncState.Error("Помилка синхронізації")
                        Timber.e("Синхронізація невдала")
                    }
                    WorkInfo.State.RUNNING -> {
                        _syncState.value = SyncState.Syncing
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Додає операцію до черги синхронізації
     * @param payloadJson - серіалізований JSON payload (використовуйте json.encodeToString())
     */
    suspend fun queueOperation(
        operationType: OperationType,
        entityType: EntityType,
        entityId: String,
        payloadJson: String
    ) {
        val operation = PendingOperationEntity(
            operationType = operationType.name.lowercase(),
            entityType = entityType.name.lowercase(),
            entityId = entityId,
            payload = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        pendingOperationDao.insert(operation)
        Timber.d("Операція додана до черги: ${operationType.name} ${entityType.name} $entityId")

        // Якщо є мережа - тригеримо синхронізацію
        if (networkMonitor.isOnline.value) {
            triggerSync()
        }
    }

    /**
     * Виконує операцію з автоматичним fallback на offline
     * Повертає true якщо операція виконана онлайн, false якщо збережена для офлайн
     * @param payloadJson - серіалізований JSON payload
     */
    suspend fun <R> executeWithSync(
        operationType: OperationType,
        entityType: EntityType,
        entityId: String,
        payloadJson: String,
        onlineAction: suspend () -> R,
        offlineAction: suspend () -> R
    ): SyncResult<R> {
        return if (networkMonitor.isOnline.value) {
            try {
                val result = onlineAction()
                SyncResult.Online(result)
            } catch (e: Exception) {
                Timber.w(e, "Онлайн операція невдала, зберігаємо офлайн")
                queueOperation(operationType, entityType, entityId, payloadJson)
                val result = offlineAction()
                SyncResult.Offline(result)
            }
        } else {
            queueOperation(operationType, entityType, entityId, payloadJson)
            val result = offlineAction()
            SyncResult.Offline(result)
        }
    }

    /**
     * Отримує час останньої синхронізації
     */
    fun getLastSyncTime(): Flow<Long> = dataStoreManager.lastSyncTime

    /**
     * Скасовує всі pending операції
     */
    suspend fun cancelAllPendingOperations() {
        pendingOperationDao.deleteAllPending()
    }

    /**
     * Скасовує операції для конкретної сутності
     */
    suspend fun cancelOperationsForEntity(entityType: EntityType, entityId: String) {
        pendingOperationDao.deleteByEntity(entityType.name.lowercase(), entityId)
    }

    companion object {
        const val PERIODIC_SYNC_WORK_NAME = "doshka_periodic_sync"
        const val ONE_TIME_SYNC_WORK_NAME = "doshka_one_time_sync"
    }
}

/**
 * Типи операцій синхронізації
 */
enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    MOVE
}

/**
 * Типи сутностей
 */
enum class EntityType {
    TASK,
    BOARD,
    COLUMN,
    MESSAGE,
    ATTACHMENT,
    LABEL
}

/**
 * Стан синхронізації
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Synced : SyncState()
    data object Offline : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Результат операції з інформацією про онлайн/офлайн
 */
sealed class SyncResult<T> {
    data class Online<T>(val data: T) : SyncResult<T>()
    data class Offline<T>(val data: T) : SyncResult<T>()

    val value: T
        get() = when (this) {
            is Online -> data
            is Offline -> data
        }

    val isOnline: Boolean
        get() = this is Online
}
