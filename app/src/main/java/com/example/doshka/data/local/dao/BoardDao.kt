package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.BoardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {

    @Query("SELECT * FROM boards WHERE id = :boardId")
    suspend fun getBoardById(boardId: String): BoardEntity?

    @Query("SELECT * FROM boards WHERE id = :boardId")
    fun observeBoardById(boardId: String): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE teamId = :teamId")
    fun observeBoardsByTeam(teamId: String): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE teamId = :teamId")
    suspend fun getBoardsByTeam(teamId: String): List<BoardEntity>

    @Query("SELECT * FROM boards WHERE teamId = :teamId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultBoard(teamId: String): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoards(boards: List<BoardEntity>)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Query("DELETE FROM boards WHERE id = :boardId")
    suspend fun deleteBoard(boardId: String)

    @Query("DELETE FROM boards")
    suspend fun deleteAllBoards()

    // Синхронізація
    @Query("UPDATE boards SET id = :newId WHERE id = :oldId")
    suspend fun updateBoardId(oldId: String, newId: String)

    @Query("UPDATE boards SET isSynced = 1 WHERE id = :boardId")
    suspend fun markAsSynced(boardId: String)

    @Query("UPDATE boards SET isSynced = 0 WHERE id = :boardId")
    suspend fun markAsUnsynced(boardId: String)
}
