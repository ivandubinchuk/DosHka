package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність вкладення до задачі
 */
@Entity(
    tableName = "attachments",
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
            childColumns = ["uploaderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["uploaderId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val uploaderId: String?,
    val fileName: String,
    val fileType: String, // image, document, audio, other
    val fileSize: Long, // розмір у байтах
    val url: String,
    val thumbnailUrl: String? = null,
    val localPath: String? = null, // шлях до локального файлу (для офлайн)
    val createdAt: Long = System.currentTimeMillis()
)
