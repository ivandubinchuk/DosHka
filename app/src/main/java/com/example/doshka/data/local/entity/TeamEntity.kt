package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність команди в локальній БД
 */
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val managerId: String,
    val inviteCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
