package com.example.doshka.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запит на вхід
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Запит на реєстрацію
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("full_name")
    val fullName: String,
    val role: String = "executor",
    @SerialName("team_id")
    val teamId: String? = null,
    @SerialName("invite_token")
    val inviteToken: String? = null
)

/**
 * Відповідь з токенами
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("token_type")
    val tokenType: String = "bearer",
    @SerialName("expires_in")
    val expiresIn: Long
)

/**
 * Запит на оновлення токена
 */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

/**
 * Запит на скидання пароля
 */
@Serializable
data class PasswordResetRequest(
    val email: String
)

/**
 * Запит на зміну пароля
 */
@Serializable
data class PasswordChangeRequest(
    @SerialName("current_password")
    val currentPassword: String,
    @SerialName("new_password")
    val newPassword: String
)
