package com.example.doshka.domain.model

/**
 * Доменна модель Канбан-дошки
 */
data class Board(
    val id: String,
    val name: String,
    val description: String? = null,
    val teamId: String,
    val isDefault: Boolean = false,
    val columns: List<Column> = emptyList()
)

/**
 * Доменна модель колонки Канбан-дошки
 */
data class Column(
    val id: String,
    val name: String,
    val boardId: String,
    val position: Int,
    val wipLimit: Int? = null,
    val color: String? = null,
    val tasks: List<Task> = emptyList(),
    val taskCount: Int = 0
)
