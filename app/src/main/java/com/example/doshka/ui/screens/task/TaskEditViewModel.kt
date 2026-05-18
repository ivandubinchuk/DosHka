package com.example.doshka.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.MoveTaskRequest
import com.example.doshka.data.remote.dto.UpdateTaskRequest
import com.example.doshka.domain.model.*
import com.example.doshka.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Стан UI екрану редагування
 */
data class TaskEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val task: Task? = null,
    val columns: List<Column> = emptyList(),
    val messagesCount: Int = 0,
    val attachmentsCount: Int = 0,
    val error: String? = null,
    val showDeleteConfirm: Boolean = false,
    val taskDeleted: Boolean = false
)

/**
 * Стан форми редагування
 */
data class TaskEditFormState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assigneeId: String? = null,
    val columnId: String? = null,
    val deadline: Instant? = null,
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    // Оригінальні значення для відстеження змін
    val originalTitle: String = "",
    val originalDescription: String = "",
    val originalPriority: TaskPriority = TaskPriority.MEDIUM,
    val originalAssigneeId: String? = null,
    val originalColumnId: String? = null,
    val originalDeadline: Instant? = null
) {
    val hasChanges: Boolean
        get() = title != originalTitle ||
                description != originalDescription ||
                priority != originalPriority ||
                assigneeId != originalAssigneeId ||
                columnId != originalColumnId ||
                deadline != originalDeadline
}

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val api: DoshkaApi,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TaskEditFormState())
    val formState: StateFlow<TaskEditFormState> = _formState.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<User>>(emptyList())
    val teamMembers: StateFlow<List<User>> = _teamMembers.asStateFlow()

    private var taskId: String = ""
    private var boardId: String? = null

    fun loadTask(taskId: String) {
        this.taskId = taskId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Завантажуємо задачу
                val taskResponse = api.getTask(taskId)
                if (taskResponse.isSuccessful && taskResponse.body() != null) {
                    val dto = taskResponse.body()!!

                    val task = Task(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        columnId = dto.columnId,
                        position = dto.position,
                        priority = TaskPriority.valueOf(dto.priority.uppercase()),
                        assignee = null,
                        creator = null,
                        deadline = DateTimeUtils.parseInstantOrNull(dto.deadline),
                        estimatedHours = dto.estimatedHours,
                        actualHours = dto.actualHours,
                        tags = dto.tags,
                        createdAt = DateTimeUtils.parseInstant(dto.createdAt),
                        updatedAt = DateTimeUtils.parseInstant(dto.updatedAt)
                    )

                    // Ініціалізуємо форму
                    _formState.value = TaskEditFormState(
                        title = task.title,
                        description = task.description ?: "",
                        priority = task.priority,
                        assigneeId = dto.assigneeId,
                        columnId = task.columnId,
                        deadline = task.deadline,
                        estimatedHours = task.estimatedHours,
                        tags = task.tags,
                        originalTitle = task.title,
                        originalDescription = task.description ?: "",
                        originalPriority = task.priority,
                        originalAssigneeId = dto.assigneeId,
                        originalColumnId = task.columnId,
                        originalDeadline = task.deadline
                    )

                    // Завантажуємо кількість повідомлень
                    val messagesResponse = api.getMessages(taskId)
                    val messagesCount = if (messagesResponse.isSuccessful) {
                        messagesResponse.body()?.size ?: 0
                    } else 0

                    // Завантажуємо кількість вкладень
                    val attachmentsResponse = api.getAttachments(taskId)
                    val attachmentsCount = if (attachmentsResponse.isSuccessful) {
                        attachmentsResponse.body()?.size ?: 0
                    } else 0

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            task = task,
                            messagesCount = messagesCount,
                            attachmentsCount = attachmentsCount
                        )
                    }

                    // Завантажуємо колонки дошки
                    loadColumns(dto.columnId)

                    // Завантажуємо учасників команди
                    loadTeamMembers()

                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Задачу не знайдено")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Помилка: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun loadColumns(columnId: String) {
        try {
            // Спочатку отримуємо колонку щоб дізнатись boardId
            val columnsResponse = api.getColumnsByBoard(getBoardIdFromColumn(columnId))
            if (columnsResponse.isSuccessful && columnsResponse.body() != null) {
                val columns = columnsResponse.body()!!.map { dto ->
                    Column(
                        id = dto.id,
                        name = dto.name,
                        boardId = dto.boardId,
                        position = dto.position,
                        wipLimit = dto.wipLimit,
                        color = dto.color
                    )
                }.sortedBy { it.position }

                boardId = columns.firstOrNull()?.boardId

                _uiState.update { it.copy(columns = columns) }
            }
        } catch (e: Exception) {
            // Ігноруємо помилку завантаження колонок
        }
    }

    private suspend fun getBoardIdFromColumn(columnId: String): String {
        // Отримуємо boardId з поточних даних або з API
        return boardId ?: run {
            // Якщо boardId невідомий, спробуємо отримати з DataStore
            val teamId = dataStoreManager.teamId.first()
            if (teamId != null) {
                val boardsResponse = api.getBoardsByTeam(teamId)
                if (boardsResponse.isSuccessful && boardsResponse.body()?.isNotEmpty() == true) {
                    boardsResponse.body()!!.first().id
                } else {
                    throw Exception("Дошку не знайдено")
                }
            } else {
                throw Exception("Команду не знайдено")
            }
        }
    }

    private suspend fun loadTeamMembers() {
        try {
            val teamId = dataStoreManager.teamId.first() ?: return
            val response = api.getTeamMembers(teamId)
            if (response.isSuccessful && response.body() != null) {
                val members = response.body()!!.map { dto ->
                    User(
                        id = dto.id,
                        email = dto.email,
                        fullName = dto.fullName,
                        role = UserRole.fromString(dto.role),
                        teamId = dto.teamId,
                        avatarUrl = dto.avatarUrl,
                        isActive = dto.isActive
                    )
                }
                _teamMembers.value = members
            }
        } catch (e: Exception) {
            // Ігноруємо помилку
        }
    }

    // Оновлення полів форми
    fun updateTitle(title: String) {
        _formState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updatePriority(priority: TaskPriority) {
        _formState.update { it.copy(priority = priority) }
    }

    fun updateAssignee(assigneeId: String?) {
        _formState.update { it.copy(assigneeId = assigneeId) }
    }

    fun updateDeadline(deadline: Instant?) {
        _formState.update { it.copy(deadline = deadline) }
    }

    fun moveToColumn(columnId: String) {
        _formState.update { it.copy(columnId = columnId) }
    }

    // Збереження змін
    fun saveChanges() {
        val form = _formState.value
        if (form.title.isBlank()) {
            _uiState.update { it.copy(error = "Введіть назву задачі") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                // Оновлюємо основні поля
                val updateRequest = UpdateTaskRequest(
                    title = form.title,
                    description = form.description.ifBlank { null },
                    priority = form.priority.toString(),
                    assigneeId = form.assigneeId,
                    deadline = form.deadline?.toString()
                )

                val updateResponse = api.updateTask(taskId, updateRequest)

                if (!updateResponse.isSuccessful) {
                    throw Exception("Не вдалося оновити задачу")
                }

                // Якщо змінилась колонка — переміщуємо
                val originalColumnId = _uiState.value.task?.columnId
                if (form.columnId != null && form.columnId != originalColumnId) {
                    val moveResponse = api.moveTask(
                        taskId,
                        MoveTaskRequest(columnId = form.columnId, position = 0)
                    )
                    if (!moveResponse.isSuccessful) {
                        throw Exception("Не вдалося перемістити задачу")
                    }
                }

                // Оновлюємо оригінальні значення
                _formState.update {
                    it.copy(
                        originalTitle = form.title,
                        originalDescription = form.description,
                        originalPriority = form.priority,
                        originalAssigneeId = form.assigneeId,
                        originalColumnId = form.columnId,
                        originalDeadline = form.deadline
                    )
                }

                _uiState.update { it.copy(isSaving = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Помилка: ${e.localizedMessage}")
                }
            }
        }
    }

    // Видалення
    fun showDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun deleteTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, showDeleteConfirm = false) }

            try {
                val response = api.deleteTask(taskId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isSaving = false, taskDeleted = true) }
                } else {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Не вдалося видалити задачу")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Помилка: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
