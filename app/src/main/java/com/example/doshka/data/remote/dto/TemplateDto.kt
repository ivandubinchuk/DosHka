package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO шаблону задачі
 */
@Serializable
data class TaskTemplateDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val priority: String = "medium",
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    @SerialName("label_ids")
    val labelIds: List<String> = emptyList(),
    @SerialName("team_id")
    val teamId: String,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Запит на створення шаблону
 */
@Serializable
data class CreateTemplateRequest(
    val name: String,
    val description: String? = null,
    val priority: String = "medium",
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,
    val tags: List<String> = emptyList(),
    @SerialName("label_ids")
    val labelIds: List<String> = emptyList(),
    @SerialName("team_id")
    val teamId: String
)
