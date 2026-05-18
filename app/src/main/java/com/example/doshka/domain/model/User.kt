package com.example.doshka.domain.model

/**
 * Доменна модель користувача
 */
data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val teamId: String? = null,
    val isActive: Boolean = true
)

/**
 * Роль користувача
 */
enum class UserRole {
    MANAGER,
    EXECUTOR;

    companion object {
        fun fromString(value: String): UserRole = when (value.lowercase()) {
            "manager" -> MANAGER
            "executor" -> EXECUTOR
            else -> EXECUTOR
        }
    }

    override fun toString(): String = when (this) {
        MANAGER -> "manager"
        EXECUTOR -> "executor"
    }
}
