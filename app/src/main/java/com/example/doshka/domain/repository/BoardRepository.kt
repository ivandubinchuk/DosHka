package com.example.doshka.domain.repository

import com.example.doshka.domain.model.Board
import com.example.doshka.domain.model.Column
import com.example.doshka.util.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторій для роботи з дошками
 */
interface BoardRepository {

    /**
     * Спостерігає за дошками команди
     */
    fun observeBoardsByTeam(teamId: String): Flow<List<Board>>

    /**
     * Спостерігає за конкретною дошкою
     */
    fun observeBoard(boardId: String): Flow<Board?>

    /**
     * Спостерігає за колонками дошки
     */
    fun observeColumnsByBoard(boardId: String): Flow<List<Column>>

    /**
     * Отримує дошку за ID
     */
    suspend fun getBoardById(boardId: String): Board?

    /**
     * Отримує дефолтну дошку команди
     */
    suspend fun getDefaultBoard(teamId: String): Board?

    /**
     * Створює нову дошку
     */
    suspend fun createBoard(name: String, description: String?, teamId: String): NetworkResult<Board>

    /**
     * Оновлює дошку
     */
    suspend fun updateBoard(boardId: String, name: String, description: String?): NetworkResult<Board>

    /**
     * Видаляє дошку
     */
    suspend fun deleteBoard(boardId: String): NetworkResult<Unit>

    /**
     * Створює колонку
     */
    suspend fun createColumn(
        name: String,
        boardId: String,
        position: Int,
        wipLimit: Int? = null,
        color: String? = null
    ): NetworkResult<Column>

    /**
     * Оновлює колонку
     */
    suspend fun updateColumn(
        columnId: String,
        name: String? = null,
        wipLimit: Int? = null,
        color: String? = null
    ): NetworkResult<Column>

    /**
     * Видаляє колонку
     */
    suspend fun deleteColumn(columnId: String): NetworkResult<Unit>

    /**
     * Синхронізує дошку з сервером
     */
    suspend fun syncBoard(boardId: String): NetworkResult<Unit>
}
