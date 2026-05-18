package com.example.doshka.ui.screens.chat

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.SendMessageRequest
import com.example.doshka.domain.model.Message
import com.example.doshka.domain.model.MessageType
import com.example.doshka.domain.model.User
import com.example.doshka.domain.model.UserRole
import com.example.doshka.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Стан екрану чату задачі
 */
data class TaskChatUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val taskId: String = "",
    val taskTitle: String = "",
    val messages: List<Message> = emptyList(),
    val currentUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class TaskChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: DoshkaApi,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow(TaskChatUiState(taskId = taskId))
    val uiState: StateFlow<TaskChatUiState> = _uiState.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    init {
        loadCurrentUser()
        loadTask()
        loadMessages()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            dataStoreManager.userId.collect { userId ->
                _uiState.update { it.copy(currentUserId = userId) }
            }
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            try {
                val response = api.getTask(taskId)
                if (response.isSuccessful && response.body() != null) {
                    val task = response.body()!!
                    _uiState.update { it.copy(taskTitle = task.title) }
                }
            } catch (e: Exception) {
                // Ігноруємо помилку, просто не покажемо назву
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getMessages(taskId)
                if (response.isSuccessful && response.body() != null) {
                    val messages = response.body()!!.map { dto ->
                        Message(
                            id = dto.id,
                            taskId = dto.taskId,
                            sender = dto.sender?.let { user ->
                                User(
                                    id = user.id,
                                    email = user.email,
                                    fullName = user.fullName,
                                    role = UserRole.fromString(user.role),
                                    teamId = user.teamId,
                                    avatarUrl = user.avatarUrl,
                                    isActive = user.isActive
                                )
                            },
                            content = dto.content,
                            type = MessageType.fromString(dto.type),
                            voiceUrl = dto.voiceUrl,
                            voiceDuration = dto.voiceDuration,
                            isRead = dto.isRead,
                            createdAt = DateTimeUtils.parseInstant(dto.createdAt)
                        )
                    }.sortedBy { it.createdAt }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Помилка завантаження повідомлень"
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

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                val request = SendMessageRequest(
                    taskId = taskId,
                    content = text,
                    type = "text"
                )
                val response = api.sendMessage(request)
                if (response.isSuccessful) {
                    _messageText.value = ""
                    // Перезавантажуємо повідомлення
                    loadMessages()
                } else {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = "Не вдалося надіслати"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Відправка голосового повідомлення
     * Конвертуємо файл у Base64 і відправляємо як voice-повідомлення
     */
    fun sendVoiceMessage(file: File, durationSeconds: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                // Читаємо файл і конвертуємо в Base64
                val bytes = file.readBytes()
                val base64Audio = Base64.encodeToString(bytes, Base64.NO_WRAP)

                val request = SendMessageRequest(
                    taskId = taskId,
                    content = "🎤 Голосове повідомлення (${durationSeconds}с)",
                    type = "voice",
                    voiceData = base64Audio,
                    voiceDuration = durationSeconds
                )

                val response = api.sendMessage(request)
                if (response.isSuccessful) {
                    // Видаляємо тимчасовий файл
                    file.delete()
                    // Перезавантажуємо повідомлення
                    loadMessages()
                } else {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = "Не вдалося надіслати голосове повідомлення"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }
}
