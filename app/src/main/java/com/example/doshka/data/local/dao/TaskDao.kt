package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE columnId = :columnId AND isDeleted = 0 ORDER BY position ASC")
    fun observeTasksByColumn(columnId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE columnId = :columnId AND isDeleted = 0 ORDER BY position ASC")
    suspend fun getTasksByColumn(columnId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE assigneeId = :assigneeId AND isDeleted = 0")
    fun observeTasksByAssignee(assigneeId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE assigneeId = :assigneeId AND isDeleted = 0")
    suspend fun getTasksByAssignee(assigneeId: String): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE deadline IS NOT NULL
        AND deadline < :timestamp
        AND completedAt IS NULL
        AND isDeleted = 0
    """)
    fun observeOverdueTasks(timestamp: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE deadline IS NOT NULL
        AND deadline BETWEEN :startTimestamp AND :endTimestamp
        AND completedAt IS NULL
        AND isDeleted = 0
    """)
    fun observeTasksDeadlineSoon(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority AND isDeleted = 0")
    fun observeTasksByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE columnId = :columnId AND isDeleted = 0")
    fun observeTaskCountByColumn(columnId: String): Flow<Int>

    @Query("""
        SELECT * FROM tasks t
        INNER JOIN columns c ON t.columnId = c.id
        WHERE c.boardId = :boardId AND t.isDeleted = 0
        ORDER BY c.position ASC, t.position ASC
    """)
    fun observeTasksByBoard(boardId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET columnId = :columnId, position = :position, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun moveTask(taskId: String, columnId: String, position: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET position = :position WHERE id = :taskId")
    suspend fun updateTaskPosition(taskId: String, position: Int)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND updatedAt < :timestamp")
    suspend fun permanentlyDeleteOldTasks(timestamp: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Аналітика
    @Query("SELECT COUNT(*) FROM tasks WHERE completedAt IS NOT NULL AND completedAt >= :startDate AND completedAt < :endDate")
    suspend fun getCompletedTasksCount(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE createdAt >= :startDate AND createdAt < :endDate")
    suspend fun getCreatedTasksCount(startDate: Long, endDate: Long): Int

    @Query("""
        SELECT AVG(completedAt - createdAt) FROM tasks
        WHERE completedAt IS NOT NULL
        AND assigneeId = :assigneeId
    """)
    suspend fun getAverageCycleTime(assigneeId: String): Long?

    // Синхронізація
    @Query("UPDATE tasks SET id = :newId WHERE id = :oldId")
    suspend fun updateTaskId(oldId: String, newId: String)

    @Query("UPDATE tasks SET version = :version WHERE id = :taskId")
    suspend fun updateVersion(taskId: String, version: Int)

    @Query("UPDATE tasks SET isSynced = 1 WHERE id = :taskId")
    suspend fun markAsSynced(taskId: String)

    @Query("UPDATE tasks SET isSynced = 0 WHERE id = :taskId")
    suspend fun markAsUnsynced(taskId: String)

    @Query("""
        UPDATE tasks SET
            title = :title,
            description = :description,
            columnId = :columnId,
            position = :position,
            priority = :priority,
            assigneeId = :assigneeId,
            deadline = :deadline,
            version = :version,
            updatedAt = :updatedAt,
            isSynced = 1
        WHERE id = :id
    """)
    suspend fun updateFromServer(
        id: String,
        title: String,
        description: String?,
        columnId: String,
        position: Int,
        priority: String,
        assigneeId: String?,
        deadline: Long?,
        version: Int,
        updatedAt: Long
    )

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>
}
