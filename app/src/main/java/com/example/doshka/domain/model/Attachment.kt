package com.example.doshka.domain.model

import java.time.Instant

/**
 * Доменна модель вкладення
 */
data class Attachment(
    val id: String,
    val taskId: String,
    val uploader: User?,
    val fileName: String,
    val fileType: AttachmentType,
    val fileSize: Long,
    val url: String,
    val thumbnailUrl: String? = null,
    val localPath: String? = null,
    val createdAt: Instant = Instant.now()
) {
    /**
     * Форматований розмір файлу
     */
    val formattedSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize Б"
                fileSize < 1024 * 1024 -> "${fileSize / 1024} КБ"
                else -> "${fileSize / (1024 * 1024)} МБ"
            }
        }
}

/**
 * Тип вкладення
 */
enum class AttachmentType {
    IMAGE,
    DOCUMENT,
    AUDIO,
    OTHER;

    companion object {
        fun fromString(value: String): AttachmentType = when (value.lowercase()) {
            "image" -> IMAGE
            "document" -> DOCUMENT
            "audio" -> AUDIO
            else -> OTHER
        }

        fun fromMimeType(mimeType: String): AttachmentType = when {
            mimeType.startsWith("image/") -> IMAGE
            mimeType.startsWith("audio/") -> AUDIO
            mimeType.startsWith("application/pdf") ||
            mimeType.startsWith("application/msword") ||
            mimeType.startsWith("application/vnd.") ||
            mimeType.startsWith("text/") -> DOCUMENT
            else -> OTHER
        }
    }

    override fun toString(): String = name.lowercase()
}
