package com.example.doshka.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.local.dao.*
import com.example.doshka.data.local.entity.PendingOperationEntity
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.*
import com.example.doshka.data.sync.CreateTaskPayload
import com.example.doshka.data.sync.UpdateTaskPayload
import com.example.doshka.data.sync.MoveTaskPayload
import com.example.doshka.data.sync.SendMessagePayload
import com.example.doshka.data.sync.CreateBoardPayload
import com.example.doshka.data.sync.CreateColumnPayload
import com.example.doshka.util.DateTimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker для синхронізації офлайн-операцій з сервером
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val taskDao: TaskDao,
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val messageDao: MessageDao,
    private val api: DoshkaApi,
    private val dataStoreManager: DataStoreManager,
    private val json: Json
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.d("Запуск синхронізації офлайн-операцій")

        try {
            // Отримуємо всі pending операції
            val pendingOperations = pendingOperationDao.getPendingOperations()

            if (pendingOperations.isEmpty()) {
                Timber.d("Немає операцій для синхронізації")
                dataStoreManager.setLastSyncTime(System.currentTimeMillis())
                return@withContext Result.success()
            }

            Timber.d("Знайдено ${pendingOperations.size} операцій для синхронізації")

            var successCount = 0
            var failCount = 0

            for (operation in pendingOperations) {
                try {
                    // Позначаємо як processing
                    pendingOperationDao.updateOperationStatus(operation.id, "processing")

                    // Виконуємо операцію
                    val success = executeOperation(operation)

                    if (success) {
                        pendingOperationDao.updateOperationStatus(operation.id, "completed")
                        successCount++
                    } else {
                        handleOperationFailure(operation, "Сервер повернув помилку")
                        failCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Помилка при виконанні операції ${operation.id}")
                    handleOperationFailure(operation, e.message ?: "Невідома помилка")
                    failCount++
                }
            }

            // Очищаємо завершені операції
            pendingOperationDao.deleteCompletedOperations()

            // Оновлюємо час синхронізації
            dataStoreManager.setLastSyncTime(System.currentTimeMillis())
            dataStoreManager.setOfflineMode(false)

            Timber.d("Синхронізація завершена: $successCount успішно, $failCount невдало")

            if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Критична помилка синхронізації")
            dataStoreManager.setOfflineMode(true)
            Result.retry()
        }
    }

    private suspend fun handleOperationFailure(operation: PendingOperationEntity, errorMessage: String) {
        pendingOperationDao.incrementRetryCount(operation.id)
        if (operation.retryCount + 1 >= operation.maxRetries) {
            pendingOperationDao.markAsFailed(operation.id, errorMessage)
        } else {
            pendingOperationDao.updateOperationStatus(operation.id, "pending")
        }
    }

    private suspend fun executeOperation(operation: PendingOperationEntity): Boolean {
        return when (operation.entityType) {
            "task" -> executeTaskOperation(operation)
            "message" -> executeMessageOperation(operation)
            "board" -> executeBoardOperation(operation)
            "column" -> executeColumnOperation(operation)
            else -> {
                Timber.w("Невідомий тип сутності: ${operation.entityType}")
                false
            }
        }
    }

    // ==================== TASK OPERATIONS ====================

    private suspend fun executeTaskOperation(operation: PendingOperationEntity): Boolean {
        return when (operation.operationType) {
            "create" -> executeCreateTask(operation)
            "update" -> executeUpdateTask(operation)
            "delete" -> executeDeleteTask(operation)
            "move" -> executeMoveTask(operation)
            else -> {
                Timber.w("Невідомий тип операції задачі: ${operation.operationType}")
                false
            }
        }
    }

    private suspend fun executeCreateTask(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<CreateTaskPayload>(operation.payload)

        val request = CreateTaskRequest(
            title = payload.title,
            description = payload.description,
            columnId = payload.columnId,
            priority = payload.priority,
            assigneeId = payload.assigneeId,
            deadline = payload.deadline,
            estimatedHours = payload.estimatedHours,
            tags = payload.tags,
            labelIds = payload.labelIds
        )

        val response = api.createTask(request)
        if (response.isSuccessful && response.body() != null) {
            val serverTask = response.body()!!
            // Оновлюємо локальний ID на серверний якщо потрібно
            if (operation.entityId != serverTask.id) {
                taskDao.updateTaskId(operation.entityId, serverTask.id)
            }
            // Оновлюємо версію для conflict resolution
            taskDao.updateVersion(serverTask.id, serverTask.version)
            return true
        }
        return false
    }

    private suspend fun executeUpdateTask(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<UpdateTaskPayload>(operation.payload)

        val request = UpdateTaskRequest(
            title = payload.title,
            description = payload.description,
            priority = payload.priority,
            assigneeId = payload.assigneeId,
            deadline = payload.deadline,
            estimatedHours = payload.estimatedHours,
            actualHours = payload.actualHours,
            tags = payload.tags,
            labelIds = payload.labelIds,
            version = payload.version // Для conflict detection
        )

        val response = api.updateTask(operation.entityId, request)
        return when {
            response.isSuccessful && response.body() != null -> {
                val serverTask = response.body()!!
                taskDao.updateVersion(serverTask.id, serverTask.version)
                true
            }
            response.code() == 409 -> {
                // Conflict - сервер має новішу версію
                Timber.w("Конфлікт версій для задачі ${operation.entityId}, потрібне злиття")
                handleTaskConflict(operation.entityId)
                false
            }
            else -> false
        }
    }

    private suspend fun executeDeleteTask(operation: PendingOperationEntity): Boolean {
        val response = api.deleteTask(operation.entityId)
        return response.isSuccessful
    }

    private suspend fun executeMoveTask(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<MoveTaskPayload>(operation.payload)

        val request = MoveTaskRequest(
            columnId = payload.columnId,
            position = payload.position
        )

        val response = api.moveTask(operation.entityId, request)
        return response.isSuccessful && response.body() != null
    }

    private suspend fun handleTaskConflict(taskId: String) {
        // Завантажуємо серверну версію
        val response = api.getTask(taskId)
        if (response.isSuccessful && response.body() != null) {
            val serverTask = response.body()!!
            // Last Write Wins - оновлюємо локальну версію серверною
            taskDao.updateFromServer(
                id = serverTask.id,
                title = serverTask.title,
                description = serverTask.description,
                columnId = serverTask.columnId,
                position = serverTask.position,
                priority = serverTask.priority,
                assigneeId = serverTask.assigneeId,
                deadline = DateTimeUtils.parseToEpochMilliOrNull(serverTask.deadline),
                version = serverTask.version,
                updatedAt = DateTimeUtils.parseToEpochMilli(serverTask.updatedAt)
            )
        }
    }

    // ==================== MESSAGE OPERATIONS ====================

    private suspend fun executeMessageOperation(operation: PendingOperationEntity): Boolean {
        return when (operation.operationType) {
            "create" -> executeSendMessage(operation)
            else -> {
                Timber.w("Невідомий тип операції повідомлення: ${operation.operationType}")
                false
            }
        }
    }

    private suspend fun executeSendMessage(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<SendMessagePayload>(operation.payload)

        val request = SendMessageRequest(
            taskId = payload.taskId,
            content = payload.content,
            type = payload.type
        )

        val response = api.sendMessage(request)
        if (response.isSuccessful && response.body() != null) {
            val serverMessage = response.body()!!
            // Оновлюємо локальний ID
            if (operation.entityId != serverMessage.id) {
                messageDao.updateMessageId(operation.entityId, serverMessage.id)
            }
            messageDao.markAsSynced(serverMessage.id)
            return true
        }
        return false
    }

    // ==================== BOARD OPERATIONS ====================

    private suspend fun executeBoardOperation(operation: PendingOperationEntity): Boolean {
        return when (operation.operationType) {
            "create" -> executeCreateBoard(operation)
            "update" -> executeUpdateBoard(operation)
            "delete" -> executeDeleteBoard(operation)
            else -> false
        }
    }

    private suspend fun executeCreateBoard(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<CreateBoardPayload>(operation.payload)

        val request = CreateBoardRequest(
            name = payload.name,
            description = payload.description,
            teamId = payload.teamId
        )

        val response = api.createBoard(request)
        if (response.isSuccessful && response.body() != null) {
            val serverBoard = response.body()!!
            if (operation.entityId != serverBoard.id) {
                boardDao.updateBoardId(operation.entityId, serverBoard.id)
            }
            return true
        }
        return false
    }

    private suspend fun executeUpdateBoard(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<CreateBoardPayload>(operation.payload)

        val request = CreateBoardRequest(
            name = payload.name,
            description = payload.description,
            teamId = payload.teamId
        )

        val response = api.updateBoard(operation.entityId, request)
        return response.isSuccessful
    }

    private suspend fun executeDeleteBoard(operation: PendingOperationEntity): Boolean {
        val response = api.deleteBoard(operation.entityId)
        return response.isSuccessful
    }

    // ==================== COLUMN OPERATIONS ====================

    private suspend fun executeColumnOperation(operation: PendingOperationEntity): Boolean {
        return when (operation.operationType) {
            "create" -> executeCreateColumn(operation)
            "update" -> executeUpdateColumn(operation)
            "delete" -> executeDeleteColumn(operation)
            else -> false
        }
    }

    private suspend fun executeCreateColumn(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<CreateColumnPayload>(operation.payload)

        val request = CreateColumnRequest(
            name = payload.name,
            boardId = payload.boardId,
            position = payload.position,
            wipLimit = payload.wipLimit,
            color = payload.color
        )

        val response = api.createColumn(request)
        if (response.isSuccessful && response.body() != null) {
            val serverColumn = response.body()!!
            if (operation.entityId != serverColumn.id) {
                columnDao.updateColumnId(operation.entityId, serverColumn.id)
            }
            return true
        }
        return false
    }

    private suspend fun executeUpdateColumn(operation: PendingOperationEntity): Boolean {
        val payload = json.decodeFromString<CreateColumnPayload>(operation.payload)

        val request = CreateColumnRequest(
            name = payload.name,
            boardId = payload.boardId,
            position = payload.position,
            wipLimit = payload.wipLimit,
            color = payload.color
        )

        val response = api.updateColumn(operation.entityId, request)
        return response.isSuccessful
    }

    private suspend fun executeDeleteColumn(operation: PendingOperationEntity): Boolean {
        val response = api.deleteColumn(operation.entityId)
        return response.isSuccessful
    }

    companion object {
        const val WORK_NAME = "sync_worker"

        fun buildOneTimeWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }

        fun buildPeriodicWorkRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()
        }
    }
}
