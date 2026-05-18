package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність повідомлення в чаті задачі
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["senderId"]),
        Index(value = ["createdAt"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val senderId: String?,
    val content: String,
    val type: String = "text", // text, voice, system
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null, // тривалість у секундах
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)
