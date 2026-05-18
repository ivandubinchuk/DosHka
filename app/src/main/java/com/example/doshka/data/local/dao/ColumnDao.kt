package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.ColumnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnDao {

    @Query("SELECT * FROM columns WHERE id = :columnId")
    suspend fun getColumnById(columnId: String): ColumnEntity?

    @Query("SELECT * FROM columns WHERE boardId = :boardId ORDER BY position ASC")
    fun observeColumnsByBoard(boardId: String): Flow<List<ColumnEntity>>

    @Query("SELECT * FROM columns WHERE boardId = :boardId ORDER BY position ASC")
    suspend fun getColumnsByBoard(boardId: String): List<ColumnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: ColumnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumns(columns: List<ColumnEntity>)

    @Update
    suspend fun updateColumn(column: ColumnEntity)

    @Query("UPDATE columns SET position = :position WHERE id = :columnId")
    suspend fun updateColumnPosition(columnId: String, position: Int)

    @Query("DELETE FROM columns WHERE id = :columnId")
    suspend fun deleteColumn(columnId: String)

    @Query("DELETE FROM columns WHERE boardId = :boardId")
    suspend fun deleteColumnsByBoard(boardId: String)

    @Query("DELETE FROM columns")
    suspend fun deleteAllColumns()

    // Синхронізація
    @Query("UPDATE columns SET id = :newId WHERE id = :oldId")
    suspend fun updateColumnId(oldId: String, newId: String)

    @Query("UPDATE columns SET isSynced = 1 WHERE id = :columnId")
    suspend fun markAsSynced(columnId: String)

    @Query("UPDATE columns SET isSynced = 0 WHERE id = :columnId")
    suspend fun markAsUnsynced(columnId: String)
}
