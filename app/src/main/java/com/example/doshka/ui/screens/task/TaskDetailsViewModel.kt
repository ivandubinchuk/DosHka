package com.example.doshka.ui.screens.task

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.AttachmentDto
import com.example.doshka.data.remote.dto.MoveTaskRequest
import com.example.doshka.data.remote.dto.UpdateTaskRequest
import com.example.doshka.domain.model.*
import com.example.doshka.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Стан екрану деталей задачі
 */
data class TaskDetailsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val task: Task? = null,
    val assignee: User? = null,
    val creator: User? = null,
    val columns: List<Column> = emptyList(),
    val messagesCount: Int = 0,
    val unreadMessagesCount: Int = 0,  // Непрочитані повідомлення
    val attachmentsCount: Int = 0,
    val attachments: List<AttachmentDto> = emptyList(),
    val currentUserId: String? = null,
    val isManager: Boolean = false,
    val error: String? = null,
    val showDeleteConfirm: Boolean = false,
    val taskDeleted: Boolean = false
)

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: DoshkaApi,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow(TaskDetailsUiState())
    val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadTask()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            combine(
                dataStoreManager.userId,
                dataStoreManager.userRole
            ) { userId, role ->
                Pair(userId, role)
            }.collect { (userId, role) ->
                _uiState.update {
                    it.copy(
                        currentUserId = userId,
                        isManager = role == "manager"
                    )
                }
            }
        }
    }

    fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getTask(taskId)
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!

                    // Завантажуємо виконавця
                    val assignee = dto.assigneeId?.let { loadUser(it) }

                    // Завантажуємо створювача
                    val creator = dto.creatorId?.let { loadUser(it) }

                    // Створюємо Task з завантаженими User об'єктами
                    val task = Task(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        columnId = dto.columnId,
                        position = dto.position,
                        priority = TaskPriority.valueOf(dto.priority.uppercase()),
                        assignee = assignee,
                        creator = creator,
                        deadline = DateTimeUtils.parseInstantOrNull(dto.deadline),
                        estimatedHours = dto.estimatedHours,
                        actualHours = dto.actualHours,
                        tags = dto.tags,
                        createdAt = DateTimeUtils.parseInstant(dto.createdAt),
                        updatedAt = DateTimeUtils.parseInstant(dto.updatedAt)
                    )

                    // Завантажуємо повідомлення та рахуємо непрочитані
                    val messagesResponse = api.getMessages(taskId)
                    val messages = if (messagesResponse.isSuccessful) {
                        messagesResponse.body() ?: emptyList()
                    } else emptyList()
                    val messagesCount = messages.size
                    val currentUserId = _uiState.value.currentUserId
                    // Непрочитані = не від мене і не прочитані
                    val unreadCount = messages.count { msg ->
                        !msg.isRead && msg.senderId != currentUserId
                    }

                    // Завантажуємо вкладення
                    val attachmentsResponse = api.getAttachments(taskId)
                    val attachments = if (attachmentsResponse.isSuccessful) {
                        attachmentsResponse.body() ?: emptyList()
                    } else emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            task = task,
                            assignee = assignee,
                            creator = creator,
                            messagesCount = messagesCount,
                            unreadMessagesCount = unreadCount,
                            attachmentsCount = attachments.size,
                            attachments = attachments
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Задачу не знайдено"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun loadUser(userId: String): User? {
        return try {
            val response = api.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                User(
                    id = dto.id,
                    email = dto.email,
                    fullName = dto.fullName,
                    role = UserRole.fromString(dto.role),
                    teamId = dto.teamId,
                    avatarUrl = dto.avatarUrl,
                    isActive = dto.isActive
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun updatePriority(priority: TaskPriority) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val response = api.updateTask(
                    taskId = task.id,
                    request = UpdateTaskRequest(priority = priority.toString())
                )
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            task = task.copy(priority = priority)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Не вдалося оновити")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.localizedMessage)
                }
            }
        }
    }

    fun moveToColumn(columnId: String) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val response = api.moveTask(
                    taskId = task.id,
                    request = MoveTaskRequest(columnId = columnId, position = 0)
                )
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            task = task.copy(columnId = columnId)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Не вдалося перемістити")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.localizedMessage)
                }
            }
        }
    }

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
                        it.copy(isSaving = false, error = "Не вдалося видалити")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.localizedMessage)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Завантажує файл як вкладення до задачі
     */
    fun uploadAttachment(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Не вдалося відкрити файл")

                // Отримуємо ім'я файлу
                val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"

                // Отримуємо MIME тип
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                // Створюємо тимчасовий файл
                val tempFile = File(context.cacheDir, fileName)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                // Створюємо multipart запит
                val taskIdBody = taskId.toRequestBody("text/plain".toMediaTypeOrNull())
                val fileBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)

                val response = api.uploadAttachment(taskIdBody, filePart)

                // Видаляємо тимчасовий файл
                tempFile.delete()

                if (response.isSuccessful && response.body() != null) {
                    val newAttachment = response.body()!!
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            attachments = it.attachments + newAttachment,
                            attachmentsCount = it.attachmentsCount + 1
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            error = "Не вдалося завантажити файл"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Видаляє вкладення
     */
    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val response = api.deleteAttachment(attachmentId)
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            attachments = it.attachments.filter { a -> a.id != attachmentId },
                            attachmentsCount = (it.attachmentsCount - 1).coerceAtLeast(0)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Не вдалося видалити файл"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
