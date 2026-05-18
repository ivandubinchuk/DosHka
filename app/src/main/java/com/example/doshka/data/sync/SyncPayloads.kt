package com.example.doshka.data.sync

import kotlinx.serialization.Serializable

/**
 * Payload для створення задачі
 */
@Serializable
data class CreateTaskPayload(
    val title: String,
    val description: String? = null,
    val columnId: String,
    val priority: String = "medium",
    val assigneeId: String? = null,
    val deadline: String? = null,
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    val labelIds: List<String> = emptyList()
)

/**
 * Payload для оновлення задачі
 */
@Serializable
data class UpdateTaskPayload(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val assigneeId: String? = null,
    val deadline: String? = null,
    val estimatedHours: Float? = null,
    val actualHours: Float? = null,
    val tags: List<String>? = null,
    val labelIds: List<String>? = null,
    val version: Int = 0 // Для optimistic locking
)

/**
 * Payload для переміщення задачі
 */
@Serializable
data class MoveTaskPayload(
    val columnId: String,
    val position: Int
)

/**
 * Payload для відправки повідомлення
 */
@Serializable
data class SendMessagePayload(
    val taskId: String,
    val content: String,
    val type: String = "text"
)

/**
 * Payload для створення/оновлення дошки
 */
@Serializable
data class CreateBoardPayload(
    val name: String,
    val description: String? = null,
    val teamId: String
)

/**
 * Payload для створення/оновлення колонки
 */
@Serializable
data class CreateColumnPayload(
    val name: String,
    val boardId: String,
    val position: Int,
    val wipLimit: Int? = null,
    val color: String? = null
)
