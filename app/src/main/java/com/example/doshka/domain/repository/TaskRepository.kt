package com.example.doshka.domain.repository

import com.example.doshka.domain.model.Task
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Репозиторій для роботи з задачами
 */
interface TaskRepository {

    /**
     * Спостерігає за задачами в колонці
     */
    fun observeTasksByColumn(columnId: String): Flow<List<Task>>

    /**
     * Спостерігає за задачами виконавця
     */
    fun observeTasksByAssignee(assigneeId: String): Flow<List<Task>>

    /**
     * Спостерігає за простроченими задачами
     */
    fun observeOverdueTasks(): Flow<List<Task>>

    /**
     * Отримує задачу за ID
     */
    suspend fun getTaskById(taskId: String): Task?

    /**
     * Створює нову задачу
     */
    suspend fun createTask(
        title: String,
        description: String? = null,
        columnId: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        assigneeId: String? = null,
        deadline: Instant? = null,
        estimatedHours: Float? = null,
        tags: List<String> = emptyList(),
        labelIds: List<String> = emptyList()
    ): NetworkResult<Task>

    /**
     * Оновлює задачу
     */
    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        description: String? = null,
        priority: TaskPriority? = null,
        assigneeId: String? = null,
        deadline: Instant? = null,
        estimatedHours: Float? = null,
        actualHours: Float? = null,
        tags: List<String>? = null,
        labelIds: List<String>? = null
    ): NetworkResult<Task>

    /**
     * Переміщує задачу в іншу колонку
     */
    suspend fun moveTask(taskId: String, columnId: String, position: Int): NetworkResult<Task>

    /**
     * Видаляє задачу
     */
    suspend fun deleteTask(taskId: String): NetworkResult<Unit>

    /**
     * Синхронізує задачі з сервером
     */
    suspend fun syncTasks(columnId: String): NetworkResult<Unit>
}
