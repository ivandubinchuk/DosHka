package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність Канбан-дошки в локальній БД
 */
@Entity(
    tableName = "boards",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["teamId"])]
)
data class BoardEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val teamId: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)
