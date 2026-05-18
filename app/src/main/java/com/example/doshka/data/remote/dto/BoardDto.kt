package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO команди з сервера
 */
@Serializable
data class TeamDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("manager_id")
    val managerId: String,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    val members: List<UserDto>? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * DTO дошки з сервера
 */
@Serializable
data class BoardDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("team_id")
    val teamId: String,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    val columns: List<ColumnDto>? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * DTO колонки з сервера
 */
@Serializable
data class ColumnDto(
    val id: String,
    val name: String,
    @SerialName("board_id")
    val boardId: String,
    val position: Int,
    @SerialName("wip_limit")
    val wipLimit: Int? = null,
    val color: String? = null,
    val tasks: List<TaskDto>? = null,
    @SerialName("task_count")
    val taskCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * DTO мітки з сервера
 */
@Serializable
data class LabelDto(
    val id: String,
    val name: String,
    val color: String,
    @SerialName("team_id")
    val teamId: String,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Запит на створення команди
 */
@Serializable
data class CreateTeamRequest(
    val name: String,
    val description: String? = null
)

/**
 * Запит на створення дошки
 */
@Serializable
data class CreateBoardRequest(
    val name: String,
    val description: String? = null,
    @SerialName("team_id")
    val teamId: String
)

/**
 * Запит на створення колонки
 */
@Serializable
data class CreateColumnRequest(
    val name: String,
    @SerialName("board_id")
    val boardId: String,
    val position: Int,
    @SerialName("wip_limit")
    val wipLimit: Int? = null,
    val color: String? = null
)

/**
 * Запит на створення мітки
 */
@Serializable
data class CreateLabelRequest(
    val name: String,
    val color: String,
    @SerialName("team_id")
    val teamId: String
)
