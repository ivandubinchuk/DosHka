package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність кольорової мітки в локальній БД
 */
@Entity(
    tableName = "labels",
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
data class LabelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String, // HEX колір, наприклад "#FF5722"
    val teamId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Зв'язок many-to-many між задачами та мітками
 */
@Entity(
    tableName = "task_labels",
    primaryKeys = ["taskId", "labelId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["labelId"])
    ]
)
data class TaskLabelCrossRef(
    val taskId: String,
    val labelId: String
)
