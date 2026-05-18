package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність шаблону задачі
 */
@Entity(
    tableName = "task_templates",
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
data class TaskTemplateEntity(
    @PrimaryKey
    val id: String,
    val teamId: String,
    val name: String,
    val titleTemplate: String,
    val descriptionTemplate: String? = null,
    val priority: String = "medium",
    val tags: String = "[]", // JSON масив тегів
    val estimatedHours: Float? = null,
    val cronSchedule: String? = null, // CRON-вираз для повторюваних задач
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
