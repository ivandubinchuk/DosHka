package com.example.doshka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сутність колонки Канбан-дошки в локальній БД
 */
@Entity(
    tableName = "columns",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["boardId"])]
)
data class ColumnEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val boardId: String,
    val position: Int,
    val wipLimit: Int? = null, // Work In Progress ліміт
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)
