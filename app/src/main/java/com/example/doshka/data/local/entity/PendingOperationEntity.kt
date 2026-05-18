package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність для зберігання офлайн-операцій, які потрібно синхронізувати
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String, // create_task, update_task, delete_task, move_task, send_message
    val entityType: String, // task, message, attachment
    val entityId: String,
    val payload: String, // JSON з даними операції
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: String = "pending", // pending, processing, failed, completed
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null
)
