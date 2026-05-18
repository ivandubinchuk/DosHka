package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність задачі в локальній БД
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ColumnEntity::class,
            parentColumns = ["id"],
            childColumns = ["columnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["assigneeId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["columnId"]),
        Index(value = ["assigneeId"]),
        Index(value = ["creatorId"]),
        Index(value = ["deadline"]),
        Index(value = ["priority"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val columnId: String,
    val position: Int,
    val priority: String = "medium", // critical, high, medium, low
    val assigneeId: String? = null,
    val creatorId: String? = null,
    val deadline: Long? = null,
    val estimatedHours: Float? = null,
    val actualHours: Float? = null,
    val tags: String = "[]", // JSON масив тегів
    val attachmentsCount: Int = 0,
    val commentsCount: Int = 0,
    val isDeleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Поля синхронізації
    val version: Int = 0, // Optimistic locking version
    val isSynced: Boolean = true // false = потребує синхронізації
)
