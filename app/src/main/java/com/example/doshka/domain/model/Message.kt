package com.example.doshka.domain.model

import java.time.Instant

/**
 * Доменна модель повідомлення в чаті задачі
 */
data class Message(
    val id: String,
    val taskId: String,
    val sender: User?,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null,
    val isRead: Boolean = false,
    val createdAt: Instant = Instant.now()
)

/**
 * Тип повідомлення
 */
enum class MessageType {
    TEXT,
    VOICE,
    SYSTEM;

    companion object {
        fun fromString(value: String): MessageType = when (value.lowercase()) {
            "text" -> TEXT
            "voice" -> VOICE
            "system" -> SYSTEM
            else -> TEXT
        }
    }

    override fun toString(): String = name.lowercase()
}
