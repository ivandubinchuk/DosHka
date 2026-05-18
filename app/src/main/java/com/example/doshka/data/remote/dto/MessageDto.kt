package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO повідомлення з сервера
 */
@Serializable
data class MessageDto(
    val id: String,
    @SerialName("task_id")
    val taskId: String,
    @SerialName("sender_id")
    val senderId: String?,
    val sender: UserDto? = null,
    val content: String,
    val type: String = "text",
    @SerialName("voice_url")
    val voiceUrl: String? = null,
    @SerialName("voice_duration")
    val voiceDuration: Int? = null,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Запит на відправку повідомлення
 */
@Serializable
data class SendMessageRequest(
    @SerialName("task_id")
    val taskId: String,
    val content: String,
    val type: String = "text",
    @SerialName("voice_data")
    val voiceData: String? = null, // Base64 закодоване аудіо
    @SerialName("voice_duration")
    val voiceDuration: Int? = null // Тривалість у секундах
)

/**
 * DTO вкладення з сервера
 */
@Serializable
data class AttachmentDto(
    val id: String,
    @SerialName("task_id")
    val taskId: String,
    @SerialName("uploader_id")
    val uploaderId: String?,
    val uploader: UserDto? = null,
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_type")
    val fileType: String,
    @SerialName("file_size")
    val fileSize: Long,
    val url: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String
)
