package com.example.doshka.data.repository

import com.example.doshka.data.local.dao.TaskDao
import com.example.doshka.data.local.dao.LabelDao
import com.example.doshka.data.local.entity.TaskEntity
import com.example.doshka.data.local.entity.TaskLabelCrossRef
import com.example.doshka.data.network.NetworkMonitor
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.CreateTaskRequest
import com.example.doshka.data.remote.dto.MoveTaskRequest
import com.example.doshka.data.remote.dto.UpdateTaskRequest
import com.example.doshka.data.sync.SyncManager
import com.example.doshka.data.sync.OperationType
import com.example.doshka.data.sync.EntityType
import com.example.doshka.data.sync.CreateTaskPayload
import com.example.doshka.data.sync.UpdateTaskPayload
import com.example.doshka.data.sync.MoveTaskPayload
import com.example.doshka.domain.model.Task
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.domain.repository.TaskRepository
import com.example.doshka.util.DateTimeUtils
import com.example.doshka.util.NetworkResult
import com.example.doshka.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Реалізація репозиторію задач з offline-first підходом
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val labelDao: LabelDao,
    private val api: DoshkaApi,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val json: Json
) : TaskRepository {

    override fun observeTasksByColumn(columnId: String): Flow<List<Task>> {
        return taskDao.observeTasksByColumn(columnId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTasksByAssignee(assigneeId: String): Flow<List<Task>> {
        return taskDao.observeTasksByAssignee(assigneeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeOverdueTasks(): Flow<List<Task>> {
        return taskDao.observeOverdueTasks(System.currentTimeMillis()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    /**
     * Створення задачі з offline-first підходом
     * 1. Зберігаємо локально з тимчасовим ID
     * 2. Якщо онлайн - синхронізуємо з сервером
     * 3. Якщо офлайн - додаємо в чергу синхронізації
     */
    override suspend fun createTask(
        title: String,
        description: String?,
        columnId: String,
        priority: TaskPriority,
        assigneeId: String?,
        deadline: Instant?,
        estimatedHours: Float?,
        tags: List<String>,
        labelIds: List<String>
    ): NetworkResult<Task> {
        // Генеруємо тимчасовий ID (буде замінений серверним)
        val tempId = "temp_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        // Створюємо локальний entity
        val entity = TaskEntity(
            id = tempId,
            title = title,
            description = description,
            columnId = columnId,
            position = getNextPosition(columnId),
            priority = priority.toString(),
            assigneeId = assigneeId,
            creatorId = null, // Буде встановлено з сервера
            deadline = deadline?.toEpochMilli(),
            estimatedHours = estimatedHours,
            actualHours = null,
            tags = json.encodeToString(tags),
            attachmentsCount = 0,
            commentsCount = 0,
            isDeleted = false,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
            version = 0,
            isSynced = false // Позначаємо як несинхронізовану
        )

        // Спочатку зберігаємо локально
        taskDao.insertTask(entity)

        // Зберігаємо зв'язки з мітками
        labelIds.forEach { labelId ->
            labelDao.addLabelToTask(TaskLabelCrossRef(tempId, labelId))
        }

        // Якщо онлайн - синхронізуємо з сервером
        if (networkMonitor.isOnline.value) {
            try {
                val request = CreateTaskRequest(
                    title = title,
                    description = description,
                    columnId = columnId,
                    priority = priority.toString(),
                    assigneeId = assigneeId,
                    deadline = deadline?.toString(),
                    estimatedHours = estimatedHours,
                    tags = tags,
                    labelIds = labelIds
                )

                val response = api.createTask(request)
                if (response.isSuccessful && response.body() != null) {
                    val serverTask = response.body()!!

                    // Оновлюємо локальний ID на серверний
                    if (tempId != serverTask.id) {
                        taskDao.updateTaskId(tempId, serverTask.id)
                        // Оновлюємо зв'язки міток
                        labelIds.forEach { labelId ->
                            labelDao.addLabelToTask(TaskLabelCrossRef(serverTask.id, labelId))
                        }
                    }

                    // Оновлюємо version та позначаємо як синхронізовану
                    taskDao.updateVersion(serverTask.id, serverTask.version)
                    taskDao.markAsSynced(serverTask.id)

                    val syncedEntity = taskDao.getTaskById(serverTask.id)
                    return NetworkResult.Success(syncedEntity?.toDomain() ?: entity.toDomain())
                }
            } catch (e: Exception) {
                Timber.w(e, "Не вдалося синхронізувати задачу з сервером, зберігаємо офлайн")
            }
        }

        // Додаємо в чергу синхронізації
        val payload = CreateTaskPayload(
            title = title,
            description = description,
            columnId = columnId,
            priority = priority.toString(),
            assigneeId = assigneeId,
            deadline = deadline?.toString(),
            estimatedHours = estimatedHours,
            tags = tags,
            labelIds = labelIds
        )
        syncManager.queueOperation(OperationType.CREATE, EntityType.TASK, tempId, json.encodeToString(payload))

        return NetworkResult.Success(entity.toDomain())
    }

    override suspend fun updateTask(
        taskId: String,
        title: String?,
        description: String?,
        priority: TaskPriority?,
        assigneeId: String?,
        deadline: Instant?,
        estimatedHours: Float?,
        actualHours: Float?,
        tags: List<String>?,
        labelIds: List<String>?
    ): NetworkResult<Task> {
        // Отримуємо поточну версію
        val currentTask = taskDao.getTaskById(taskId)
            ?: return NetworkResult.Error("Задачу не знайдено")

        // Оновлюємо локально
        val updatedEntity = currentTask.copy(
            title = title ?: currentTask.title,
            description = description ?: currentTask.description,
            priority = priority?.toString() ?: currentTask.priority,
            assigneeId = assigneeId ?: currentTask.assigneeId,
            deadline = deadline?.toEpochMilli() ?: currentTask.deadline,
            estimatedHours = estimatedHours ?: currentTask.estimatedHours,
            actualHours = actualHours ?: currentTask.actualHours,
            tags = if (tags != null) json.encodeToString(tags) else currentTask.tags,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.updateTask(updatedEntity)

        // Оновлюємо мітки
        if (labelIds != null) {
            labelDao.removeAllLabelsFromTask(taskId)
            labelIds.forEach { labelId ->
                labelDao.addLabelToTask(TaskLabelCrossRef(taskId, labelId))
            }
        }

        // Якщо онлайн - синхронізуємо
        if (networkMonitor.isOnline.value) {
            try {
                val request = UpdateTaskRequest(
                    title = title,
                    description = description,
                    priority = priority?.toString(),
                    assigneeId = assigneeId,
                    deadline = deadline?.toString(),
                    estimatedHours = estimatedHours,
                    actualHours = actualHours,
                    tags = tags,
                    labelIds = labelIds,
                    version = currentTask.version // Для conflict detection
                )

                val response = api.updateTask(taskId, request)
                when {
                    response.isSuccessful && response.body() != null -> {
                        val serverTask = response.body()!!
                        taskDao.updateVersion(taskId, serverTask.version)
                        taskDao.markAsSynced(taskId)
                        val syncedEntity = taskDao.getTaskById(taskId)
                        return NetworkResult.Success(syncedEntity?.toDomain() ?: updatedEntity.toDomain())
                    }
                    response.code() == 409 -> {
                        // Конфлікт версій - перезавантажуємо з сервера
                        Timber.w("Конфлікт версій для задачі $taskId")
                        syncFromServer(taskId)
                        val refreshedTask = taskDao.getTaskById(taskId)
                        return NetworkResult.Error("Конфлікт версій. Дані оновлено з сервера.")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Не вдалося синхронізувати оновлення задачі")
            }
        }

        // Додаємо в чергу синхронізації
        val payload = UpdateTaskPayload(
            title = title,
            description = description,
            priority = priority?.toString(),
            assigneeId = assigneeId,
            deadline = deadline?.toString(),
            estimatedHours = estimatedHours,
            actualHours = actualHours,
            tags = tags,
            labelIds = labelIds,
            version = currentTask.version
        )
        syncManager.queueOperation(OperationType.UPDATE, EntityType.TASK, taskId, json.encodeToString(payload))

        return NetworkResult.Success(updatedEntity.toDomain())
    }

    override suspend fun moveTask(taskId: String, columnId: String, position: Int): NetworkResult<Task> {
        // Спочатку пробуємо синхронізувати з сервером
        if (networkMonitor.isOnline.value) {
            try {
                val response = api.moveTask(taskId, MoveTaskRequest(columnId, position))
                if (response.isSuccessful && response.body() != null) {
                    val serverTask = response.body()!!
                    // Оновлюємо локальну БД
                    taskDao.moveTask(taskId, columnId, position)
                    taskDao.updateVersion(taskId, serverTask.version)
                    taskDao.markAsSynced(taskId)

                    // Повертаємо задачу з локальної БД або конвертуємо з відповіді сервера
                    val localTask = taskDao.getTaskById(taskId)
                    return if (localTask != null) {
                        NetworkResult.Success(localTask.toDomain())
                    } else {
                        // Задачі немає локально - це ОК, просто повертаємо успіх
                        // Задача буде перезавантажена при наступному sync
                        NetworkResult.Success(
                            Task(
                                id = serverTask.id,
                                title = serverTask.title,
                                description = serverTask.description,
                                columnId = serverTask.columnId,
                                position = serverTask.position,
                                priority = com.example.doshka.domain.model.TaskPriority.valueOf(serverTask.priority.uppercase()),
                                assignee = null,
                                creator = null,
                                deadline = serverTask.deadline?.let { com.example.doshka.util.DateTimeUtils.parseInstantOrNull(it) },
                                estimatedHours = serverTask.estimatedHours,
                                actualHours = serverTask.actualHours,
                                tags = serverTask.tags,
                                createdAt = com.example.doshka.util.DateTimeUtils.parseInstant(serverTask.createdAt),
                                updatedAt = com.example.doshka.util.DateTimeUtils.parseInstant(serverTask.updatedAt)
                            )
                        )
                    }
                } else {
                    return NetworkResult.Error("Не вдалося перемістити задачу")
                }
            } catch (e: Exception) {
                Timber.w(e, "Не вдалося синхронізувати переміщення задачі")
                return NetworkResult.Error(e.message ?: "Помилка переміщення")
            }
        }

        // Офлайн режим - оновлюємо локально і додаємо в чергу
        taskDao.moveTask(taskId, columnId, position)
        taskDao.markAsUnsynced(taskId)

        val payload = MoveTaskPayload(columnId, position)
        syncManager.queueOperation(OperationType.MOVE, EntityType.TASK, taskId, json.encodeToString(payload))

        val task = taskDao.getTaskById(taskId)
        return if (task != null) {
            NetworkResult.Success(task.toDomain())
        } else {
            NetworkResult.Error("Задачу не знайдено в локальній базі")
        }
    }

    override suspend fun deleteTask(taskId: String): NetworkResult<Unit> {
        // Soft delete локально
        taskDao.softDeleteTask(taskId)

        // Якщо онлайн - синхронізуємо
        if (networkMonitor.isOnline.value) {
            try {
                val response = api.deleteTask(taskId)
                if (response.isSuccessful) {
                    return NetworkResult.Success(Unit)
                }
            } catch (e: Exception) {
                Timber.w(e, "Не вдалося синхронізувати видалення задачі")
            }
        }

        // Додаємо в чергу синхронізації (пустий payload для delete)
        syncManager.queueOperation(OperationType.DELETE, EntityType.TASK, taskId, "{}")

        return NetworkResult.Success(Unit)
    }

    override suspend fun syncTasks(columnId: String): NetworkResult<Unit> {
        if (!networkMonitor.isOnline.value) {
            return NetworkResult.Error("Немає підключення до мережі")
        }

        return safeApiCall {
            val response = api.getTasksByColumn(columnId)
            if (response.isSuccessful && response.body() != null) {
                val serverTasks = response.body()!!

                // Оновлюємо локальні дані серверними
                serverTasks.forEach { dto ->
                    val entity = dto.toEntity().copy(isSynced = true)
                    taskDao.insertTask(entity)
                }
            } else {
                throw Exception("Не вдалося синхронізувати задачі")
            }
        }
    }

    /**
     * Синхронізує конкретну задачу з сервера (для вирішення конфліктів)
     */
    private suspend fun syncFromServer(taskId: String) {
        try {
            val response = api.getTask(taskId)
            if (response.isSuccessful && response.body() != null) {
                val serverTask = response.body()!!
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
        } catch (e: Exception) {
            Timber.e(e, "Не вдалося синхронізувати задачу $taskId з сервера")
        }
    }

    /**
     * Отримує наступну позицію для задачі в колонці
     */
    private suspend fun getNextPosition(columnId: String): Int {
        val tasks = taskDao.getTasksByColumn(columnId)
        return (tasks.maxOfOrNull { it.position } ?: 0) + 1
    }

    // Маппери
    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            columnId = columnId,
            position = position,
            priority = TaskPriority.fromString(priority),
            assignee = null, // завантажується окремо
            creator = null,
            deadline = deadline?.let { Instant.ofEpochMilli(it) },
            estimatedHours = estimatedHours,
            actualHours = actualHours,
            tags = json.decodeFromString(tags),
            labels = emptyList(), // завантажується окремо
            attachmentsCount = attachmentsCount,
            commentsCount = commentsCount,
            isDeleted = isDeleted,
            completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            isSynced = isSynced
        )
    }

    private fun com.example.doshka.data.remote.dto.TaskDto.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            columnId = columnId,
            position = position,
            priority = priority,
            assigneeId = assigneeId,
            creatorId = creatorId,
            deadline = DateTimeUtils.parseToEpochMilliOrNull(deadline),
            estimatedHours = estimatedHours,
            actualHours = actualHours,
            tags = json.encodeToString(tags),
            attachmentsCount = attachmentsCount,
            commentsCount = commentsCount,
            isDeleted = isDeleted,
            completedAt = DateTimeUtils.parseToEpochMilliOrNull(completedAt),
            createdAt = DateTimeUtils.parseToEpochMilli(createdAt),
            updatedAt = DateTimeUtils.parseToEpochMilli(updatedAt),
            version = version,
            isSynced = true
        )
    }
}
