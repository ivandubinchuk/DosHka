package com.example.doshka.ui.screens.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.local.dao.BoardDao
import com.example.doshka.data.local.dao.ColumnDao
import com.example.doshka.data.local.dao.TeamDao
import com.example.doshka.data.local.dao.UserDao
import com.example.doshka.data.local.entity.BoardEntity
import com.example.doshka.data.local.entity.ColumnEntity
import com.example.doshka.data.local.entity.TeamEntity
import com.example.doshka.data.local.entity.UserEntity
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.CreateBoardRequest
import com.example.doshka.data.remote.dto.CreateColumnRequest
import com.example.doshka.data.remote.dto.CreateTeamRequest
import com.example.doshka.domain.model.*
import com.example.doshka.domain.repository.AuthRepository
import com.example.doshka.domain.repository.BoardRepository
import com.example.doshka.domain.repository.TaskRepository
import com.example.doshka.util.DateTimeUtils
import com.example.doshka.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Стан екрану Канбан-дошки
 */
data class BoardUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val offlineSince: Long? = null,
    val board: Board? = null,
    val columns: List<ColumnWithTasks> = emptyList(),
    val currentUser: User? = null,
    val error: String? = null,
    val selectedTask: Task? = null,
    val showTaskDetails: Boolean = false,
    val showCreateTask: Boolean = false,
    val selectedColumnId: String? = null,
    // Нові поля для створення команди/дошки
    val hasTeam: Boolean = false,
    val hasBoard: Boolean = false,
    val showCreateTeamDialog: Boolean = false,
    val showCreateBoardDialog: Boolean = false,
    val showCreateColumnDialog: Boolean = false
)

/**
 * Колонка з задачами
 */
data class ColumnWithTasks(
    val column: Column,
    val tasks: List<Task>
)

/**
 * Стан форми створення/редагування задачі
 */
data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assigneeId: String? = null,
    val deadline: Long? = null,
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    val labelIds: List<String> = emptyList(),
    val titleError: String? = null
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val api: DoshkaApi,
    private val userDao: UserDao,
    private val teamDao: TeamDao,
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    private val _taskForm = MutableStateFlow(TaskFormState())
    val taskForm: StateFlow<TaskFormState> = _taskForm.asStateFlow()

    // Форми для створення
    private val _teamName = MutableStateFlow("")
    val teamName: StateFlow<String> = _teamName.asStateFlow()

    private val _boardName = MutableStateFlow("")
    val boardName: StateFlow<String> = _boardName.asStateFlow()

    private val _columnName = MutableStateFlow("")
    val columnName: StateFlow<String> = _columnName.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<com.example.doshka.domain.model.User>>(emptyList())
    val teamMembers: StateFlow<List<com.example.doshka.domain.model.User>> = _teamMembers.asStateFlow()

    private var currentBoardId: String? = null
    private var currentTeamId: String? = null

    init {
        loadCurrentUser()
        observeOfflineState()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }

                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, hasTeam = false, hasBoard = false) }
                    return@collect
                }

                val teamId = user.teamId
                if (teamId.isNullOrBlank()) {
                    // Користувач не має команди - показуємо створення команди
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasTeam = false,
                            hasBoard = false
                        )
                    }
                } else {
                    currentTeamId = teamId
                    _uiState.update { it.copy(hasTeam = true) }
                    loadDefaultBoard(teamId)
                }
            }
        }
    }

    private fun observeOfflineState() {
        viewModelScope.launch {
            dataStoreManager.isOfflineMode.collect { isOffline ->
                val offlineSince = if (isOffline) {
                    dataStoreManager.lastSyncTime.first()
                } else null

                _uiState.update {
                    it.copy(
                        isOffline = isOffline,
                        offlineSince = offlineSince
                    )
                }
            }
        }
    }

    private suspend fun loadDefaultBoard(teamId: String) {
        _uiState.update { it.copy(isLoading = true) }

        try {
            // Перевіряємо чи Team є в Room, якщо ні — завантажуємо і зберігаємо
            if (teamDao.getTeamById(teamId) == null) {
                val teamResponse = api.getTeam(teamId)
                if (teamResponse.isSuccessful && teamResponse.body() != null) {
                    val team = teamResponse.body()!!
                    teamDao.insertTeam(TeamEntity(
                        id = team.id,
                        name = team.name,
                        description = team.description,
                        managerId = team.managerId,
                        inviteCode = team.inviteCode
                    ))
                }
            }

            // Запитуємо дошки команди з сервера
            val response = api.getBoardsByTeam(teamId)
            if (response.isSuccessful) {
                val boards = response.body() ?: emptyList()
                if (boards.isEmpty()) {
                    // Немає дошок - пропонуємо створити
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasBoard = false
                        )
                    }
                } else {
                    // Беремо першу дошку
                    val board = boards.first()
                    currentBoardId = board.id

                    // Зберігаємо Board в Room (для FK колонок)
                    boardDao.insertBoard(BoardEntity(
                        id = board.id,
                        name = board.name,
                        description = board.description,
                        teamId = board.teamId,
                        isDefault = true
                    ))

                    _uiState.update {
                        it.copy(
                            hasBoard = true,
                            board = Board(
                                id = board.id,
                                name = board.name,
                                teamId = board.teamId,
                                description = board.description
                            )
                        )
                    }
                    loadBoardColumns(board.id)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Помилка завантаження: ${response.code()}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isOffline = true,
                    error = "Помилка з'єднання: ${e.localizedMessage}"
                )
            }
        }
    }

    private suspend fun loadBoardColumns(boardId: String) {
        try {
            val response = api.getColumnsByBoard(boardId)
            if (response.isSuccessful) {
                val columns = response.body() ?: emptyList()
                if (columns.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, columns = emptyList()) }
                } else {
                    // Зберігаємо колонки в Room (для FK задач)
                    columnDao.insertColumns(columns.map { col ->
                        ColumnEntity(
                            id = col.id,
                            name = col.name,
                            boardId = col.boardId,
                            position = col.position,
                            wipLimit = col.wipLimit,
                            color = col.color
                        )
                    })

                    // Завантажуємо задачі для кожної колонки
                    val columnsWithTasks = columns.map { col ->
                        val tasksResponse = api.getTasksByColumn(col.id)
                        val tasks = if (tasksResponse.isSuccessful) {
                            tasksResponse.body()?.map { dto ->
                                Task(
                                    id = dto.id,
                                    title = dto.title,
                                    description = dto.description,
                                    columnId = dto.columnId,
                                    position = dto.position,
                                    priority = TaskPriority.valueOf(dto.priority.uppercase()),
                                    deadline = DateTimeUtils.parseInstantOrNull(dto.deadline),
                                    estimatedHours = dto.estimatedHours,
                                    actualHours = dto.actualHours,
                                    createdAt = DateTimeUtils.parseInstant(dto.createdAt),
                                    updatedAt = DateTimeUtils.parseInstant(dto.updatedAt)
                                )
                            } ?: emptyList()
                        } else emptyList()

                        ColumnWithTasks(
                            column = Column(
                                id = col.id,
                                name = col.name,
                                boardId = col.boardId,
                                position = col.position,
                                wipLimit = col.wipLimit,
                                color = col.color
                            ),
                            tasks = tasks.sortedBy { it.position }
                        )
                    }.sortedBy { it.column.position }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            columns = columnsWithTasks
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
        }
    }

    // === Створення команди ===
    fun showCreateTeamDialog() {
        _teamName.value = ""
        _uiState.update { it.copy(showCreateTeamDialog = true) }
    }

    fun hideCreateTeamDialog() {
        _uiState.update { it.copy(showCreateTeamDialog = false) }
    }

    fun updateTeamName(name: String) {
        _teamName.value = name
    }

    fun createTeam() {
        val name = _teamName.value.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateTeamDialog = false) }
            try {
                val response = api.createTeam(CreateTeamRequest(name = name))
                if (response.isSuccessful) {
                    val team = response.body()!!
                    currentTeamId = team.id

                    // Зберігаємо Team в Room (для FK)
                    teamDao.insertTeam(TeamEntity(
                        id = team.id,
                        name = team.name,
                        description = team.description,
                        managerId = team.managerId,
                        inviteCode = team.inviteCode
                    ))

                    // Отримуємо оновлені дані користувача з сервера
                    val userResponse = api.getCurrentUser()
                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        val userDto = userResponse.body()!!
                        // Оновлюємо користувача в DataStore
                        dataStoreManager.saveUser(
                            id = userDto.id,
                            email = userDto.email,
                            name = userDto.fullName,
                            role = userDto.role,
                            teamId = userDto.teamId
                        )
                        // Оновлюємо користувача в Room (ключовий крок!)
                        userDao.insertUser(UserEntity(
                            id = userDto.id,
                            email = userDto.email,
                            fullName = userDto.fullName,
                            role = userDto.role,
                            teamId = userDto.teamId,
                            avatarUrl = userDto.avatarUrl,
                            isActive = userDto.isActive
                        ))
                        // Оновлюємо локальний стан
                        val updatedUser = User(
                            id = userDto.id,
                            email = userDto.email,
                            fullName = userDto.fullName,
                            role = UserRole.fromString(userDto.role),
                            teamId = userDto.teamId,
                            avatarUrl = userDto.avatarUrl,
                            isActive = userDto.isActive
                        )
                        _uiState.update {
                            it.copy(
                                hasTeam = true,
                                isLoading = false,
                                currentUser = updatedUser
                            )
                        }
                    } else {
                        // Якщо не вдалося отримати користувача, використовуємо локальні дані
                        _uiState.update { it.copy(hasTeam = true, isLoading = false) }
                    }

                    loadDefaultBoard(team.id)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Невідома помилка"
                    _uiState.update {
                        it.copy(isLoading = false, error = "Помилка: $errorBody")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = "Помилка: ${e.localizedMessage}") }
            }
        }
    }

    // === Створення дошки ===
    fun showCreateBoardDialog() {
        _boardName.value = ""
        _uiState.update { it.copy(showCreateBoardDialog = true) }
    }

    fun hideCreateBoardDialog() {
        _uiState.update { it.copy(showCreateBoardDialog = false) }
    }

    fun updateBoardName(name: String) {
        _boardName.value = name
    }

    fun createBoard() {
        val name = _boardName.value.trim()
        val teamId = currentTeamId ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateBoardDialog = false) }
            try {
                val response = api.createBoard(CreateBoardRequest(name = name, teamId = teamId))
                if (response.isSuccessful) {
                    val board = response.body()!!
                    currentBoardId = board.id

                    // Зберігаємо Board в Room (для FK колонок)
                    boardDao.insertBoard(BoardEntity(
                        id = board.id,
                        name = board.name,
                        description = board.description,
                        teamId = board.teamId,
                        isDefault = true
                    ))

                    _uiState.update {
                        it.copy(
                            hasBoard = true,
                            board = Board(
                                id = board.id,
                                name = board.name,
                                teamId = board.teamId,
                                description = board.description
                            )
                        )
                    }
                    // Створюємо стандартні колонки
                    createDefaultColumns(board.id)
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Помилка створення дошки")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private suspend fun createDefaultColumns(boardId: String) {
        // 4 стандартні колонки Kanban
        val defaultColumns = listOf(
            Triple("Backlog", 0, null),
            Triple("To Do", 1, 5),
            Triple("In Progress", 2, 3),
            Triple("Done", 3, null)
        )

        for ((name, position, wipLimit) in defaultColumns) {
            try {
                api.createColumn(CreateColumnRequest(
                    name = name,
                    boardId = boardId,
                    position = position,
                    wipLimit = wipLimit
                ))
            } catch (e: Exception) {
                // Ігноруємо помилки при створенні колонок
            }
        }

        loadBoardColumns(boardId)
    }

    // === Створення колонки ===
    fun showCreateColumnDialog() {
        _columnName.value = ""
        _uiState.update { it.copy(showCreateColumnDialog = true) }
    }

    fun hideCreateColumnDialog() {
        _uiState.update { it.copy(showCreateColumnDialog = false) }
    }

    fun updateColumnName(name: String) {
        _columnName.value = name
    }

    fun createColumn() {
        val name = _columnName.value.trim()
        val boardId = currentBoardId ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(showCreateColumnDialog = false) }
            try {
                val position = _uiState.value.columns.size
                val response = api.createColumn(CreateColumnRequest(
                    name = name,
                    boardId = boardId,
                    position = position
                ))
                if (response.isSuccessful) {
                    loadBoardColumns(boardId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun loadBoard(boardId: String) {
        currentBoardId = boardId
        viewModelScope.launch {
            loadBoardColumns(boardId)
        }
    }

    fun observeColumns() {
        val boardId = currentBoardId ?: return
        viewModelScope.launch {
            loadBoardColumns(boardId)
        }
    }

    fun syncBoard() {
        val boardId = currentBoardId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                loadBoardColumns(boardId)
                dataStoreManager.setLastSyncTime(System.currentTimeMillis())
                dataStoreManager.setOfflineMode(false)
                _uiState.update { it.copy(isSyncing = false, isOffline = false) }
            } catch (e: Exception) {
                dataStoreManager.setOfflineMode(true)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isOffline = true,
                        error = "Працюємо в офлайн режимі"
                    )
                }
            }
        }
    }

    // Форма задачі
    fun updateTaskTitle(title: String) {
        _taskForm.update { it.copy(title = title, titleError = null) }
    }

    fun updateTaskDescription(description: String) {
        _taskForm.update { it.copy(description = description) }
    }

    fun updateTaskPriority(priority: TaskPriority) {
        _taskForm.update { it.copy(priority = priority) }
    }

    fun updateTaskAssignee(assigneeId: String?) {
        _taskForm.update { it.copy(assigneeId = assigneeId) }
    }

    fun updateTaskDeadline(deadline: Long?) {
        _taskForm.update { it.copy(deadline = deadline) }
    }

    fun updateTaskEstimatedHours(hours: Float?) {
        _taskForm.update { it.copy(estimatedHours = hours) }
    }

    fun updateTaskTags(tags: List<String>) {
        _taskForm.update { it.copy(tags = tags) }
    }

    fun updateTaskLabels(labelIds: List<String>) {
        _taskForm.update { it.copy(labelIds = labelIds) }
    }

    // Операції з задачами
    fun showCreateTaskDialog(columnId: String) {
        _taskForm.update { TaskFormState() }
        _uiState.update {
            it.copy(
                showCreateTask = true,
                selectedColumnId = columnId
            )
        }
    }

    fun hideCreateTaskDialog() {
        _uiState.update { it.copy(showCreateTask = false, selectedColumnId = null) }
    }

    fun createTask() {
        val form = _taskForm.value
        val columnId = _uiState.value.selectedColumnId ?: return

        // Валідація
        if (form.title.isBlank()) {
            _taskForm.update { it.copy(titleError = "Введіть назву задачі") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = taskRepository.createTask(
                title = form.title,
                description = form.description.ifBlank { null },
                columnId = columnId,
                priority = form.priority,
                assigneeId = form.assigneeId,
                deadline = form.deadline?.let { java.time.Instant.ofEpochMilli(it) },
                estimatedHours = form.estimatedHours,
                tags = form.tags,
                labelIds = form.labelIds
            )

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            showCreateTask = false,
                            selectedColumnId = null
                        )
                    }
                    _taskForm.update { TaskFormState() }
                    // Перезавантажуємо колонки
                    currentBoardId?.let { loadBoardColumns(it) }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun selectTask(task: Task) {
        _uiState.update {
            it.copy(
                selectedTask = task,
                showTaskDetails = true
            )
        }
    }

    fun hideTaskDetails() {
        _uiState.update {
            it.copy(
                selectedTask = null,
                showTaskDetails = false
            )
        }
    }

    fun moveTask(taskId: String, toColumnId: String, position: Int) {
        viewModelScope.launch {
            val result = taskRepository.moveTask(taskId, toColumnId, position)
            when (result) {
                is NetworkResult.Success -> {
                    currentBoardId?.let { loadBoardColumns(it) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = taskRepository.deleteTask(taskId)
            when (result) {
                is NetworkResult.Success -> {
                    hideTaskDetails()
                    currentBoardId?.let { loadBoardColumns(it) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            currentTeamId?.let { loadDefaultBoard(it) }
                ?: loadCurrentUser()
        }
    }

    // === CRUD для колонок ===

    /**
     * Видалення колонки
     */
    fun deleteColumn(columnId: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteColumn(columnId)
                if (response.isSuccessful) {
                    currentBoardId?.let { loadBoardColumns(it) }
                } else {
                    _uiState.update { it.copy(error = "Не вдалося видалити колонку") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /**
     * Оновлення назви колонки
     */
    fun renameColumn(columnId: String, newName: String) {
        if (newName.isBlank()) return
        val boardId = currentBoardId ?: return

        viewModelScope.launch {
            try {
                val response = api.updateColumn(
                    columnId = columnId,
                    request = CreateColumnRequest(
                        name = newName,
                        boardId = boardId,
                        position = 0 // позиція не змінюється
                    )
                )
                if (response.isSuccessful) {
                    loadBoardColumns(boardId)
                } else {
                    _uiState.update { it.copy(error = "Не вдалося оновити колонку") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    // === CRUD для команд ===

    /**
     * Оновлення команди
     */
    fun updateTeam(newName: String) {
        val teamId = currentTeamId ?: return
        if (newName.isBlank()) return

        viewModelScope.launch {
            try {
                val response = api.updateTeam(teamId, CreateTeamRequest(name = newName))
                if (response.isSuccessful) {
                    loadCurrentUser()
                } else {
                    _uiState.update { it.copy(error = "Не вдалося оновити команду") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /**
     * Завантаження списку учасників команди
     */
    fun loadTeamMembers() {
        viewModelScope.launch {
            val teamId = currentTeamId ?: return@launch
            try {
                val response = api.getTeamMembers(teamId)
                if (response.isSuccessful && response.body() != null) {
                    val members = response.body()!!.map { dto ->
                        com.example.doshka.domain.model.User(
                            id = dto.id,
                            email = dto.email,
                            fullName = dto.fullName,
                            role = com.example.doshka.domain.model.UserRole.fromString(dto.role),
                            teamId = dto.teamId,
                            avatarUrl = dto.avatarUrl,
                            isActive = dto.isActive
                        )
                    }
                    _teamMembers.value = members
                }
            } catch (e: Exception) {
                // Помилка
            }
        }
    }

    // === CRUD для задач (оновлення) ===

    /**
     * Оновлення задачі
     */
    fun updateTask(taskId: String, title: String? = null, description: String? = null, priority: String? = null) {
        viewModelScope.launch {
            try {
                val response = api.updateTask(
                    taskId = taskId,
                    request = com.example.doshka.data.remote.dto.UpdateTaskRequest(
                        title = title,
                        description = description,
                        priority = priority
                    )
                )
                if (response.isSuccessful) {
                    currentBoardId?.let { loadBoardColumns(it) }
                } else {
                    _uiState.update { it.copy(error = "Не вдалося оновити задачу") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearAll()
        }
    }
}
