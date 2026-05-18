package com.example.doshka.data.repository

import com.example.doshka.data.local.dao.BoardDao
import com.example.doshka.data.local.dao.ColumnDao
import com.example.doshka.data.local.entity.BoardEntity
import com.example.doshka.data.local.entity.ColumnEntity
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.BoardDto
import com.example.doshka.data.remote.dto.ColumnDto
import com.example.doshka.data.remote.dto.CreateBoardRequest
import com.example.doshka.data.remote.dto.CreateColumnRequest
import com.example.doshka.domain.model.Board
import com.example.doshka.domain.model.Column
import com.example.doshka.domain.repository.BoardRepository
import com.example.doshka.util.DateTimeUtils
import com.example.doshka.util.NetworkResult
import com.example.doshka.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepositoryImpl @Inject constructor(
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val api: DoshkaApi
) : BoardRepository {

    override fun observeBoardsByTeam(teamId: String): Flow<List<Board>> {
        return boardDao.observeBoardsByTeam(teamId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeBoard(boardId: String): Flow<Board?> {
        return boardDao.observeBoardById(boardId).map { it?.toDomain() }
    }

    override fun observeColumnsByBoard(boardId: String): Flow<List<Column>> {
        return columnDao.observeColumnsByBoard(boardId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBoardById(boardId: String): Board? {
        return boardDao.getBoardById(boardId)?.toDomain()
    }

    override suspend fun getDefaultBoard(teamId: String): Board? {
        return boardDao.getDefaultBoard(teamId)?.toDomain()
    }

    override suspend fun createBoard(
        name: String,
        description: String?,
        teamId: String
    ): NetworkResult<Board> {
        return safeApiCall {
            val request = CreateBoardRequest(name, description, teamId)
            val response = api.createBoard(request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity()
                boardDao.insertBoard(entity)

                // Зберігаємо колонки
                dto.columns?.let { columns ->
                    columnDao.insertColumns(columns.map { it.toEntity() })
                }

                entity.toDomain()
            } else {
                throw Exception("Не вдалося створити дошку")
            }
        }
    }

    override suspend fun updateBoard(
        boardId: String,
        name: String,
        description: String?
    ): NetworkResult<Board> {
        return safeApiCall {
            val board = boardDao.getBoardById(boardId)
                ?: throw Exception("Дошку не знайдено")

            val request = CreateBoardRequest(name, description, board.teamId)
            val response = api.updateBoard(boardId, request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity()
                boardDao.updateBoard(entity)
                entity.toDomain()
            } else {
                throw Exception("Не вдалося оновити дошку")
            }
        }
    }

    override suspend fun deleteBoard(boardId: String): NetworkResult<Unit> {
        return safeApiCall {
            val response = api.deleteBoard(boardId)
            if (response.isSuccessful) {
                boardDao.deleteBoard(boardId)
            } else {
                throw Exception("Не вдалося видалити дошку")
            }
        }
    }

    override suspend fun createColumn(
        name: String,
        boardId: String,
        position: Int,
        wipLimit: Int?,
        color: String?
    ): NetworkResult<Column> {
        return safeApiCall {
            val request = CreateColumnRequest(name, boardId, position, wipLimit, color)
            val response = api.createColumn(request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity()
                columnDao.insertColumn(entity)
                entity.toDomain()
            } else {
                throw Exception("Не вдалося створити колонку")
            }
        }
    }

    override suspend fun updateColumn(
        columnId: String,
        name: String?,
        wipLimit: Int?,
        color: String?
    ): NetworkResult<Column> {
        return safeApiCall {
            val column = columnDao.getColumnById(columnId)
                ?: throw Exception("Колонку не знайдено")

            val request = CreateColumnRequest(
                name = name ?: column.name,
                boardId = column.boardId,
                position = column.position,
                wipLimit = wipLimit ?: column.wipLimit,
                color = color ?: column.color
            )
            val response = api.updateColumn(columnId, request)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = dto.toEntity()
                columnDao.updateColumn(entity)
                entity.toDomain()
            } else {
                throw Exception("Не вдалося оновити колонку")
            }
        }
    }

    override suspend fun deleteColumn(columnId: String): NetworkResult<Unit> {
        return safeApiCall {
            val response = api.deleteColumn(columnId)
            if (response.isSuccessful) {
                columnDao.deleteColumn(columnId)
            } else {
                throw Exception("Не вдалося видалити колонку")
            }
        }
    }

    override suspend fun syncBoard(boardId: String): NetworkResult<Unit> {
        return safeApiCall {
            val response = api.getBoard(boardId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                boardDao.insertBoard(dto.toEntity())

                dto.columns?.let { columns ->
                    columnDao.insertColumns(columns.map { it.toEntity() })
                }
            } else {
                throw Exception("Не вдалося синхронізувати дошку")
            }
        }
    }

    // Маппери
    private fun BoardEntity.toDomain(): Board {
        return Board(
            id = id,
            name = name,
            description = description,
            teamId = teamId,
            isDefault = isDefault,
            columns = emptyList()
        )
    }

    private fun BoardDto.toEntity(): BoardEntity {
        return BoardEntity(
            id = id,
            name = name,
            description = description,
            teamId = teamId,
            isDefault = isDefault,
            createdAt = DateTimeUtils.parseToEpochMilli(createdAt),
            updatedAt = DateTimeUtils.parseToEpochMilli(updatedAt)
        )
    }

    private fun ColumnEntity.toDomain(): Column {
        return Column(
            id = id,
            name = name,
            boardId = boardId,
            position = position,
            wipLimit = wipLimit,
            color = color,
            tasks = emptyList(),
            taskCount = 0
        )
    }

    private fun ColumnDto.toEntity(): ColumnEntity {
        return ColumnEntity(
            id = id,
            name = name,
            boardId = boardId,
            position = position,
            wipLimit = wipLimit,
            color = color,
            createdAt = DateTimeUtils.parseToEpochMilli(createdAt),
            updatedAt = DateTimeUtils.parseToEpochMilli(updatedAt)
        )
    }
}
