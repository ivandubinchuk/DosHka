package com.example.doshka.domain.model

/**
 * Доменна модель команди
 */
data class Team(
    val id: String,
    val name: String,
    val description: String? = null,
    val managerId: String,
    val inviteCode: String? = null,
    val members: List<User> = emptyList()
)
