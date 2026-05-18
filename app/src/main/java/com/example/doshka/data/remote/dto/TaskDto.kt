package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO задачі з сервера
 */
@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("column_id")
    val columnId: String,
    val position: Int,
    val priority: String = "medium",
    @SerialName("assignee_id")
    val assigneeId: String? = null,
    val assignee: UserDto? = null,
    @SerialName("creator_id")
    val creatorId: String? = null,
    val creator: UserDto? = null,
    val deadline: String? = null,
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,
    @SerialName("actual_hours")
    val actualHours: Float? = null,
    val tags: List<String> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    @SerialName("attachments_count")
    val attachmentsCount: Int = 0,
    @SerialName("comments_count")
    val commentsCount: Int = 0,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val version: Int = 0 // Optimistic locking version
)

/**
 * Запит на створення задачі
 */
@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    @SerialName("column_id")
    val columnId: String,
    val priority: String = "medium",
    @SerialName("assignee_id")
    val assigneeId: String? = null,
    val deadline: String? = null,
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    @SerialName("label_ids")
    val labelIds: List<String> = emptyList()
)

/**
 * Запит на оновлення задачі
 */
@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    @SerialName("assignee_id")
    val assigneeId: String? = null,
    val deadline: String? = null,
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,
    @SerialName("actual_hours")
    val actualHours: Float? = null,
    val tags: List<String>? = null,
    @SerialName("label_ids")
    val labelIds: List<String>? = null,
    val version: Int? = null // Для optimistic locking
)

/**
 * Запит на переміщення задачі
 */
@Serializable
data class MoveTaskRequest(
    @SerialName("column_id")
    val columnId: String,
    val position: Int
)
