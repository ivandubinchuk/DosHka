package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO користувача з сервера
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    val role: String = "executor",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("team_id")
    val teamId: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

/**
 * Запит на оновлення профілю
 */
@Serializable
data class UpdateProfileRequest(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
